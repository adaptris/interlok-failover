package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.FAILOVER_GROUP_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_PORT_KEY;

import java.util.Properties;

public class FailoverMasterBootstrap extends FailoverBootstrap {
  
  @Override
  protected void startFailover(Properties bootstrapProperties) {
    broadcaster = new Broadcaster(bootstrapProperties.getProperty(FAILOVER_GROUP_KEY), Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PORT_KEY)));
    listener = new Listener(bootstrapProperties.getProperty(FAILOVER_GROUP_KEY), Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PORT_KEY)));
    
    FailoverManager failoverManager = new FailoverManager(listener, broadcaster, true, 0);
    try {
      failoverManager.registerListener(this);
      failoverManager.start();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  public static final void main(String[] arguments) {
    if(arguments.length != 1) {
      doUsage();
    } else
      new FailoverMasterBootstrap().doBootstrap(arguments[0]);
  }

}
