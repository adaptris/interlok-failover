package com.adaptris.failover;

import java.util.UUID;

import static org.mockito.Mockito.verify;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import junit.framework.TestCase;

public class FailoverManagerTest extends TestCase {
  
  private static final int PRIMARY = 1;
  private static final int SECONDARY = 2;
  
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
  private MultiPrimaryConflictHandler mockMultiPrimaryConflictHandler;
  
  private Ping mockPrimaryPing;
  
  private Ping mockSecondaryPing;
  
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    failoverManager = new FailoverManager("myHost", "1111", mockListener, mockBroadcaster, false, 0);
    failoverManager.setPollingThread(mockMonitorThread);
    failoverManager.registerListener(mockStateChangeEventListener);
    failoverManager.setMultiPrimaryConflictHandler(mockMultiPrimaryConflictHandler);
    
    mockPrimaryPing = new Ping();
    mockPrimaryPing.setInstanceId(UUID.randomUUID());
    mockPrimaryPing.setInstanceType(PRIMARY);
    mockPrimaryPing.setSecondaryNumber(0);
    mockPrimaryPing.setSourceHost("myHost");
    mockPrimaryPing.setSourcePort("1111");
    
    mockSecondaryPing = new Ping();
    mockSecondaryPing.setInstanceId(UUID.randomUUID());
    mockSecondaryPing.setInstanceType(SECONDARY);
    mockSecondaryPing.setSecondaryNumber(1);
    mockSecondaryPing.setSourceHost("myHost");
    mockSecondaryPing.setSourcePort("1111");
  }
  
  public void tearDown() throws Exception {
    
  }
  
  public void testStartUpAssignsSecondaryNumber() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSecondaryNumber());
    
    failoverManager.start();
    // simulate a polling trigger
    failoverManager.pollTriggered();
    
    assertEquals(1, failoverManager.getMyInstance().getSecondaryNumber());
    assertEquals(SECONDARY, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testSecondaryPromoted() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSecondaryNumber());
    
    failoverManager.start();
    failoverManager.getMyInstance().setSecondaryNumber(2); // set this as secondary number 2!
    // simulate a polling trigger
    failoverManager.pollTriggered();
    // check we have been promoted, because there is no secondary 1.
    assertEquals(1, failoverManager.getMyInstance().getSecondaryNumber());
    assertEquals(SECONDARY, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testSecondaryPromotedToPrimary() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSecondaryNumber());
    
    failoverManager.start();
    // simulate a polling trigger
    failoverManager.pollTriggered();
    
    assertEquals(1, failoverManager.getMyInstance().getSecondaryNumber());
    assertEquals(SECONDARY, failoverManager.getMyInstance().getInstanceType());
    
    // simulate another polling trigger which should recognise no primary and promote us.
    failoverManager.pollTriggered();
    
    assertEquals(PRIMARY, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testPrimaryPingedStayAsSecondary() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSecondaryNumber());
    
    failoverManager.start();
    // simulate a polling trigger
    failoverManager.pollTriggered();
    // check we have been promoted, because there is no secondary 1.
    assertEquals(1, failoverManager.getMyInstance().getSecondaryNumber());
    assertEquals(SECONDARY, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.primaryPinged(mockPrimaryPing);
    
    // simulate another polling trigger
    failoverManager.pollTriggered();
    // Primary now exists, check we have NOT been promoted.
    assertEquals(1, failoverManager.getMyInstance().getSecondaryNumber());
    assertEquals(SECONDARY, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testSecondaryNumbersReassigned() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSecondaryNumber());
    
    failoverManager.start();
    failoverManager.getMyInstance().setId(UUID.fromString(UUID2)); // give ourselves a UUID, once ordered will be second in the list
    // simulate a polling trigger
    failoverManager.pollTriggered();
    // check we have been assigned a secondary number 1 - hence no pings received yet
    assertEquals(1, failoverManager.getMyInstance().getSecondaryNumber());
    assertEquals(SECONDARY, failoverManager.getMyInstance().getInstanceType());
    
    // now another secondary pings us, also thinking it is secondary number 1
    mockSecondaryPing.setInstanceId(UUID.fromString(UUID1));
    failoverManager.secondaryPinged(mockSecondaryPing);
    
    // simulate a polling trigger, should reassign our secondary number.
    failoverManager.pollTriggered();
    
    // check we have been re-assigned a secondary number 2
    assertEquals(2, failoverManager.getMyInstance().getSecondaryNumber());
    assertEquals(SECONDARY, failoverManager.getMyInstance().getInstanceType());
    
    failoverManager.stop();
  }
  
  public void testPrimaryConflictHandled() throws Exception {
    // make sure we are primary
    failoverManager = new FailoverManager("myHost", "1111", mockListener, mockBroadcaster, true, 0);
    failoverManager.setPollingThread(mockMonitorThread);
    failoverManager.registerListener(mockStateChangeEventListener);
    failoverManager.setMultiPrimaryConflictHandler(mockMultiPrimaryConflictHandler);
    
    failoverManager.start();
    
    // simulate a polling trigger
    failoverManager.pollTriggered();
    // check we are primary
    assertEquals(0, failoverManager.getMyInstance().getSecondaryNumber());
    assertEquals(PRIMARY, failoverManager.getMyInstance().getInstanceType());
    
    // now ping ourselves from a fictitious other instance that thinks its the primary!
    failoverManager.primaryPinged(mockPrimaryPing);
    
    // simulate another polling trigger
    failoverManager.pollTriggered();
    
    failoverManager.stop();
    // Make sure the conflict handler ran
    verify(mockMultiPrimaryConflictHandler).handle(failoverManager.getMyInstance(), mockPrimaryPing);
  }
  
  public void testSecondaryPromotedToPrimaryThenConflictHandler() throws Exception {
    assertEquals(0, failoverManager.getMyInstance().getSecondaryNumber());
    
    failoverManager.start();
    // simulate a polling trigger
    failoverManager.pollTriggered();
    
    assertEquals(1, failoverManager.getMyInstance().getSecondaryNumber());
    assertEquals(SECONDARY, failoverManager.getMyInstance().getInstanceType());
    
    // simulate another polling trigger which should recognise no primary and promote us.
    failoverManager.pollTriggered();
    
    assertEquals(PRIMARY, failoverManager.getMyInstance().getInstanceType());
    
    // now ping ourselves from a fictitious other instance that thinks its the primary!
    failoverManager.primaryPinged(mockPrimaryPing);
    
    // simulate another polling trigger
    failoverManager.pollTriggered();
    
    failoverManager.stop();
    
    // Make sure the conflict handler ran
    verify(mockMultiPrimaryConflictHandler).handle(failoverManager.getMyInstance(), mockPrimaryPing);
  }

}
