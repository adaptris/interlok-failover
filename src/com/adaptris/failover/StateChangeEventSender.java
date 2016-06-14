package com.adaptris.failover;

public interface StateChangeEventSender {

  public void notifyPromoteToMaster();
  
  public void notifyPromoteSlave();
  
  public void notifyAdapterStopped();
  
  public void registerListener(StateChangeEventListener listener);
  
}
