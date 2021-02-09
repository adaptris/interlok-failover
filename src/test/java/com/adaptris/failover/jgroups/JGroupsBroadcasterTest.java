package com.adaptris.failover.jgroups;

import static org.mockito.Mockito.verify;

import java.util.Properties;
import java.util.UUID;

import org.jgroups.JChannel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.failover.NetworkPingSender;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.Constants;

import junit.framework.TestCase;

public class JGroupsBroadcasterTest extends TestCase {
  
  private static final int MASTER = 1;
  private static final int SECONDARY = 2;
  private static final String CLUSTER_NAME = "myClusterName";
  private static final String CONFIG_FILE = "./myConfigFile";
  private static final String HOST = null;
  private static final int HOST_PORT = 0;
  
  @Mock
  private NetworkPingSender mockNetworkPingSender;
  @Mock
  private JChannel mockJChannel;
  
  private JGroupsBroadcaster broadcaster;
  private Ping mockMasterPing;
  private Ping mockSecondaryPing;
  
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    JGroupsChannel.getInstance().setJGroupsChannel(mockJChannel);
    
    Properties props = new Properties();
    props.put(Constants.FAILOVER_JGROUPS_CONFIG_KEY, CONFIG_FILE);
    props.put(Constants.FAILOVER_JGROUPS_CLUSTER_KEY, CLUSTER_NAME);
    
    broadcaster = new JGroupsBroadcaster(props);
    broadcaster.setNetworkPingSender(mockNetworkPingSender);
    
    mockMasterPing = new Ping();
    mockMasterPing.setInstanceId(UUID.randomUUID());
    mockMasterPing.setInstanceType(MASTER);
    mockMasterPing.setSecondaryNumber(0);
    
    mockSecondaryPing = new Ping();
    mockSecondaryPing.setInstanceId(UUID.randomUUID());
    mockSecondaryPing.setInstanceType(SECONDARY);
    mockSecondaryPing.setSecondaryNumber(1);
  }
  
  public void tearDown() throws Exception {
    broadcaster.stop();
  }

  public void testSendMasterPing() throws Exception {
    broadcaster.setPingData(mockMasterPing);
    broadcaster.start();
    
    Thread.sleep(4000); // first ping will be sent after 3 seconds
    
    verify(mockNetworkPingSender).sendData(HOST, HOST_PORT, mockMasterPing);
  }
  
  public void testSendSecondaryPing() throws Exception {
    broadcaster.setPingData(mockSecondaryPing);
    broadcaster.start();
    
    Thread.sleep(4000); // first ping will be sent after 3 seconds
    
    verify(mockNetworkPingSender).sendData(HOST, HOST_PORT, mockSecondaryPing);
  }

}