package com.adaptris.failover;

public interface StateChangeEventSender {

  public void notifyPromoteToMaster();
  
  public void notifyPromoteSecondary();
  
  public void notifyAdapterStopped();
  
  public void registerListener(StateChangeEventListener listener);
  
}
