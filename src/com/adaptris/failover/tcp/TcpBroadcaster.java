package com.adaptris.failover.tcp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.failover.Broadcaster;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.DirectTcpNetworkPingSender;
import com.adaptris.failover.util.NetworkPingSender;
import com.adaptris.failover.util.PacketHelper;

public class TcpBroadcaster implements Broadcaster {

  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());

  private static final int DEFAULT_SEND_DELAY_SECONDS = 3;
  
  private static final int TIMEOUT_CONNECT = 10000;

  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> schedulerHandle;
  private Ping pingData;
  private int sendDelaySeconds;
  private String peersString;
  private List<Peer> peers;
  private NetworkPingSender networkPingSender;

  public TcpBroadcaster(final String peers) {
    this.setSendDelaySeconds(DEFAULT_SEND_DELAY_SECONDS);
    this.setPeers(new ArrayList<Peer>());
    this.setNetworkPingSender(new DirectTcpNetworkPingSender());
  }

  public void start() throws IOException {
    this.setPeers(decodePeers());
    this.getNetworkPingSender().initialize(null, 0);
    
    scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
        return new Thread(runnable, "Broadcast Thread");
      }
    });

    final Runnable broadcastRunnable = new Runnable() {
      @Override
      public void run() {
        for (Peer peer : getPeers()) {
          Socket socket = null;
          try {
            socket = new Socket();
            socket.connect(peer, TIMEOUT_CONNECT);
            
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            outToServer.write(PacketHelper.createDataPacket(getPingData()));

          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            try {
              socket.close();
            } catch (IOException e) {
              // Silently
            }
          }
        }
      }
    };

    this.schedulerHandle = this.scheduler.scheduleWithFixedDelay(broadcastRunnable, this.getSendDelaySeconds(), this.getSendDelaySeconds(), TimeUnit.SECONDS);
  }

  private List<Peer> decodePeers() {
    List<Peer> results = new ArrayList<>();
    
    String[] peersSplit = this.getPeersString().split(";");
    for(String singlePeer : peersSplit) {
      String[] hostPortSplit = singlePeer.split(":");
      if(hostPortSplit.length == 2) {
        try {
          Peer peer = new Peer(hostPortSplit[0], Integer.parseInt(hostPortSplit[1]));
          results.add(peer);
        } catch (Exception e) {
          log.warn("Peer address could not be understood, ignoring: " + singlePeer);
        }
      } else
        log.warn("Peer address could not be understood, ignoring: " + singlePeer); 
      
    }
    return results;
  }

  public void stop() {
    if (schedulerHandle != null) {
      this.schedulerHandle.cancel(true);
      scheduler.shutdownNow();
    }
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

  public String getPeersString() {
    return peersString;
  }

  public void setPeersString(String peers) {
    this.peersString = peers;
  }

  public List<Peer> getPeers() {
    return peers;
  }

  public void setPeers(List<Peer> peers) {
    this.peers = peers;
  }
  
  public NetworkPingSender getNetworkPingSender() {
    return networkPingSender;
  }

  public void setNetworkPingSender(NetworkPingSender networkPingSender) {
    this.networkPingSender = networkPingSender;
  }

  class Peer {
    
    private String host;
    private int port;
    
    public Peer(String host, int port) {
      this.setHost(host);
      this.setPort(port);
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }
    
  }
}
