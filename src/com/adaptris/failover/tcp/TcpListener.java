package com.adaptris.failover.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.failover.Listener;
import com.adaptris.failover.Ping;
import com.adaptris.failover.PingEventListener;
import com.adaptris.failover.util.PacketHelper;
import com.adaptris.failover.util.SocketFactory;

public class TcpListener implements Listener {

protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  private List<PingEventListener> listeners;
  private ServerSocket socket;
  private int port;
  private SocketFactory socketFactory;
  private volatile boolean shutdownRequested;
  
  public TcpListener(final int port) {
    this.setPort(port);
    
    shutdownRequested = false;
    listeners = new ArrayList<PingEventListener>();
    this.setSocketFactory(new SocketFactory());
  }
  
  public void start() throws IOException {
    try {
      socketConnect();
      
      (new Thread("Listener Thread") {
        public void run() {
          while (!shutdownRequested) {
            try {
              new ClientSocketHandlerThread(socket.accept()).start();              
            } catch (SocketTimeoutException e) {
              
            } catch (final IOException e) {
              if(!shutdownRequested) {
                log.error(e.getMessage(), e);
                try {
                  Thread.sleep(5000);
                  socketConnect();
                } catch (Exception e1) {
                }
              }
            }
          }
        }
      }).start();
      
    } catch (IOException ex) {
      log.error("Error with the TCP Listener, cannot start it up.");
      throw ex;
    }
  }
  
  private void socketConnect() throws IOException {
    socket = this.getSocketFactory().createServerSocket(this.getPort());
    socket.setSoTimeout(30000);
  }

  public void stop() {
    this.shutdownRequested = true;
    if(socket != null) {
      try {
        socket.close();
      } catch (Exception ex) {
        ;
      }
    }
  }

  private void sendPingEvent(byte[] data) {
    Ping pingRecord = PacketHelper.createPingRecord(data);
    if(PacketHelper.isMasterPing(pingRecord))
      this.sendMasterPingEvent(pingRecord);
    else
      this.sendSlavePingEvent(pingRecord);
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public void registerListener(PingEventListener eventListener) {
    this.listeners.add(eventListener);
  }

  @Override
  public void deregisterListener(PingEventListener eventListener) {
    this.listeners.remove(eventListener);
  }

  @Override
  public void sendMasterPingEvent(Ping ping) {
    for(PingEventListener listener : this.listeners)
      listener.masterPinged(ping);
  }

  @Override
  public void sendSlavePingEvent(Ping ping) {
    for(PingEventListener listener : this.listeners)
      listener.slavePinged(ping);
  }
  
  class ClientSocketHandlerThread extends Thread {
    
    Socket clientSocket;
    
    ClientSocketHandlerThread(Socket clientSocket) {
      super("Client Socket Handler");
      this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
      byte[] data = new byte[PacketHelper.STANDARD_PACKET_SIZE];
      
      try {
        int byteCount = clientSocket.getInputStream().read(data);
        if(byteCount != PacketHelper.STANDARD_PACKET_SIZE) {
          log.warn("Incorrect packet size ({}) received on the TCP socket, ignoring this data stream.", byteCount);
        } else {
          sendPingEvent(data);
        }
      } catch (IOException e) {
        log.warn("Error reading the TCP socket data, ignoring this data stream.", e);
      }
    }
    
  }

  public SocketFactory getSocketFactory() {
    return socketFactory;
  }

  public void setSocketFactory(SocketFactory socketFactory) {
    this.socketFactory = socketFactory;
  }

}
