package com.adaptris.failover;

public interface Listener extends PingEventSender {

  public void start();
  
  public void stop();
  
}
