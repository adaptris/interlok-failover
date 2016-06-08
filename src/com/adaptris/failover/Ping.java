package com.adaptris.failover;

import java.util.UUID;

public class Ping {
    
  private UUID instanceId;
  
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

}
