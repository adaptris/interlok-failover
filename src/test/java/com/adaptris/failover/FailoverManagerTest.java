package com.adaptris.failover;

import java.util.UUID;

import static org.mockito.Mockito.verify;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import junit.framework.TestCase;

public class FailoverManagerTest extends TestCase {
  
  private static final int MASTER = 1;
  private static final int SLAVE = 2;
  
  private static final String UUID1 = "11111111-1111-1111-1111-111111111111";
  private static final String UUID2 = "22222222-2222-2222-2222-222222222222";
  
  private FailoverManager failoverManager;
  
  @Mock
  private Listener mockListener;
  @Mock
  private Broadcaster mockBroadcaster;
  @Mock
  private MonitorThread mockMonitorThread;
  @Mock
  private StateChangeEventListener mockStateChangeEventListener;
  @Mock
  private MultiMasterConflictHandler mockMultiMasterConflictHandler;
  
  private Ping mockMasterPing;
  
  private Ping mockSlavePing;
  
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    failoverManager = new FailoverManager("myHost", "1111", mockListener, mockBroadcaster, false, 0);
    failoverManager.setPollingThread(mockMonitorThread);
    failoverManager.registerListener(mockStateChangeEventListener);
    failoverManager.setMultiMasterConflictHandler(mockMultiMasterConflictHandler);
    
    mockMasterPing = new Ping();
    mockMasterPing.setInstanceId(UUID.randomUUID());
    mockMasterPing.setInstanceType(MASTER);
    mockMasterPing.setSlaveNumber(0);
    mockMasterPing.setSourceHost("myHost");
    mockMasterPing.setSourcePort("1111");
    
    mockSlavePing = new Ping();
    mockSlavePing.setInstanceId(UUID.randomUUID());
    mockSlavePing.setInstanceType(SLAVE);
    mockSlavePing.setSlaveNumber(1);
    mockSlavePing.setSourceHost("myHost");
    mockSlavePing.setSourcePort("1111");
  }
  
  public void tearDown() throws Exception {
    
  }
  
  public void testStartUpAssignsSlaveNumber() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSlaveNumber());
    
    failoverManager.start();
    // simulate a polling trigger
    failoverManager.pollTriggered();
    
    assertEquals(1, failoverManager.getMyInstance().getSlaveNumber());
    assertEquals(SLAVE, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testSlavePromoted() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSlaveNumber());
    
    failoverManager.start();
    failoverManager.getMyInstance().setSlaveNumber(2); // set this as slave number 2!
    // simulate a polling trigger
    failoverManager.pollTriggered();
    // check we have been promoted, because there is no slave 1.
    assertEquals(1, failoverManager.getMyInstance().getSlaveNumber());
    assertEquals(SLAVE, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testSlavePromotedToMaster() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSlaveNumber());
    
    failoverManager.start();
    // simulate a polling trigger
    failoverManager.pollTriggered();
    
    assertEquals(1, failoverManager.getMyInstance().getSlaveNumber());
    assertEquals(SLAVE, failoverManager.getMyInstance().getInstanceType());
    
    // simulate another polling trigger which should recognise no master and promote us.
    failoverManager.pollTriggered();
    
    assertEquals(MASTER, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testMasterPingedStayAsSlave() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSlaveNumber());
    
    failoverManager.start();
    // simulate a polling trigger
    failoverManager.pollTriggered();
    // check we have been promoted, because there is no slave 1.
    assertEquals(1, failoverManager.getMyInstance().getSlaveNumber());
    assertEquals(SLAVE, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.masterPinged(mockMasterPing);
    
    // simulate another polling trigger
    failoverManager.pollTriggered();
    // Master now exists, check we have NOT been promoted.
    assertEquals(1, failoverManager.getMyInstance().getSlaveNumber());
    assertEquals(SLAVE, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testSlaveNumbersReassigned() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSlaveNumber());
    
    failoverManager.start();
    failoverManager.getMyInstance().setId(UUID.fromString(UUID2)); // give ourselves a UUID, once ordered will be second in the list
    // simulate a polling trigger
    failoverManager.pollTriggered();
    // check we have been assigned a slave number 1 - hence no pings received yet
    assertEquals(1, failoverManager.getMyInstance().getSlaveNumber());
    assertEquals(SLAVE, failoverManager.getMyInstance().getInstanceType());
    
    // now another slave pings us, also thinking it is slave number 1
    mockSlavePing.setInstanceId(UUID.fromString(UUID1));
    failoverManager.slavePinged(mockSlavePing);
    
    // simulate a polling trigger, should reassign our slave number.
    failoverManager.pollTriggered();
    
    // check we have been re-assigned a slave number 2
    assertEquals(2, failoverManager.getMyInstance().getSlaveNumber());
    assertEquals(SLAVE, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testMasterConflictHandled() throws Exception {
    // make sure we are master
    failoverManager = new FailoverManager("myHost", "1111", mockListener, mockBroadcaster, true, 0);
    failoverManager.setPollingThread(mockMonitorThread);
    failoverManager.registerListener(mockStateChangeEventListener);
    failoverManager.setMultiMasterConflictHandler(mockMultiMasterConflictHandler);
    
    failoverManager.start();
    
    // simulate a polling trigger
    failoverManager.pollTriggered();
    // check we are master
    assertEquals(0, failoverManager.getMyInstance().getSlaveNumber());
    assertEquals(MASTER, failoverManager.getMyInstance().getInstanceType());
    
    // now ping ourselves from a fictitious other instance that thinks its the master!
    failoverManager.masterPinged(mockMasterPing);
    
    // simulate another polling trigger
    failoverManager.pollTriggered();
    
    failoverManager.stop();
    // Make sure the conflict handler ran
    verify(mockMultiMasterConflictHandler).handle(failoverManager.getMyInstance(), mockMasterPing);
  }
  
  public void testSlavePromotedToMasterThenConflictHandler() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSlaveNumber());
    
    failoverManager.start();
    // simulate a polling trigger
    failoverManager.pollTriggered();
    
    assertEquals(1, failoverManager.getMyInstance().getSlaveNumber());
    assertEquals(SLAVE, failoverManager.getMyInstance().getInstanceType());
    
    // simulate another polling trigger which should recognise no master and promote us.
    failoverManager.pollTriggered();
    
    assertEquals(MASTER, failoverManager.getMyInstance().getInstanceType());
    
    // now ping ourselves from a fictitious other instance that thinks its the master!
    failoverManager.masterPinged(mockMasterPing);
    
    // simulate another polling trigger
    failoverManager.pollTriggered();
    
    failoverManager.stop();
    
    // Make sure the conflict handler ran
    verify(mockMultiMasterConflictHandler).handle(failoverManager.getMyInstance(), mockMasterPing);
  }

}
