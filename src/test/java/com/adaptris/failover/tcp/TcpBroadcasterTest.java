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
  
  private static final int PRIMARY = 1;
  private static final int SECONDARY = 2;
  private static final String PEERS = "localhost:4445";
  private static final String HOST = "localhost";
  private static final int HOST_PORT = 4445;
  
  @Mock
  private NetworkPingSender mockNetworkPingSender;
  
  private TcpBroadcaster broadcaster;
  private Ping mockPrimaryPing;
  private Ping mockSecondaryPing;
  
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    Properties props = new Properties();
    props.put(Constants.FAILOVER_TCP_PEERS_KEY, PEERS);
    
    broadcaster = new TcpBroadcaster(props);
    broadcaster.setNetworkPingSender(mockNetworkPingSender);
    
    mockPrimaryPing = new Ping();
    mockPrimaryPing.setInstanceId(UUID.randomUUID());
    mockPrimaryPing.setInstanceType(PRIMARY);
    mockPrimaryPing.setSecondaryNumber(0);
    
    mockSecondaryPing = new Ping();
    mockSecondaryPing.setInstanceId(UUID.randomUUID());
    mockSecondaryPing.setInstanceType(SECONDARY);
    mockSecondaryPing.setSecondaryNumber(1);
  }
  
  public void tearDown() throws Exception {
    broadcaster.stop();
  }

  public void testSendPrimaryPing() throws Exception {
    broadcaster.setPingData(mockPrimaryPing);
    broadcaster.start();
    
    Thread.sleep(4000); // first ping will be sent after 3 seconds
    
    verify(mockNetworkPingSender).sendData(HOST, HOST_PORT, mockPrimaryPing);
  }
  
  public void testSendSecondaryPing() throws Exception {
    broadcaster.setPingData(mockSecondaryPing);
    broadcaster.start();
    
    Thread.sleep(4000); // first ping will be sent after 3 seconds
    
    verify(mockNetworkPingSender).sendData(HOST, HOST_PORT, mockSecondaryPing);
  }
  
  public void testNoSendToIncorrectHostPortConfig() throws Exception {
    Properties props = new Properties();
    props.put(Constants.FAILOVER_TCP_PEERS_KEY, "localhost:madeUpPort");
    
    broadcaster = new TcpBroadcaster(props);
    broadcaster.setNetworkPingSender(mockNetworkPingSender);
    broadcaster.setPingData(mockSecondaryPing);
    broadcaster.start();
    
    Thread.sleep(4000); // first ping will be sent after 3 seconds
    
    verify(mockNetworkPingSender, times(0)).sendData(any(String.class), any(int.class), any(Ping.class));
  }

}
