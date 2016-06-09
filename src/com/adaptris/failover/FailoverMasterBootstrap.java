package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.FAILOVER_INSTANCE_TIMEOUT_KEY;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailoverMasterBootstrap extends FailoverBootstrap {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  private FailoverManager failoverManager;
  
  @Override
  protected void startFailover(Properties bootstrapProperties) {
    log.info("Starting Interlok instance in failover mode as the master.");
    
    failoverManager = new FailoverManager(listener, broadcaster, true, 0);
    if(bootstrapProperties.containsKey(FAILOVER_INSTANCE_TIMEOUT_KEY))
      failoverManager.setInstanceTimeoutSeconds(Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_INSTANCE_TIMEOUT_KEY)));
    try {
      failoverManager.registerListener(this);
      failoverManager.start();
      
      promoteToMaster();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  protected void stopFailover() {
    if(failoverManager != null)
      failoverManager.stop();
  }
  
  public static final void main(String[] arguments) {
    if(arguments.length != 1) {
      doUsage();
    } else
      new FailoverMasterBootstrap().doBootstrap(arguments[0]);
  }

}
