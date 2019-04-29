package com.adaptris.failover.jgroups;

import static com.adaptris.failover.util.Constants.FAILOVER_JGROUPS_CLUSTER_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_JGROUPS_CONFIG_KEY;
import static com.adaptris.failover.util.PropertiesHelper.getPropertyValue;

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

public class JGroupsBroadcaster implements Broadcaster {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  private static final int DEFAULT_SEND_DELAY_SECONDS = 3;
  
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> schedulerHandle;
  private Ping pingData;
  private int sendDelaySeconds;
  private NetworkPingSender networkPingSender;
  private List<Peer> peers;
  private String jGroupsConfigFile;
  private String jGroupsClusterName;

  public JGroupsBroadcaster(Properties bootstrapProperties) {
    this.setjGroupsConfigFile(getPropertyValue(bootstrapProperties, FAILOVER_JGROUPS_CONFIG_KEY));
    this.setjGroupsClusterName(getPropertyValue(bootstrapProperties, FAILOVER_JGROUPS_CLUSTER_KEY));
    this.setSendDelaySeconds(DEFAULT_SEND_DELAY_SECONDS);
    this.setNetworkPingSender(new JGroupsNetworkPingSender());
  }
  
  @Override
  public void start() throws Exception {
    JGroupsChannel jGroupsChannel = JGroupsChannel.getInstance();
    jGroupsChannel.setConfigFileLocation(this.getjGroupsConfigFile());
    jGroupsChannel.setClusterName(this.getjGroupsClusterName());
    jGroupsChannel.start();
    
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
          getNetworkPingSender().sendData(null, 0, getPingData());
        } catch (Exception e) {
          log.warn("Error trying to broadcast to others in the cluster; continuing.", e);
        }
      }
    };

    this.schedulerHandle = this.scheduler.scheduleWithFixedDelay(broadcastRunnable, this.getSendDelaySeconds(), this.getSendDelaySeconds(), TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    if (schedulerHandle != null) {
      this.schedulerHandle.cancel(true);
      scheduler.shutdownNow();
    }
    this.getNetworkPingSender().Stop();
    JGroupsChannel.getInstance().stop();
  }

  @Override
  public List<Peer> getPeers() {
    return peers;
  }

  public Ping getPingData() {
    return pingData;
  }

  public void setPingData(Ping pingData) {
    this.pingData = pingData;
  }

  public int getSendDelaySeconds() {
    return sendDelaySeconds;
  }

  public void setSendDelaySeconds(int sendDelaySeconds) {
    this.sendDelaySeconds = sendDelaySeconds;
  }

  public NetworkPingSender getNetworkPingSender() {
    return networkPingSender;
  }

  public void setNetworkPingSender(NetworkPingSender networkPingSender) {
    this.networkPingSender = networkPingSender;
  }

  public String getjGroupsConfigFile() {
    return jGroupsConfigFile;
  }

  public void setjGroupsConfigFile(String jGroupsConfigFile) {
    this.jGroupsConfigFile = jGroupsConfigFile;
  }

  public String getjGroupsClusterName() {
    return jGroupsClusterName;
  }

  public void setjGroupsClusterName(String jGroupsClusterName) {
    this.jGroupsClusterName = jGroupsClusterName;
  }

}
