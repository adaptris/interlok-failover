package com.adaptris.failover.util;

import java.io.IOException;
import java.net.MulticastSocket;
import java.net.ServerSocket;

public class SocketFactory {
  
  public ServerSocket createServerSocket(int port) throws IOException {
    return new ServerSocket(port);
  }
  
  public MulticastSocket createMulticastSocket(int port) throws IOException {
    return new MulticastSocket(port);
  }

}
