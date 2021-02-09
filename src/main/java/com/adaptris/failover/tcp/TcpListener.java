package com.adaptris.failover.tcp;

import static com.adaptris.failover.util.Constants.FAILOVER_TCP_PORT_KEY;
import static com.adaptris.failover.util.PropertiesHelper.getPropertyValue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;

import com.adaptris.failover.AbstractListener;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.PacketHelper;
import com.adaptris.failover.util.SocketFactory;

public class TcpListener extends AbstractListener {
  
  private ServerSocket socket;
  private int port;
  private SocketFactory socketFactory;
  private volatile boolean shutdownRequested;
  
  public TcpListener() {
    super();
  }
  
  public TcpListener(Properties bootstrapProperties) {
    this();
    this.setPort(Integer.parseInt(getPropertyValue(bootstrapProperties, FAILOVER_TCP_PORT_KEY)));
    
    shutdownRequested = false;
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
      this.sendSecondaryPingEvent(pingRecord);
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
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
