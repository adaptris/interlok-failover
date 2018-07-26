package com.adaptris.failover.multicast;

import java.net.InetAddress;
import java.net.MulticastSocket;

import com.adaptris.failover.NetworkPingSender;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.PacketHelper;

public class MulticastNetworkPingSender implements NetworkPingSender {
  
  private MulticastSocket socket;

  @Override
  public void sendData(String host, int port, Ping data) throws Exception {
    socket.send(PacketHelper.createDatagramPacket(data, host, port));
  }

  @Override
  public void initialize(String host, int port) throws Exception {
    socket = new MulticastSocket(port);
    socket.joinGroup(InetAddress.getByName(host));
    socket.setTimeToLive((byte) 1); // 1 byte ttl for subnet only
  }

  @Override
  public void Stop() {
    socket.close();
  }

}
