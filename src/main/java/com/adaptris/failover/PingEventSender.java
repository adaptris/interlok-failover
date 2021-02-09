package com.adaptris.failover;

public interface PingEventSender {
  
  public void registerListener(PingEventListener eventListener);
  
  public void deregisterListener(PingEventListener eventListener);
  
  public void sendPrimaryPingEvent(Ping ping);
  
  public void sendSecondaryPingEvent(Ping ping);

}
