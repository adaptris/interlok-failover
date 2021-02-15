package com.adaptris.failover;

import java.util.UUID;

public class Ping {
  
  private UUID instanceId;
  
  private String sourceHost;
  
  private String sourcePort;
  
  // 1 = Primary, 2 = Secondary
  private int instanceType;
  
  private int secondaryNumber;
  
  public Ping() {
  }

  public UUID getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(UUID instanceId) {
    this.instanceId = instanceId;
  }

  public int getSecondaryNumber() {
    return secondaryNumber;
  }

  public void setSecondaryNumber(int secondaryNumber) {
    this.secondaryNumber = secondaryNumber;
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
    sb.append(this.getInstanceType() == 1 ? "PRIMARY" : "SECONDARY");
    sb.append(" :: ");
    sb.append("Secondary number: " + this.getSecondaryNumber());
    sb.append(" :: ");
    sb.append("Source host: " + this.getSourceHost());
    sb.append(" :: ");
    sb.append("Source port: " + this.getSourcePort());
    return sb.toString();
  }

}
