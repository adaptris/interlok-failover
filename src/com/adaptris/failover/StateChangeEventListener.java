package com.adaptris.failover;

public interface StateChangeEventListener {

  public void promoteToMaster();
  
  public void promoteSlave(int position);
  
}
