package com.adaptris.failover.tcp;

import static org.mockito.Matchers.any;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.UUID;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.failover.Ping;
import com.adaptris.failover.PingEventListener;
import com.adaptris.failover.util.PacketHelper;
import com.adaptris.failover.util.SocketFactory;

import junit.framework.TestCase;

public class TcpListenerTest extends TestCase {
  
  private static final int SLAVE = 2;
  private static final int MASTER = 1;
  
  private static final int PORT = 1;
  
  private Ping mockMasterPing;
  private Ping mockSlavePing;
  
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
    
    tcpListener = new TcpListener(PORT);
    tcpListener.setSocketFactory(mockSocketFactory);
    
    mockMasterPing = new Ping();
    mockMasterPing.setInstanceId(UUID.randomUUID());
    mockMasterPing.setInstanceType(MASTER);
    mockMasterPing.setSlaveNumber(0);
    
    mockSlavePing = new Ping();
    mockSlavePing.setInstanceId(UUID.randomUUID());
    mockSlavePing.setInstanceType(SLAVE);
    mockSlavePing.setSlaveNumber(1);
    
    when(mockSocketFactory.createServerSocket(PORT))
        .thenReturn(mockServerSocket);
  }
  
  public void tearDown() throws Exception {
    tcpListener.deregisterListener(mockPingEventListener);
    tcpListener.stop();
  }
  
  public void testReceiveMasterPing() throws Exception {
    when(mockServerSocket.accept())
        .thenReturn(mockSocket);
    when(mockSocket.getInputStream())
        .thenReturn(new ByteArrayInputStream(PacketHelper.createDataPacket(mockMasterPing)));
    
    tcpListener.registerListener(mockPingEventListener);
    tcpListener.start();
    
    Thread.sleep(3000);
    
    verify(mockPingEventListener).masterPinged(any(Ping.class));
  }
  
  public void testReceiveSlavePing() throws Exception {
    when(mockServerSocket.accept())
        .thenReturn(mockSocket);
    when(mockSocket.getInputStream())
        .thenReturn(new ByteArrayInputStream(PacketHelper.createDataPacket(mockSlavePing)));
    
    tcpListener.registerListener(mockPingEventListener);
    tcpListener.start();
    
    Thread.sleep(3000);
    
    verify(mockPingEventListener).slavePinged(any(Ping.class));
  }
  
  public void testReceiveErrorOnReadPacketIsHandled() throws Exception {
    when(mockServerSocket.accept())
        .thenReturn(mockSocket);
    when(mockSocket.getInputStream())
        .thenThrow(new IOException());
    
    tcpListener.registerListener(mockPingEventListener);
    tcpListener.start();
    
    Thread.sleep(1000);
    
    verify(mockPingEventListener, times(0)).masterPinged(any(Ping.class));
    verify(mockPingEventListener, times(0)).masterPinged(any(Ping.class));
  }
  
  public void testReceiveTimesout() throws Exception {
    when(mockServerSocket.accept())
        .thenThrow(new SocketTimeoutException());
    
    tcpListener.start();
    
    Thread.sleep(1000);
    
    verify(mockPingEventListener, times(0)).slavePinged(any(Ping.class));
    verify(mockPingEventListener, times(0)).masterPinged(any(Ping.class));
  }
  
  public void testReceiveIoExceptionReconnect() throws Exception {
    when(mockServerSocket.accept())
        .thenThrow(new IOException());
    
    tcpListener.registerListener(mockPingEventListener);
    tcpListener.start();
    
    Thread.sleep(6000); // will reconnect after 5 seconds, so we should see 2 connects (first on start, second for reconnect)
    
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
