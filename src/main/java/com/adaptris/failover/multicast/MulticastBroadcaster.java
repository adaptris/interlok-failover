package com.adaptris.failover.multicast;

import static com.adaptris.failover.util.Constants.FAILOVER_GROUP_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_PORT_KEY;
import static com.adaptris.failover.util.PropertiesHelper.getPropertyValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.failover.Broadcaster;
import com.adaptris.failover.NetworkPingSender;
import com.adaptris.failover.Peer;
import com.adaptris.failover.Ping;

public class MulticastBroadcaster implements Broadcaster {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  private static final int DEFAULT_SEND_DELAY_SECONDS = 3;
  
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> schedulerHandle;
  
  private String group;
  private int port;
  private Ping pingData;
  private int sendDelaySeconds;
  private List<Peer> peers;
  private NetworkPingSender networkPingSender;
  
  public MulticastBroadcaster(Properties bootstrapProperties) {
    this.setGroup(getPropertyValue(bootstrapProperties, FAILOVER_GROUP_KEY));
    this.setPort(Integer.parseInt(getPropertyValue(bootstrapProperties, FAILOVER_PORT_KEY)));
    this.setSendDelaySeconds(DEFAULT_SEND_DELAY_SECONDS);
    this.setNetworkPingSender(new MulticastNetworkPingSender());
    this.setPeers(new ArrayList<>());
  }
  
  public void start() throws IOException {
    try {
      this.getNetworkPingSender().initialize(getGroup(), getPort());
    } catch (Exception e1) {
      throw new IOException(e1);
    }
    
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
          getNetworkPingSender().sendData(getGroup(), getPort(), getPingData());
        } catch (Exception e) {
          log.warn("Could not send ping data, will try again.", e);
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
    this.getNetworkPingSender().Stop();
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

  public NetworkPingSender getNetworkPingSender() {
    return networkPingSender;
  }

  public void setNetworkPingSender(NetworkPingSender networkPingSender) {
    this.networkPingSender = networkPingSender;
  }

  @Override
  public List<Peer> getPeers() {
    return this.peers;
  }

  public void setPeers(List<Peer> peers) {
    this.peers = peers;
  }

}
