package com.adaptris.failover.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class DirectTcpNetworkPingSender implements NetworkPingSender {

  @Override
  public void initialize(String host, int port) throws IOException {
    // TODO Auto-generated method stub
    SocketAddress peer = new InetSocketAddress(hostPortSplit[0], Integer.parseInt(hostPortSplit[1]));
  }

  @Override
  public void sendData(byte[] data) throws Exception {
    // TODO Auto-generated method stub
    
  }

}
