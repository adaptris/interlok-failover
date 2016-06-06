package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.MASTER;
import static com.adaptris.failover.util.Constants.SLAVE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FailoverManager implements PingEventListener, StateChangeEventSender {
  
  private UUID uniqueId;
  private Listener listener;
  private Broadcaster broadcaster;
  private List<StateChangeEventListener> listeners;
  
  private OnlineInstance myInstance;
  private OnlineInstance currentMaster;
  private List<OnlineInstance> instances;
  
  public FailoverManager(Listener listener, Broadcaster broadcaster, boolean master) {
    this.listener = listener;
    this.broadcaster = broadcaster;
    this.setUniqueId(UUID.randomUUID());
    listeners = new ArrayList<StateChangeEventListener>();
    instances = new ArrayList<>();
    if(master) {
      currentMaster = new OnlineInstance(this.getUniqueId());
      currentMaster.setInstanceType(MASTER);
      myInstance = currentMaster;
    } else {
      myInstance = new OnlineInstance(getUniqueId());
      myInstance.setInstanceType(SLAVE);
    }
  }
  
  public void start() throws IOException {
    this.broadcaster.start();
    this.listener.start();
  }

  @Override
  public void masterPinged(Ping ping) {
    if(currentMaster.getId().equals(uniqueId)) { // we are master!
      if(!ping.getInstanceId().equals(getUniqueId())) // someone else thinks they are master
        handleMasterConflict();
    } else {
      if(currentMaster == null) {
        currentMaster = new OnlineInstance(ping.getInstanceId());
        currentMaster.setInstanceType(MASTER);
      }
      currentMaster.setLastContact(System.currentTimeMillis());
      
      if(!instances.contains(currentMaster))
        instances.add(currentMaster);
      else
        instances.get(instances.indexOf(currentMaster)).setLastContact(System.currentTimeMillis());
    }
  }

  @Override
  public void slavePinged(Ping ping) {
    
  }
  
  private void handleMasterConflict() {
    System.exit(1);
  }
  
  @Override
  public boolean equals(PingEventListener eventListener) {
    if(eventListener instanceof FailoverManager) {
      FailoverManager otherInstance = (FailoverManager) eventListener;
      return this.getUniqueId().equals(otherInstance.getUniqueId());
    } else
      return false;
  }

  @Override
  public void notifyPromoteToMaster() {
    for(StateChangeEventListener changeEventListener : this.listeners)
      changeEventListener.promoteToMaster();
  }
  
  public void notifyPromoteSlave() {
    for(StateChangeEventListener changeEventListener : this.listeners)
      changeEventListener.promoteSlave();
  }

  @Override
  public void registerListener(StateChangeEventListener listener) {
    listeners.add(listener);
  }

  public UUID getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(UUID uniqueId) {
    this.uniqueId = uniqueId;
  }

}
