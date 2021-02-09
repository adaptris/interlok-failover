package com.adaptris.failover;

public interface StateChangeEventListener {

  public void promoteToMaster();
  
  public void promoteSecondary(int position);
  
  public void adapterStopped();
  
}
