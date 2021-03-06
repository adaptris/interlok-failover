package com.adaptris.failover.multicast;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.failover.NetworkPingSender;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.Constants;

import junit.framework.TestCase;

public class MulticastBroadcasterTest extends TestCase {
  
  private static final int PRIMARY = 1;
  private static final int SECONDARY = 2;
  private static final String GROUP = "204.0.0.1";
  private static final int PORT = 1;
  
  @Mock
  private NetworkPingSender mockNetworkPingSender;
  
  private MulticastBroadcaster broadcaster;
  private Ping mockPrimaryPing;
  private Ping mockSecondaryPing;
  
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    Properties props = new Properties();
    props.put(Constants.FAILOVER_GROUP_KEY, GROUP);
    props.put(Constants.FAILOVER_PORT_KEY, Integer.toString(PORT));
    
    broadcaster = new MulticastBroadcaster(props);
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
    
    verify(mockNetworkPingSender).sendData(GROUP, PORT, mockPrimaryPing);
  }
  
  public void testSendSecondaryPing() throws Exception {
    broadcaster.setPingData(mockSecondaryPing);
    broadcaster.start();
    
    Thread.sleep(4000); // first ping will be sent after 3 seconds
    
    verify(mockNetworkPingSender).sendData(GROUP, PORT, mockSecondaryPing);
  }
  
  public void testStartupError() throws Exception {
    doThrow(new IOException())
        .when(mockNetworkPingSender).initialize(GROUP, PORT);
    
    try {
      broadcaster.start();
      fail("Should fail on startup.");
    } catch (IOException ex) {
      // expected
    }
  }
  
}
