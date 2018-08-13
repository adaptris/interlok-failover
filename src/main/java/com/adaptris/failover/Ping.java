package com.adaptris.failover;

import java.util.UUID;

public class Ping {
  
  private UUID instanceId;
  
  private String sourceHost;
  
  private String sourcePort;
  
  // 1 = Master, 2 = Slave
  private int instanceType;
  
  private int slaveNumber;
  
  public Ping() {
  }

  public UUID getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(UUID instanceId) {
    this.instanceId = instanceId;
  }

  public int getSlaveNumber() {
    return slaveNumber;
  }

  public void setSlaveNumber(int slaveNumber) {
    this.slaveNumber = slaveNumber;
  }

  public int getInstanceType() {
    return instanceType;
  }

  public void setInstanceType(int instanceType) {
    this.instanceType = instanceType;
  }
  
  public String getSourceHost() {
    return sourceHost;
  }

  public void setSourceHost(String sourceHost) {
    this.sourceHost = sourceHost;
  }

  public String getSourcePort() {
    return sourcePort;
  }

  public void setSourcePort(String sourcePort) {
    this.sourcePort = sourcePort;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(this.getInstanceId().toString());
    sb.append(" :: ");
    sb.append(this.getInstanceType() == 1 ? "MASTER" : "SLAVE");
    sb.append(" :: ");
    sb.append("Slave number: " + this.getSlaveNumber());
    sb.append(" :: ");
    sb.append("Source host: " + this.getSourceHost());
    sb.append(" :: ");
    sb.append("Source port: " + this.getSourcePort());
    return sb.toString();
  }

}
