package com.adaptris.failover;

import java.util.Objects;

public class Peer {

  private String host;
  private int port;

  public Peer(String host, int port) {
    setHost(host);
    setPort(port);
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

  @Override
  public boolean equals(Object object) {
    if(object instanceof Peer) {
      if(((Peer) object).getHost().equals(getHost())
          && ((Peer) object).getPort() == getPort()) {
        return true;
      }
    } else {
      return false;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getHost(), getPort());
  }

}
