package com.adaptris.failover;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractListener implements Listener {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  private List<PingEventListener> listeners;
  
  public AbstractListener() {
    listeners = new ArrayList<PingEventListener>();
  }
  
  @Override
  public void sendMasterPingEvent(Ping ping) {
    for(PingEventListener listener : this.getListeners())
      listener.masterPinged(ping);
  }

  @Override
  public void sendSecondaryPingEvent(Ping ping) {
    for(PingEventListener listener : this.getListeners())
      listener.secondaryPinged(ping);
  }
  
  @Override
  public void registerListener(PingEventListener eventListener) {
    this.listeners.add(eventListener);
  }

  @Override
  public void deregisterListener(PingEventListener eventListener) {
    this.listeners.remove(eventListener);
  }

  public List<PingEventListener> getListeners() {
    return listeners;
  }

}
