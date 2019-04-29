package com.adaptris.failover.jgroups;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.Properties;
import java.util.UUID;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.failover.Ping;
import com.adaptris.failover.PingEventListener;
import com.adaptris.failover.util.Constants;
import com.adaptris.failover.util.PacketHelper;

import junit.framework.TestCase;

public class JGroupsListenerTest extends TestCase {
  
  private static final int SLAVE = 2;
  private static final int MASTER = 1;
    
  private static final String CLUSTER_NAME = "myClusterName";
  private static final String CONFIG_FILE = "./myConfigFile";
  
  private Ping mockMasterPing;
  private Ping mockSlavePing;
    
  private JGroupsListener jGroupsListener;
  @Mock
  private PingEventListener mockPingEventListener;
  @Mock
  private JChannel mockJChannel;
  
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    JGroupsChannel.getInstance().setJGroupsChannel(mockJChannel);
    
    Properties props = new Properties();
    props.put(Constants.FAILOVER_JGROUPS_CONFIG_KEY, CONFIG_FILE);
    props.put(Constants.FAILOVER_JGROUPS_CLUSTER_KEY, CLUSTER_NAME);
    
    jGroupsListener = new JGroupsListener(props);
        
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
    jGroupsListener.deregisterListener(mockPingEventListener);
    jGroupsListener.stop();
  }
  
  public void testReceiveMasterPing() throws Exception {
    jGroupsListener.registerListener(mockPingEventListener);
    jGroupsListener.start();
    
    jGroupsListener.receive(new Message(null, PacketHelper.createDataPacket(mockMasterPing)));
    
    Thread.sleep(3000);
    
    verify(mockPingEventListener).masterPinged(any(Ping.class));
  }
  
  public void testReceiveSlavePing() throws Exception {
    jGroupsListener.registerListener(mockPingEventListener);
    jGroupsListener.start();
    
    jGroupsListener.receive(new Message(null, PacketHelper.createDataPacket(mockSlavePing)));
    
    Thread.sleep(3000);
    
    verify(mockPingEventListener).slavePinged(any(Ping.class));
  }
  
}
