package com.adaptris.failover.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.failover.Listener;
import com.adaptris.failover.Ping;
import com.adaptris.failover.PingEventListener;
import com.adaptris.failover.util.PacketHelper;

public class MulticastListener implements Listener {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  private List<PingEventListener> listeners;
  private MulticastSocket socket;
  private String group;
  private int port;
  private volatile boolean shutdownRequested;
  
  public MulticastListener(final String group, final int port) {
    this.setGroup(group);
    this.setPort(port);
    
    shutdownRequested = false;
    listeners = new ArrayList<PingEventListener>();
  }
  
  public void start() {
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
      ex.printStackTrace();
      log.error("Error with the Multicast Listener, cannot start it up.");
    }
  }
  
  private void socketConnect() throws IOException {
    socket = new MulticastSocket(this.getPort());
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
      this.sendSlavePingEvent(pingRecord);
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

}
