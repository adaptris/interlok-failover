package com.adaptris.failover;

public class Peer {
  
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
