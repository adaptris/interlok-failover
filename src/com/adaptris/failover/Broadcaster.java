package com.adaptris.failover;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Broadcaster {
  
  private static final int DEFAULT_SEND_DELAY_SECONDS = 3;
  
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> schedulerHandle;
  private MulticastSocket socket;
  private String group;
  private short port;
  private DatagramPacket packet;
  private int sendDelaySeconds;
  
  public Broadcaster(final String group, final short port, DatagramPacket packet) {
    this.setGroup(group);
    this.setPort(port);
    this.setPacket(packet);
    this.setSendDelaySeconds(DEFAULT_SEND_DELAY_SECONDS);
  }
  
  public void start() throws IOException {
    socket = new MulticastSocket(this.getPort());
    socket.joinGroup(InetAddress.getByName(this.getGroup()));
    socket.setTimeToLive((byte) 1); // 1 byte ttl for subnet only
    
    scheduler = Executors.newScheduledThreadPool(1);
    
    final Runnable broadcastRunnable = new Runnable() {
      @Override
      public void run() {
        try {
          socket.send(getPacket());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    
    this.schedulerHandle = this.scheduler.schedule(broadcastRunnable, this.getSendDelaySeconds(), TimeUnit.SECONDS);
  }
  
  public void stop() {
    this.schedulerHandle.cancel(true);
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public short getPort() {
    return port;
  }

  public void setPort(short port) {
    this.port = port;
  }

  public DatagramPacket getPacket() {
    return packet;
  }

  public void setPacket(DatagramPacket packet) {
    this.packet = packet;
  }

  public int getSendDelaySeconds() {
    return sendDelaySeconds;
  }

  public void setSendDelaySeconds(int sendDelaySeconds) {
    this.sendDelaySeconds = sendDelaySeconds;
  }

}
