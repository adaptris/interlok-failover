package com.adaptris.failover.tcp;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.UUID;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.failover.Ping;
import com.adaptris.failover.PingEventListener;
import com.adaptris.failover.util.Constants;
import com.adaptris.failover.util.PacketHelper;
import com.adaptris.failover.util.SocketFactory;

import junit.framework.TestCase;

public class TcpListenerTest extends TestCase {
  
  private static final int SECONDARY = 2;
  private static final int PRIMARY = 1;
  
  private static final int PORT = 1;
  
  private Ping mockPrimaryPing;
  private Ping mockSecondaryPing;
  
  private TcpListener tcpListener;
  @Mock
  private SocketFactory mockSocketFactory;
  @Mock  
  private ServerSocket mockServerSocket;
  @Mock
  private Socket mockSocket;
  @Mock
  private PingEventListener mockPingEventListener;
  
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    Properties props = new Properties();
    props.put(Constants.FAILOVER_TCP_PORT_KEY, Integer.toString(PORT));
    
    tcpListener = new TcpListener(props);
    tcpListener.setSocketFactory(mockSocketFactory);
    
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
    
    when(mockSocketFactory.createServerSocket(PORT))
        .thenReturn(mockServerSocket);
  }
  
  public void tearDown() throws Exception {
    tcpListener.deregisterListener(mockPingEventListener);
    tcpListener.stop();
  }
  
  public void testReceivePrimaryPing() throws Exception {
    when(mockServerSocket.accept())
        .thenReturn(mockSocket);
    when(mockSocket.getInputStream())
        .thenReturn(new ByteArrayInputStream(PacketHelper.createDataPacket(mockPrimaryPing)));
    
    tcpListener.registerListener(mockPingEventListener);
    tcpListener.start();
    
    Thread.sleep(3000);
    
    verify(mockPingEventListener).primaryPinged(any(Ping.class));
  }
  
  public void testReceiveSecondaryPing() throws Exception {
    when(mockServerSocket.accept())
        .thenReturn(mockSocket);
    when(mockSocket.getInputStream())
        .thenReturn(new ByteArrayInputStream(PacketHelper.createDataPacket(mockSecondaryPing)));
    
    tcpListener.registerListener(mockPingEventListener);
    tcpListener.start();
    
    Thread.sleep(3000);
    
    verify(mockPingEventListener).secondaryPinged(any(Ping.class));
  }
  
  public void testReceiveErrorOnReadPacketIsHandled() throws Exception {
    when(mockServerSocket.accept())
        .thenReturn(mockSocket);
    when(mockSocket.getInputStream())
        .thenThrow(new IOException());
    
    tcpListener.registerListener(mockPingEventListener);
    tcpListener.start();
    
    Thread.sleep(1000);
    
    verify(mockPingEventListener, times(0)).primaryPinged(any(Ping.class));
    verify(mockPingEventListener, times(0)).primaryPinged(any(Ping.class));
  }
  
  public void testReceiveTimesout() throws Exception {
    when(mockServerSocket.accept())
        .thenThrow(new SocketTimeoutException());
    
    tcpListener.start();
    
    Thread.sleep(1000);
    
    verify(mockPingEventListener, times(0)).secondaryPinged(any(Ping.class));
    verify(mockPingEventListener, times(0)).primaryPinged(any(Ping.class));
  }
  
  public void testReceiveIoExceptionReconnect() throws Exception {
    when(mockServerSocket.accept())
        .thenThrow(new IOException());
    
    tcpListener.registerListener(mockPingEventListener);
    tcpListener.start();
    
    Thread.sleep(7000); // will reconnect after 5 seconds, so we should see 2 connects (first on start, second for reconnect)
    
    verify(mockServerSocket, times(2)).setSoTimeout(any(int.class));
  }
  
  public void testStartupIoException() throws Exception {
    when(mockSocketFactory.createServerSocket(PORT))
        .thenThrow(new IOException());
    
    try {
      tcpListener.start();
      fail("Should throw an exception.");
    } catch (IOException ex) {
      // expected
    }
  }
  
}
