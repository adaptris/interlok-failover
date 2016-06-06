package com.adaptris.failover;

import java.util.UUID;

public class Ping {
  
  private byte[] data;
  
  private UUID instanceId;
  
  public Ping() {
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public UUID getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(UUID instanceId) {
    this.instanceId = instanceId;
  }

}
