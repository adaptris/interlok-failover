package com.adaptris.failover;

import java.util.UUID;

public class OnlineInstance {
  
  private UUID id;
  
  private int instanceType;
  
  private long lastContact;
  
  private int slaveNumber;
  
  public OnlineInstance(UUID id) {
    this.setId(id);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public int getInstanceType() {
    return instanceType;
  }

  public void setInstanceType(int instanceType) {
    this.instanceType = instanceType;
  }

  public long getLastContact() {
    return lastContact;
  }

  public void setLastContact(long lastContact) {
    this.lastContact = lastContact;
  }
  
  public boolean equals(Object object) {
    if(object instanceof OnlineInstance) {
      OnlineInstance other = (OnlineInstance) object;
      return other.getId().equals(this.getId());
    }
    return false;
  }

  public int getSlaveNumber() {
    return slaveNumber;
  }

  public void setSlaveNumber(int slaveNumber) {
    this.slaveNumber = slaveNumber;
  }

}
