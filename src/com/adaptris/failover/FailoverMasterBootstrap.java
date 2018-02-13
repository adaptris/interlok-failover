package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.FAILOVER_INSTANCE_TIMEOUT_KEY;

import java.util.Properties;

import com.adaptris.core.management.ClasspathInitialiser;

public class FailoverMasterBootstrap extends FailoverBootstrapImp {

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
      
      AdapterEventListener.createInstance(failoverManager);
      
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
    System.err.println("FailoverMasterBootstrap is deprecated, and will be removed for Java9 support");
    ClasspathInitialiser.init(null, false);
    if(arguments.length != 1) {
      doUsage();
    } else
      new FailoverMasterBootstrap().doBootstrap(arguments[0]);
  }


}
