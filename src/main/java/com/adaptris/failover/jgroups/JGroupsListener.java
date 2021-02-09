package com.adaptris.failover.jgroups;

import static com.adaptris.failover.util.Constants.FAILOVER_JGROUPS_CLUSTER_KEY;
import static com.adaptris.failover.util.Constants.FAILOVER_JGROUPS_CONFIG_KEY;
import static com.adaptris.failover.util.PropertiesHelper.getPropertyValue;

import java.util.Properties;

import org.jgroups.Message;
import org.jgroups.Receiver;

import com.adaptris.failover.AbstractListener;
import com.adaptris.failover.Ping;
import com.adaptris.failover.util.PacketHelper;

public class JGroupsListener extends AbstractListener implements Receiver {
  
  private String jGroupsConfigFile;
  
  private String jGroupsClusterName;
  
  public JGroupsListener() {
    super();
  }
  
  public JGroupsListener(Properties bootstrapProperties) {
    this();
    
    this.setjGroupsConfigFile(getPropertyValue(bootstrapProperties, FAILOVER_JGROUPS_CONFIG_KEY));
    this.setjGroupsClusterName(getPropertyValue(bootstrapProperties, FAILOVER_JGROUPS_CLUSTER_KEY));
  }

  @Override
  public void start() throws Exception {
    JGroupsChannel jGroupsChannel = JGroupsChannel.getInstance();
    jGroupsChannel.setConfigFileLocation(this.getjGroupsConfigFile());
    jGroupsChannel.setClusterName(this.getjGroupsClusterName());
    jGroupsChannel.getJGroupsChannel().setReceiver(this);
    
    jGroupsChannel.start();
  }
  
  @Override
  public void receive(Message msg) {    
    int byteCount = msg.getLength();
    if(byteCount != PacketHelper.STANDARD_PACKET_SIZE) {
      log.warn("Incorrect packet size ({}) received on the TCP socket, ignoring this data stream.", byteCount);
    } else {
      sendPingEvent(msg.getBuffer());
    }
  }


  private void sendPingEvent(byte[] data) {
    Ping pingRecord = PacketHelper.createPingRecord(data);
    if(PacketHelper.isPrimaryPing(pingRecord))
      this.sendPrimaryPingEvent(pingRecord);
    else
      this.sendSecondaryPingEvent(pingRecord);
  }
  
  @Override
  public void stop() {
    JGroupsChannel.getInstance().stop();
  }

  public String getjGroupsConfigFile() {
    return jGroupsConfigFile;
  }

  public void setjGroupsConfigFile(String jGroupsConfigFile) {
    this.jGroupsConfigFile = jGroupsConfigFile;
  }

  public String getjGroupsClusterName() {
    return jGroupsClusterName;
  }

  public void setjGroupsClusterName(String jGroupsClusterName) {
    this.jGroupsClusterName = jGroupsClusterName;
  }

}
