package com.adaptris.failover.util;

public class Constants {
  
  public static final boolean DEBUG = Boolean.getBoolean("interlok.failover.debug");

  public static final int MASTER = 1;
  
  public static final int SLAVE = 2;
  
  public static final String SOCKET_MODE = "failover.socket.mode";
  
  public static final String FAILOVER_TCP_PEERS_KEY = "failover.tcp.peers";
  
  public static final String FAILOVER_TCP_PORT_KEY = "failover.tcp.port";
  
  public static final String FAILOVER_TCP_HOST_KEY = "failover.tcp.host";
  
  public static final String FAILOVER_GROUP_KEY = "failover.multicast.group";
  
  public static final String FAILOVER_PORT_KEY = "failover.multicast.port";
  
  public static final String FAILOVER_SLAVE_NUMBER_KEY = "failover.slave.position";
  
  public static final String FAILOVER_PING_INTERVAL_KEY = "failover.ping.interval.seconds";
  
  public static final String FAILOVER_INSTANCE_TIMEOUT_KEY = "failover.instance.timeout.seconds";
  
  public static final String FAILOVER_JGROUPS_CONFIG_KEY = "failover.jgroups.config.file";
  
  public static final String FAILOVER_JGROUPS_CLUSTER_KEY = "failover.jgroups.cluster.name";
  
  public static final String FAILOVER_DEFAULT_RESOURCE = com.adaptris.core.management.Constants.DEFAULT_PROPS_RESOURCE;
}
