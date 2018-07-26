package com.adaptris.failover;

public interface PingEventListener {
  
  public void masterPinged(Ping ping);
  
  public void slavePinged(Ping ping);
  
  public boolean equals(PingEventListener eventListener);

}
