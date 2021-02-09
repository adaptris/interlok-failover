package com.adaptris.failover;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.adaptris.failover.util.Constants;

public class OnlineInstance {
  
  private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
  
  private UUID id;
  
  private int instanceType;
  
  private long lastContact;
  
  private int secondaryNumber;
  
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

  public int getSecondaryNumber() {
    return secondaryNumber;
  }

  public void setSecondaryNumber(int secondaryNumber) {
    this.secondaryNumber = secondaryNumber;
  }
  
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("ID", getId())
        .append("Type", getInstanceType() == Constants.MASTER ? "master" : "secondary").append("Position", getSecondaryNumber())
        .append("last", new Date(getLastContact()))
        .toString();
  }

}
