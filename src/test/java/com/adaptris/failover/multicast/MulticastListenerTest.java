package com.adaptris.failover.multicast;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.failover.Ping;
import com.adaptris.failover.PingEventListener;
import com.adaptris.failover.util.Constants;
import com.adaptris.failover.util.PacketHelper;
import com.adaptris.failover.util.SocketFactory;

public class MulticastListenerTest {

  private static final int SECONDARY = 2;
  private static final int PRIMARY = 1;

  private static final String GROUP = "204.0.0.1";
  private static final int PORT = 1;

  private Ping mockPrimaryPing;
  private Ping mockSecondaryPing;

  private MulticastListener multicastListener;
  @Mock
  private SocketFactory mockSocketFactory;
  @Mock
  private MulticastSocket mockServerSocket;
  @Mock
  private PingEventListener mockPingEventListener;

  private AutoCloseable openMocks;

  @BeforeEach
  public void setUp() throws Exception {
    openMocks = MockitoAnnotations.openMocks(this);

    Properties props = new Properties();
    props.put(Constants.FAILOVER_GROUP_KEY, GROUP);
    props.put(Constants.FAILOVER_PORT_KEY, Integer.toString(PORT));

    multicastListener = new MulticastListener(props);
    multicastListener.setSocketFactory(mockSocketFactory);

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

    when(mockSocketFactory.createMulticastSocket(PORT)).thenReturn(mockServerSocket);
  }

  @AfterEach
  public void tearDown() throws Exception {
    multicastListener.deregisterListener(mockPingEventListener);
    multicastListener.stop();
    openMocks.close();
  }

  @Test
  public void testReceivePrimaryPing() throws Exception {
    // when we call receive, set the datagram packet to our mocked one.
    doAnswer(invocation -> {
      DatagramPacket arg = (DatagramPacket) invocation.getArguments()[0];
      try {
        arg.setData(PacketHelper.createDataPacket(mockPrimaryPing));
        multicastListener.stop(); // stop it looping forever, trying to receive more packets
      } catch (UnknownHostException e) {
        fail(e.getMessage());
      }
      return null;
    }).when(mockServerSocket).receive(any(DatagramPacket.class));

    multicastListener.registerListener(mockPingEventListener);
    multicastListener.start();

    Thread.sleep(1000);

    verify(mockPingEventListener).primaryPinged(any(Ping.class));
  }

  @Test
  public void testReceiveSecondaryPing() throws Exception {
    // when we call receive, set the datagram packet to our mocked one.
    doAnswer(invocation -> {
      DatagramPacket arg = (DatagramPacket) invocation.getArguments()[0];
      try {
        arg.setData(PacketHelper.createDataPacket(mockSecondaryPing));
        multicastListener.stop(); // stop it looping forever, trying to receive more packets
      } catch (UnknownHostException e) {
        fail(e.getMessage());
      }
      return null;
    }).when(mockServerSocket).receive(any(DatagramPacket.class));

    multicastListener.registerListener(mockPingEventListener);
    multicastListener.start();

    Thread.sleep(1000);

    verify(mockPingEventListener).secondaryPinged(any(Ping.class));
  }

  @Test
  public void testReceiveTimesout() throws Exception {
    // when we call receive, first time throw an exception, then shut it all down
    doThrow(new SocketTimeoutException()).doAnswer(invocation -> {
      multicastListener.stop(); // stop it looping forever, trying to receive more packets
      return null;
    }).when(mockServerSocket).receive(any(DatagramPacket.class));

    multicastListener.start();

    Thread.sleep(1000);

    verify(mockPingEventListener, times(0)).secondaryPinged(any(Ping.class));
    verify(mockPingEventListener, times(0)).primaryPinged(any(Ping.class));
  }

  @Test
  public void testReceiveIoExceptionReconnect() throws Exception {
    doThrow(new IOException()).when(mockServerSocket).receive(any(DatagramPacket.class));

    multicastListener.registerListener(mockPingEventListener);
    multicastListener.start();

    Thread.sleep(6000); // will reconnect after 5 seconds, so we should see 2 connects (first on start, second for reconnect)

    verify(mockServerSocket, times(2)).setSoTimeout(any(int.class));
  }

  @Test
  public void testStartupIoException() throws Exception {
    when(mockSocketFactory.createMulticastSocket(PORT))
        .thenThrow(new IOException());

    try {
      multicastListener.start();
      fail("Should throw an exception.");
    } catch (IOException ex) {
      // expected
    }
  }

}
