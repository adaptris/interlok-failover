package com.adaptris.failover;

public interface StateChangeEventListener {

  public void promoteToPrimary();
  
  public void promoteSecondary(int position);
  
  public void adapterStopped();
  
}
