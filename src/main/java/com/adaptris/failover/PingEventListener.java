package com.adaptris.failover;

public interface PingEventListener {
  
  public void primaryPinged(Ping ping);
  
  public void secondaryPinged(Ping ping);
  
  public boolean equals(PingEventListener eventListener);

}
