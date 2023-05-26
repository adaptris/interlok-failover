package com.adaptris.failover.multicast;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.failover.NetworkPingSender;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.Constants;

public class MulticastBroadcasterTest {

  private static final int PRIMARY = 1;
  private static final int SECONDARY = 2;
  private static final String GROUP = "204.0.0.1";
  private static final int PORT = 1;

  @Mock
  private NetworkPingSender mockNetworkPingSender;

  private AutoCloseable openMocks;

  private MulticastBroadcaster broadcaster;
  private Ping mockPrimaryPing;
  private Ping mockSecondaryPing;

  @BeforeEach
  public void setUp() throws Exception {
    openMocks = MockitoAnnotations.openMocks(this);

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

  @AfterEach
  public void tearDown() throws Exception {
    broadcaster.stop();
    openMocks.close();
  }

  @Test
  public void testSendPrimaryPing() throws Exception {
    broadcaster.setPingData(mockPrimaryPing);
    broadcaster.start();

    Thread.sleep(4000); // first ping will be sent after 3 seconds

    verify(mockNetworkPingSender).sendData(GROUP, PORT, mockPrimaryPing);
  }

  @Test
  public void testSendSecondaryPing() throws Exception {
    broadcaster.setPingData(mockSecondaryPing);
    broadcaster.start();

    Thread.sleep(4000); // first ping will be sent after 3 seconds

    verify(mockNetworkPingSender).sendData(GROUP, PORT, mockSecondaryPing);
  }

  @Test
  public void testStartupError() throws Exception {
    doThrow(new IOException()).when(mockNetworkPingSender).initialize(GROUP, PORT);

    try {
      broadcaster.start();
      fail("Should fail on startup.");
    } catch (IOException ex) {
      // expected
    }
  }

}
