package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.FAILOVER_GROUP_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_PORT_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_SLAVE_NUMBER_KEY;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailoverSlaveBootstrap extends FailoverBootstrap {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());

  @Override
  protected void startFailover(Properties bootstrapProperties) {
    log.info("Starting Interlok instance in failover mode as a slave.");
    
    broadcaster = new Broadcaster(bootstrapProperties.getProperty(FAILOVER_GROUP_KEY), Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PORT_KEY)));
    listener = new Listener(bootstrapProperties.getProperty(FAILOVER_GROUP_KEY), Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PORT_KEY)));
    
    String slaveNumber = bootstrapProperties.getProperty(FAILOVER_SLAVE_NUMBER_KEY);
    int slavePosition = 0;
    if(!StringUtils.isEmpty(slaveNumber)) {
      slavePosition = Integer.parseInt(slaveNumber);
      log.info("Slave position " + slavePosition);
    } else {
      log.info("No slave position has been set, one will be allocated.");
    }
    FailoverManager failoverManager = new FailoverManager(listener, broadcaster, false, slavePosition);
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
      new FailoverSlaveBootstrap().doBootstrap(arguments[0]);
  }

}
