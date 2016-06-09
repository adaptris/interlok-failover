package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.FAILOVER_GROUP_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_PING_INTERVAL_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_PORT_KEY;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.management.SimpleBootstrap;
import com.adaptris.failover.util.PropertiesHelper;

public abstract class FailoverBootstrap implements StateChangeEventListener {
  
  protected Broadcaster broadcaster;
  protected Listener listener;
  
  private String bootstrapResource;

  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  protected static void doUsage() {
    System.out.println("Only one mandatory parameter is required for the failover bootstrap; the url to the bootstrap.properties");
  }

  protected void doBootstrap(String bootstrapPropertiesResource) {
    try {
      bootstrapResource = bootstrapPropertiesResource;
      Properties bootstrapProperties = PropertiesHelper.loadFromFile(bootstrapPropertiesResource);
      
      broadcaster = new Broadcaster(bootstrapProperties.getProperty(FAILOVER_GROUP_KEY), Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PORT_KEY)));
      listener = new Listener(bootstrapProperties.getProperty(FAILOVER_GROUP_KEY), Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PORT_KEY)));
      
      if(bootstrapProperties.containsKey(FAILOVER_PING_INTERVAL_KEY))
        broadcaster.setSendDelaySeconds(Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PING_INTERVAL_KEY)));
      
      Runtime.getRuntime().addShutdownHook(new ShutdownHandler());
      
      startFailover(bootstrapProperties);
      
    } catch (Exception e) {
      System.out.println("Failed to load bootstrap.properties from '" + bootstrapPropertiesResource + "'");
      e.printStackTrace();
    }
  }

  protected abstract void startFailover(Properties bootstrapProperties);
  
  protected abstract void stopFailover();
  
  public void promoteToMaster() {
    try {
      log.info("Promoting to MASTER");
      new SimpleBootstrap(new String[] {bootstrapResource}).boot();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  public void promoteSlave(int position) {
    log.info("Promoting slave to position " + position);
  }
  
  public class ShutdownHandler extends Thread {
    public void run() {
      stopFailover();
    }
  }

}
