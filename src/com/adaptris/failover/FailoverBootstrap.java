package com.adaptris.failover;

import java.util.Properties;

import com.adaptris.failover.util.PropertiesHelper;

public abstract class FailoverBootstrap {

  protected static void doUsage() {
    System.out.println("Only one mandatory parameter is required for the failover bootstrap; the url to the bootstrap.properties");
  }

  protected void doBootstrap(String bootstrapProprtiesResource) {
    try {
      Properties bootstrapProperties = PropertiesHelper.loadFromFile(bootstrapProprtiesResource);
      
      startFailover(bootstrapProperties);
      
    } catch (Exception e) {
      System.out.println("Failed to load bootstrap.properties from '" + bootstrapProprtiesResource + "'");
      e.printStackTrace();
    }
  }

  protected abstract void startFailover(Properties bootstrapProperties);

}
