package com.adaptris.failover;

import static com.adaptris.core.management.Constants.CFG_KEY_START_QUIETLY;
import static com.adaptris.failover.util.Constants.FAILOVER_GROUP_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_PING_INTERVAL_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_PORT_KEY;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.management.BootstrapProperties;
import com.adaptris.core.management.ClasspathInitialiser;
import com.adaptris.core.management.ShutdownHandler;
import com.adaptris.core.management.SystemPropertiesUtil;
import com.adaptris.core.management.UnifiedBootstrap;
import com.adaptris.core.management.VersionReport;
import com.adaptris.core.management.logging.LoggingConfigurator;
import com.adaptris.core.runtime.AdapterManagerMBean;
import com.adaptris.core.util.ManagedThreadFactory;
import com.adaptris.failover.util.PropertiesHelper;

public abstract class FailoverBootstrapImp implements StateChangeEventListener {
  
  private BootstrapProperties bootProperties;
  private UnifiedBootstrap bootstrap;
  
  protected AdapterManagerMBean adapterMBean;
  
  protected Broadcaster broadcaster;
  protected Listener listener;
  
  private String bootstrapResource;
  
  private volatile boolean isRunning;

  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  protected static void doUsage() {
    System.out.println("Only one mandatory parameter is required for the failover bootstrap; the name of the bootstrap.properties file on the classpath.");
  }

  protected void doBootstrap(String bootstrapPropertiesResource) {
    try {
      bootstrapResource = bootstrapPropertiesResource;
      Properties bootstrapProperties = PropertiesHelper.loadFromFile(bootstrapPropertiesResource);
      
      broadcaster = new Broadcaster(bootstrapProperties.getProperty(FAILOVER_GROUP_KEY), Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PORT_KEY)));
      listener = new Listener(bootstrapProperties.getProperty(FAILOVER_GROUP_KEY), Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PORT_KEY)));
      
      if(bootstrapProperties.containsKey(FAILOVER_PING_INTERVAL_KEY))
        broadcaster.setSendDelaySeconds(Integer.parseInt(bootstrapProperties.getProperty(FAILOVER_PING_INTERVAL_KEY)));
      
      Runtime.getRuntime().addShutdownHook(new FailoverShutdownHandler());
      
      doStandardBoot();
      
      startFailover(bootstrapProperties);
      
      isRunning = true;
      
    } catch (Exception e) {
      System.out.println("Failed to load bootstrap.properties from '" + bootstrapPropertiesResource + "'");
      e.printStackTrace();
    }
  }

  protected abstract void startFailover(Properties bootstrapProperties);
  
  protected abstract void stopFailover();
  
  @Override
  public void adapterStopped() {
    if(isRunning) {
      if(adapterMBean != null) {
        try {
          isRunning = false;
          bootProperties.getConfigManager().getAdapterRegistry().destroyAdapter(adapterMBean);
        } catch (Exception e) {
          log.error("Attempting to destory adapter failed.");
          log.error(e.getMessage());
        }
      }
    }
  }
  
  public void promoteToMaster() {
    try {
      log.info("Promoting to MASTER");
      
      boolean startQuietly = Boolean.valueOf(bootProperties.getProperty(CFG_KEY_START_QUIETLY, "true")).booleanValue();
      bootstrap.init(adapterMBean);
      Runtime.getRuntime().addShutdownHook(new ShutdownHandler(bootProperties));
      launchAdapter(bootstrap, startQuietly);
      
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  public void promoteSlave(int position) {
    log.info("Promoting slave to position " + position);
  }
  
  public class FailoverShutdownHandler extends Thread {
    public void run() {
      isRunning = false;
      stopFailover();
    }
  }
  
  private void doStandardBoot() throws Exception {
    ClasspathInitialiser.init(null, false);
    
    VersionReport r = VersionReport.getInstance();
    log.info(String.format("Bootstrap of Interlok %1$s complete", r.getAdapterBuildVersion()));
    
    LoggingConfigurator.newConfigurator().defaultInitialisation();
    bootProperties = new BootstrapProperties(bootstrapResource);
    SystemPropertiesUtil.addSystemProperties(bootProperties);
    SystemPropertiesUtil.addJndiProperties(bootProperties);
    
    bootstrap = new UnifiedBootstrap(bootProperties);
    adapterMBean = bootstrap.createAdapter();
  }

  //Jira 154 : If the adapter is configured with a shared connection that has a fixed-number of retries
  // which fails, then it throws an exception back to "main" which can terminate the JVM depending
  // on how you're starting it.
  // Not so much if you're using java -jar, but definitely if you're using the wrapper script.
  private void launchAdapter(final UnifiedBootstrap bootstrap, boolean quietly) throws Exception {
    final String threadName = this.getClass().getSimpleName();
    if (quietly) {
      Thread launcher = new ManagedThreadFactory().newThread(new Runnable() {
        @Override
        public void run() {
          Thread.currentThread().setName(threadName);
          try {
            bootstrap.start();
          }
          catch (Exception e) {
            System.err.println("(Error) Adapter Startup failure :" + e.getMessage());
            e.printStackTrace();
          }
        }
      });
      launcher.setDaemon(false);
      launcher.start();
    }
    else {
      bootstrap.start();
    }
  }
}
