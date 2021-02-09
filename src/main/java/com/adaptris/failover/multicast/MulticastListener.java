package com.adaptris.failover.multicast;

import static com.adaptris.failover.util.Constants.FAILOVER_GROUP_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_PORT_KEY;
import static com.adaptris.failover.util.PropertiesHelper.getPropertyValue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Properties;

import com.adaptris.failover.AbstractListener;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.PacketHelper;
import com.adaptris.failover.util.SocketFactory;

public class MulticastListener extends AbstractListener {
    
  private MulticastSocket socket;
  private String group;
  private int port;
  private SocketFactory socketFactory;
  private volatile boolean shutdownRequested;
  
  public MulticastListener() {
    super();
  }
  
  public MulticastListener(Properties bootstrapProperties) {
    this();
    this.setGroup(getPropertyValue(bootstrapProperties, FAILOVER_GROUP_KEY));
    this.setPort(Integer.parseInt(getPropertyValue(bootstrapProperties, FAILOVER_PORT_KEY)));
    
    shutdownRequested = false;
    this.setSocketFactory(new SocketFactory());
  }
  
  public void start() throws IOException {
    try {
      socketConnect();
      
      (new Thread("Listener Thread") {
        public void run() {
          final byte[] udpPacket = new byte[PacketHelper.STANDARD_PACKET_SIZE];
          while (!shutdownRequested) {
            try {
              final DatagramPacket packet = new DatagramPacket(udpPacket, udpPacket.length);
              socket.receive(packet);
              
              sendPingEvent(packet);              
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
      log.error("Error with the Multicast Listener, cannot start it up.");
      throw ex;
    }
  }
  
  private void socketConnect() throws IOException {
    socket = this.getSocketFactory().createMulticastSocket(this.getPort());
    socket.setReuseAddress(true);
    socket.setSoTimeout(30000);
    socket.joinGroup(InetAddress.getByName(this.getGroup()));
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

  private void sendPingEvent(DatagramPacket packet) {
    Ping pingRecord = PacketHelper.createPingRecord(packet);
    if(PacketHelper.isMasterPing(pingRecord))
      this.sendMasterPingEvent(pingRecord);
    else
      this.sendSecondaryPingEvent(pingRecord);
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public SocketFactory getSocketFactory() {
    return socketFactory;
  }

  public void setSocketFactory(SocketFactory socketFactory) {
    this.socketFactory = socketFactory;
  }

}
