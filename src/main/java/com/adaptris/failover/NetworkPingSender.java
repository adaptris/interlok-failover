package com.adaptris.failover;

public interface NetworkPingSender {
  
  public void initialize(String host, int port) throws Exception;
  
  public void sendData(String host, int port, Ping data) throws Exception;
  
  public void Stop();
  
}
