package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.SOCKET_MODE;

import java.util.Properties;

import com.adaptris.failover.jgroups.JGroupsBroadcaster;
import com.adaptris.failover.jgroups.JGroupsListener;
import com.adaptris.failover.multicast.MulticastBroadcaster;
import com.adaptris.failover.multicast.MulticastListener;
import com.adaptris.failover.tcp.TcpBroadcaster;
import com.adaptris.failover.tcp.TcpListener;

public class SocketModeFactory {
  
  private static final String SOCKET_MODE_TCP = "tcp";
  private static final String SOCKET_MODE_JGROUPS = "jgroups";
  private static final String SOCKET_MODE_MULTICAST = "multicast";

  public static NetworkMode create(Properties bootstrapProperties) {
    String socketMode = bootstrapProperties.getProperty(SOCKET_MODE);
    NetworkMode networkMode = new SocketModeFactory().new NetworkMode();
    
    socketMode = (socketMode == null ? SOCKET_MODE_MULTICAST : socketMode);
    
    switch (socketMode) {
      case SOCKET_MODE_TCP:
        networkMode.setBroadcaster(new TcpBroadcaster(bootstrapProperties));
        networkMode.setListener(new TcpListener(bootstrapProperties));
        
        break;
        
      case SOCKET_MODE_JGROUPS:
        networkMode.setBroadcaster(new JGroupsBroadcaster(bootstrapProperties));
        networkMode.setListener(new JGroupsListener(bootstrapProperties));
        break;
  
      default:
        networkMode.setBroadcaster(new MulticastBroadcaster(bootstrapProperties));
        networkMode.setListener(new MulticastListener(bootstrapProperties));
        break;
    }
    
    return networkMode;
  }
  
  class NetworkMode {
    private Broadcaster broadcaster;
    private Listener listener;
    
    public NetworkMode() {
    }

    public Broadcaster getBroadcaster() {
      return broadcaster;
    }

    public void setBroadcaster(Broadcaster broadcaster) {
      this.broadcaster = broadcaster;
    }

    public Listener getListener() {
      return listener;
    }

    public void setListener(Listener listener) {
      this.listener = listener;
    }
    
    
  }
  
}
