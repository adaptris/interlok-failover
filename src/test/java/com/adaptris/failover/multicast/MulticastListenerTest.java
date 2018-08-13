package com.adaptris.failover.multicast;

import static org.mockito.Matchers.any;
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
import java.util.UUID;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.adaptris.failover.Ping;
import com.adaptris.failover.PingEventListener;
import com.adaptris.failover.util.PacketHelper;
import com.adaptris.failover.util.SocketFactory;

import junit.framework.TestCase;

public class MulticastListenerTest extends TestCase {
  
  private static final int SLAVE = 2;
  private static final int MASTER = 1;
  
  private static final String GROUP = "204.0.0.1";
  private static final int PORT = 1;
  
  private Ping mockMasterPing;
  private Ping mockSlavePing;
  
  private MulticastListener multicastListener;
  @Mock
  private SocketFactory mockSocketFactory;
  @Mock  
  private MulticastSocket mockServerSocket;
  @Mock
  private PingEventListener mockPingEventListener;
  
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    multicastListener = new MulticastListener(GROUP, PORT);
    multicastListener.setSocketFactory(mockSocketFactory);
    
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
    
    when(mockSocketFactory.createMulticastSocket(PORT))
        .thenReturn(mockServerSocket);
  }
  
  public void tearDown() throws Exception {
    multicastListener.deregisterListener(mockPingEventListener);
    multicastListener.stop();
  }
  
  public void testReceiveMasterPing() throws Exception {
    // when we call receive, set the datagram packet to our mocked one.
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
          DatagramPacket arg = (DatagramPacket) invocation.getArguments()[0];
          try {
            arg.setData(PacketHelper.createDataPacket(mockMasterPing)); 
            multicastListener.stop(); // stop it looping forever, trying to receive more packets
          } catch (UnknownHostException e) {
            fail(e.getMessage());
          }
          return null;
      }
    }).when(mockServerSocket).receive(any(DatagramPacket.class));
    
    
    multicastListener.registerListener(mockPingEventListener);
    multicastListener.start();
    
    Thread.sleep(1000);
    
    verify(mockPingEventListener).masterPinged(any(Ping.class));
  }
  
  public void testReceiveSlavePing() throws Exception {
    // when we call receive, set the datagram packet to our mocked one.
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
          DatagramPacket arg = (DatagramPacket) invocation.getArguments()[0];
          try {
            arg.setData(PacketHelper.createDataPacket(mockSlavePing)); 
            multicastListener.stop(); // stop it looping forever, trying to receive more packets
          } catch (UnknownHostException e) {
            fail(e.getMessage());
          }
          return null;
      }
    }).when(mockServerSocket).receive(any(DatagramPacket.class));
    
    multicastListener.registerListener(mockPingEventListener);
    multicastListener.start();
    
    Thread.sleep(1000);
    
    verify(mockPingEventListener).slavePinged(any(Ping.class));
  }
  
  
  public void testReceiveTimesout() throws Exception {
    // when we call receive, first time throw an exception, then shut it all down
    doThrow(new SocketTimeoutException())
    .doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        multicastListener.stop(); // stop it looping forever, trying to receive more packets
        return null;
    }
  }).when(mockServerSocket).receive(any(DatagramPacket.class));
    
    multicastListener.start();
    
    Thread.sleep(1000);
    
    verify(mockPingEventListener, times(0)).slavePinged(any(Ping.class));
    verify(mockPingEventListener, times(0)).masterPinged(any(Ping.class));
  }
  
  public void testReceiveIoExceptionReconnect() throws Exception {
    doThrow(new IOException())
        .when(mockServerSocket).receive(any(DatagramPacket.class));
    
    multicastListener.registerListener(mockPingEventListener);
    multicastListener.start();
    
    Thread.sleep(6000); // will reconnect after 5 seconds, so we should see 2 connects (first on start, second for reconnect)
    
    verify(mockServerSocket, times(2)).setSoTimeout(any(int.class));
  }
  
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
