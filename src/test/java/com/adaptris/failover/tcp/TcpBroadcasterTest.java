package com.adaptris.failover.tcp;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Properties;
import java.util.UUID;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.failover.NetworkPingSender;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.Constants;

import junit.framework.TestCase;

public class TcpBroadcasterTest extends TestCase {
  
  private static final int MASTER = 1;
  private static final int SLAVE = 2;
  private static final String PEERS = "localhost:4445";
  private static final String HOST = "localhost";
  private static final int HOST_PORT = 4445;
  
  @Mock
  private NetworkPingSender mockNetworkPingSender;
  
  private TcpBroadcaster broadcaster;
  private Ping mockMasterPing;
  private Ping mockSlavePing;
  
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    Properties props = new Properties();
    props.put(Constants.FAILOVER_TCP_PEERS_KEY, PEERS);
    
    broadcaster = new TcpBroadcaster(props);
    broadcaster.setNetworkPingSender(mockNetworkPingSender);
    
    mockMasterPing = new Ping();
    mockMasterPing.setInstanceId(UUID.randomUUID());
    mockMasterPing.setInstanceType(MASTER);
    mockMasterPing.setSlaveNumber(0);
    
    mockSlavePing = new Ping();
    mockSlavePing.setInstanceId(UUID.randomUUID());
    mockSlavePing.setInstanceType(SLAVE);
    mockSlavePing.setSlaveNumber(1);
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
  
  public void testSendSlavePing() throws Exception {
    broadcaster.setPingData(mockSlavePing);
    broadcaster.start();
    
    Thread.sleep(4000); // first ping will be sent after 3 seconds
    
    verify(mockNetworkPingSender).sendData(HOST, HOST_PORT, mockSlavePing);
  }
  
  public void testNoSendToIncorrectHostPortConfig() throws Exception {
    Properties props = new Properties();
    props.put(Constants.FAILOVER_TCP_PEERS_KEY, "localhost:madeUpPort");
    
    broadcaster = new TcpBroadcaster(props);
    broadcaster.setNetworkPingSender(mockNetworkPingSender);
    broadcaster.setPingData(mockSlavePing);
    broadcaster.start();
    
    Thread.sleep(4000); // first ping will be sent after 3 seconds
    
    verify(mockNetworkPingSender, times(0)).sendData(any(String.class), any(int.class), any(Ping.class));
  }

}
