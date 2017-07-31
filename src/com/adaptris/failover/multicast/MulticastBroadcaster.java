package com.adaptris.failover.multicast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.failover.Ping;
import com.adaptris.failover.util.PacketHelper;

public class MulticastBroadcaster {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  private static final int DEFAULT_SEND_DELAY_SECONDS = 3;
  
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> schedulerHandle;
  private MulticastSocket socket;
  private String group;
  private int port;
  private Ping pingData;
  private int sendDelaySeconds;
  
  public MulticastBroadcaster(final String group, final int port) {
    this.setGroup(group);
    this.setPort(port);
    this.setSendDelaySeconds(DEFAULT_SEND_DELAY_SECONDS);
  }
  
  public void start() throws IOException {
    socket = new MulticastSocket(this.getPort());
    socket.joinGroup(InetAddress.getByName(this.getGroup()));
    socket.setTimeToLive((byte) 1); // 1 byte ttl for subnet only
    
    scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
        return new Thread(runnable, "Broadcast Thread");
      }
    });
    
    final Runnable broadcastRunnable = new Runnable() {
      @Override
      public void run() {
        try {
          socket.send(PacketHelper.createDatagramPacket(getPingData(), group, port));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    
    this.schedulerHandle = this.scheduler.scheduleWithFixedDelay(broadcastRunnable, this.getSendDelaySeconds(), this.getSendDelaySeconds(), TimeUnit.SECONDS);
  }
  
  public void stop() {
    if(schedulerHandle != null) {
      this.schedulerHandle.cancel(true);
      scheduler.shutdownNow();
    }
    if(socket != null) {
      try {
        socket.close();
      } catch (Exception ex) {
        ;
      }
    }
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

  public int getSendDelaySeconds() {
    return sendDelaySeconds;
  }

  public void setSendDelaySeconds(int sendDelaySeconds) {
    this.sendDelaySeconds = sendDelaySeconds;
  }

  public Ping getPingData() {
    return pingData;
  }

  public void setPingData(Ping pingData) {
    this.pingData = pingData;
  }

}
