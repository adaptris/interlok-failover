package com.adaptris.failover;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.adaptris.failover.util.Constants;

public class OnlineInstance {
  
  private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
  
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
  
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("ID: " + this.getId().toString());
    stringBuilder.append("\n");
    if(this.getInstanceType() == Constants.MASTER)
      stringBuilder.append("Type: Master");
    else
      stringBuilder.append("TYPE: Slave");
    stringBuilder.append("\n");
    stringBuilder.append("Last Contact: " + sdf.format(new Date(this.getLastContact())));
    stringBuilder.append("\n");
    
    return stringBuilder.toString();
  }

}
