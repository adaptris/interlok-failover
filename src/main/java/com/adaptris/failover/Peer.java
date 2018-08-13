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
  
  public boolean equals(Object object) {
    if(object instanceof Peer) {
      if((((Peer) object).getHost().equals(this.getHost()))
          && (((Peer) object).getPort() == this.getPort()))
        return true;
    } else
      return false;
    return false;
  }
}
