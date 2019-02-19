package com.adaptris.failover;

public interface Listener extends PingEventSender {

  public void start() throws Exception;
  
  public void stop();
  
}
