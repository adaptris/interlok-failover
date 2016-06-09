package com.adaptris.failover;

public class AdapterEventListener {
  
  private static AdapterEventListener instance;
  
  private FailoverManager failoverManager;
  
  private AdapterEventListener(FailoverManager failoverManager) {
    this.failoverManager = failoverManager;
  }
  
  public static AdapterEventListener getInstance() {
    if(instance == null)
      return null;
    
    return instance;
  }
  
  public static void createInstance(FailoverManager failoverManager) {
    instance = new AdapterEventListener(failoverManager);
  }
  
  public void adapterStopEvent() {
    failoverManager.stop();
  }

}
