package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.FAILOVER_INSTANCE_TIMEOUT_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_SLAVE_NUMBER_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_TCP_HOST_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_TCP_PORT_KEY;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.adaptris.core.management.ClasspathInitialiser;

public class FailoverBootstrap extends FailoverBootstrapImp {

  private FailoverManager failoverManager;

  @Override
  protected void startFailover(Properties bootstrapProperties) {
    log.info("Starting Interlok instance in failover mode as a slave.");
        
    String slaveNumber = this.getPropertyValue(bootstrapProperties, FAILOVER_SLAVE_NUMBER_KEY);
    int slavePosition = 0;
    if(!StringUtils.isEmpty(slaveNumber)) {
      slavePosition = Integer.parseInt(slaveNumber);
      log.info("Slave position " + slavePosition);
    } else {
      log.info("No slave position has been set, one will be allocated.");
    }
    
    
    try {
      failoverManager = new FailoverManager(determineMyHost(bootstrapProperties), determineMyPort(bootstrapProperties), listener, broadcaster, false, slavePosition);
      if(bootstrapProperties.containsKey(FAILOVER_INSTANCE_TIMEOUT_KEY))
        failoverManager.setInstanceTimeoutSeconds(Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_INSTANCE_TIMEOUT_KEY)));
          
      failoverManager.registerListener(this);
      failoverManager.start();
      
      AdapterEventListener.createInstance(failoverManager);
      
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  protected void stopFailover() {
    if(failoverManager != null)
      failoverManager.stopFailoverManager();
  }
  
  public static void main(String[] arguments) throws Exception {
    System.err.println("FailoverBootstrap is deprecated, and will be removed for Java9 support");
    ClasspathInitialiser.init(null, false);
    if(arguments.length != 1) {
      doUsage();
    } else
      new FailoverBootstrap().doBootstrap(arguments[0]);
  }

  private String getPropertyValue(Properties properties, String key) {
    String propertyValue = System.getProperty(key);
    if(propertyValue == null) {
      return properties.getProperty(key);
    }
    return propertyValue;
  }

}
