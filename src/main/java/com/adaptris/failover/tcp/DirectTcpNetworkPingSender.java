package com.adaptris.failover.tcp;

import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.adaptris.failover.NetworkPingSender;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.PacketHelper;

public class DirectTcpNetworkPingSender implements NetworkPingSender {
  
  private static final int TIMEOUT_CONNECT = 10000;

  private Map<String, InetSocketAddress> cachedAddresses;
  
  public DirectTcpNetworkPingSender() {
    cachedAddresses = new HashMap<>();
  }

  @Override
  public void sendData(String host, int port, Ping data) throws Exception {
    Socket socket = null;
    
    String mapKey = host + Integer.toString(port);
    InetSocketAddress inetSocketAddress = this.cachedAddresses.get(mapKey);
    if(inetSocketAddress == null) {
      inetSocketAddress = new InetSocketAddress(host, port);
      this.cachedAddresses.put(mapKey, inetSocketAddress);
    }
    
    try {
      socket = new Socket();
      socket.connect(inetSocketAddress, TIMEOUT_CONNECT);
      
      DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
      outToServer.write(PacketHelper.createDataPacket(data));
    } finally {
      socket.close();
    }
  }

  @Override
  public void initialize(String host, int port) throws Exception {
  }

  @Override
  public void Stop() {
  }

}
