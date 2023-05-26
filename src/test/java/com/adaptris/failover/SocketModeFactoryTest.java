package com.adaptris.failover;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adaptris.failover.SocketModeFactory.NetworkMode;
import com.adaptris.failover.jgroups.JGroupsBroadcaster;
import com.adaptris.failover.jgroups.JGroupsListener;
import com.adaptris.failover.multicast.MulticastBroadcaster;
import com.adaptris.failover.multicast.MulticastListener;
import com.adaptris.failover.tcp.TcpBroadcaster;
import com.adaptris.failover.tcp.TcpListener;
import com.adaptris.failover.util.Constants;

public class SocketModeFactoryTest {

  private static final String CLUSTER_NAME = "myClusterName";
  private static final String CONFIG_FILE = "./myConfigFile";
  private static final int PORT = 1;
  private static final String GROUP = "204.0.0.1";

  private static final String TCP_MODE = "tcp";
  private static final String MULTICAST_MODE = "multicast";
  private static final String JGROUPS_MODE = "jgroups";

  Properties props = null;

  @BeforeEach
  public void setUp() throws Exception {
    props = new Properties();
    props.put(Constants.FAILOVER_JGROUPS_CONFIG_KEY, CONFIG_FILE);
    props.put(Constants.FAILOVER_JGROUPS_CLUSTER_KEY, CLUSTER_NAME);
    props.put(Constants.FAILOVER_TCP_PORT_KEY, Integer.toString(PORT));
    props.put(Constants.FAILOVER_GROUP_KEY, GROUP);
    props.put(Constants.FAILOVER_PORT_KEY, Integer.toString(PORT));
  }

  @Test
  public void testTcpSocketMode() throws Exception {
    props.put(Constants.SOCKET_MODE, TCP_MODE);

    NetworkMode networkMode = SocketModeFactory.create(props);

    assertTrue(networkMode.getBroadcaster() instanceof TcpBroadcaster);
    assertTrue(networkMode.getListener() instanceof TcpListener);
  }

  @Test
  public void testMulicastSocketMode() throws Exception {
    props.put(Constants.SOCKET_MODE, MULTICAST_MODE);

    NetworkMode networkMode = SocketModeFactory.create(props);

    assertTrue(networkMode.getBroadcaster() instanceof MulticastBroadcaster);
    assertTrue(networkMode.getListener() instanceof MulticastListener);
  }

  @Test
  public void testJGroupsSocketMode() throws Exception {
    props.put(Constants.SOCKET_MODE, JGROUPS_MODE);

    NetworkMode networkMode = SocketModeFactory.create(props);

    assertTrue(networkMode.getBroadcaster() instanceof JGroupsBroadcaster);
    assertTrue(networkMode.getListener() instanceof JGroupsListener);
  }

  @Test
  public void testDefaultMulicastSocketMode() throws Exception {
    NetworkMode networkMode = SocketModeFactory.create(props);

    assertTrue(networkMode.getBroadcaster() instanceof MulticastBroadcaster);
    assertTrue(networkMode.getListener() instanceof MulticastListener);
  }

}
