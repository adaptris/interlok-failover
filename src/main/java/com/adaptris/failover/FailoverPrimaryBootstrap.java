package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.FAILOVER_INSTANCE_TIMEOUT_KEY;

import java.util.Properties;

public class FailoverPrimaryBootstrap extends FailoverBootstrapImp {

  private FailoverManager failoverManager;
  
  @Override
  protected void startFailover(Properties bootstrapProperties) {
    log.info("Starting Interlok instance in failover mode as the primary.");
    
    try {
      failoverManager = new FailoverManager(this.determineMyHost(bootstrapProperties), this.determineMyPort(bootstrapProperties), listener, broadcaster, true, 0);
      if(bootstrapProperties.containsKey(FAILOVER_INSTANCE_TIMEOUT_KEY))
        failoverManager.setInstanceTimeoutSeconds(Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_INSTANCE_TIMEOUT_KEY)));
    
      failoverManager.registerListener(this);
      failoverManager.start();
      
      AdapterEventListener.createInstance(failoverManager);
      
      promoteToPrimary();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  protected void stopFailover() {
    if(failoverManager != null)
      failoverManager.stop();
  }


}
