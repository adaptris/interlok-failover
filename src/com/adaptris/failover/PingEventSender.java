package com.adaptris.failover;

public interface PingEventSender {
  
  public void registerListener(PingEventListener eventListener);
  
  public void deregisterListener(PingEventListener eventListener);
  
  public void sendMasterPingEvent(Ping ping);
  
  public void sendSlavePingEvent(Ping ping);

}
