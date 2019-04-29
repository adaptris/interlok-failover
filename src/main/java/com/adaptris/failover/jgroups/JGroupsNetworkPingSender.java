package com.adaptris.failover.jgroups;

import org.jgroups.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.failover.NetworkPingSender;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.PacketHelper;

public class JGroupsNetworkPingSender implements NetworkPingSender {

  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  @Override
  public void sendData(String host, int port, Ping data) throws Exception {
    JGroupsChannel jGroupsChannel = JGroupsChannel.getInstance();
    if(jGroupsChannel.getJGroupsChannel().isConnected())
      jGroupsChannel.getJGroupsChannel().send(new Message(null, PacketHelper.createDataPacket(data)));
    else
      log.warn("JGroupsChannel not connected, therefore skipping the send.");
  }

  @Override
  public void Stop() {
    // do nothing
  }

  @Override
  public void initialize(String host, int port) throws Exception {
    // do nothing
  }
}
