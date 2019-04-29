package com.adaptris.failover.jgroups;

import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JGroupsChannel {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());

  private static JGroupsChannel INSTANCE;
  
  private JChannel jGroupsChannel;
  
  private String configFileLocation;
  
  private String clusterName;
  
  public static JGroupsChannel getInstance() {
    if(INSTANCE == null) {
      INSTANCE = new JGroupsChannel();
    }
    
    return INSTANCE;
  }
  
  public JChannel getJGroupsChannel() throws Exception {
    if(jGroupsChannel == null) {
      jGroupsChannel = new JChannel(this.getConfigFileLocation());
    }
    
    return jGroupsChannel;
  }
  
  public void start() throws Exception {
    if(!this.getJGroupsChannel().isConnected())
      this.getJGroupsChannel().connect(this.getClusterName());
  }
  
  public void stop() {
    try {
      if(this.getJGroupsChannel().isConnected()) {
        this.getJGroupsChannel().disconnect();
        this.getJGroupsChannel().close();
      }
    } catch (Exception ex) {
      log.error("Failed to stop the JChannel, continuing.", ex);
    }
  }
  
  

  public String getConfigFileLocation() {
    return configFileLocation;
  }

  public void setJGroupsChannel(JChannel jGroupsChannel) {
    this.jGroupsChannel = jGroupsChannel;
  }
  
  public void setConfigFileLocation(String configFileLocation) {
    this.configFileLocation = configFileLocation;
  }
  
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

}
