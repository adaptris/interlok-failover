package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.MASTER;
import static com.adaptris.failover.util.Constants.SLAVE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailoverManager implements PingEventListener, StateChangeEventSender, Triggerable {
  
  private static final int DEFAULT_INSTANCE_TIMEOUT_SECONDS = 20;
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  private UUID uniqueId;
  private Listener listener;
  private Broadcaster broadcaster;
  private List<StateChangeEventListener> listeners;
  private MonitorThread pollingThread;
  
  private OnlineInstance myInstance;
  private OnlineInstance currentMaster;
  private List<OnlineInstance> instances;
  
  private Ping myOutgoingPing;
  
  private int instanceTimeoutSeconds;
  
  public FailoverManager(Listener listener, Broadcaster broadcaster, boolean master, int slavePosition) {
    this.listener = listener;
    this.listener.registerListener(this);
    
    this.broadcaster = broadcaster;
    
    this.setUniqueId(UUID.randomUUID());
    
    instanceTimeoutSeconds = DEFAULT_INSTANCE_TIMEOUT_SECONDS;
    
    pollingThread = new MonitorThread(this);
    
    listeners = new ArrayList<StateChangeEventListener>();
    instances = new ArrayList<>();
    
    myOutgoingPing = new Ping();
    myOutgoingPing.setInstanceId(getUniqueId());
    if(master) {
      currentMaster = new OnlineInstance(this.getUniqueId());
      currentMaster.setInstanceType(MASTER);
      myInstance = currentMaster;
      myOutgoingPing.setInstanceType(MASTER);
      myOutgoingPing.setSlaveNumber(0);
    } else {
      myInstance = new OnlineInstance(getUniqueId());
      myInstance.setInstanceType(SLAVE);
      myOutgoingPing.setInstanceType(SLAVE);
      if(slavePosition > 0) {
        myInstance.setSlaveNumber(slavePosition);
        myOutgoingPing.setSlaveNumber(slavePosition);
      }
    }
  }
  
  @Override
  public void pollTriggered() {
    if(myInstance.getInstanceType() != MASTER) { // if we are master, we don't need to do anything
      if(!this.assignSlaveNumber()) { // if we don't have to assign slave numbers continue, otherwise assign and wait for next poll.
        checkPromotion();
      }
      StringBuilder sb = new StringBuilder();
      
      sb.append("My slave instance:");
      sb.append(myInstance.toString());
      sb.append("\nOther online instances:\n");
      
      for(OnlineInstance instance : instances)
        sb.append(instance.toString());
      
      log.trace(sb.toString());
    }
  }
  
  private void checkPromotion() {
    if(myInstance.getSlaveNumber() == 1) {
      if(masterNotAvailable()) {
        log.trace("Master not available, promoting myself to master.");
        myInstance.setInstanceType(MASTER);
        myOutgoingPing.setInstanceType(MASTER);
        myOutgoingPing.setSlaveNumber(0);
        currentMaster = myInstance;
        
        listener.stop();
        pollingThread.stop();
        
        this.notifyPromoteToMaster();
      }
    } else { // do we need to promote this slave?
      if(slaveNotAvailable(myInstance.getSlaveNumber() - 1)) {
        log.trace("Slave (" + (myInstance.getSlaveNumber() - 1) + ") not available, promoting myself.");
        myInstance.setSlaveNumber(myInstance.getSlaveNumber() - 1);
        myOutgoingPing.setSlaveNumber(myInstance.getSlaveNumber());
        this.broadcaster.setPingData(myOutgoingPing);
        this.notifyPromoteSlave();
      }
    }
  }

  private boolean slaveNotAvailable(int slaveNumber) {
    OnlineInstance slaveInstance = null;
    for(OnlineInstance instance : instances) {
      if(instance.getSlaveNumber() == slaveNumber) { 
        slaveInstance = instance;
        break;
      }
    }
    if(slaveInstance != null) {
      return (slaveInstance.getLastContact() < (System.currentTimeMillis() - (this.getInstanceTimeoutSeconds() * 1000))); 
    }
    return false;
  }

  private boolean masterNotAvailable() {
    if(currentMaster == null)
      return true;
    else
      return (currentMaster.getLastContact() < (System.currentTimeMillis() - (this.getInstanceTimeoutSeconds() * 1000)));
  }

  private boolean assignSlaveNumber() {
    boolean needToAssignNumbers = false;
    
    if(myInstance.getSlaveNumber() == 0)
      needToAssignNumbers = true;
    
    if(!needToAssignNumbers) {
      for(OnlineInstance instance : instances) {
        if(instance.getSlaveNumber() == 0)
          needToAssignNumbers = true;
      }
    }
    
    if(needToAssignNumbers) {
      // we're going to use the UUID, order those to decide the order of the slaves.
      String[] instanceIds = new String[instances.size() + 1];
      for(int counter = 0; counter < instances.size(); counter ++)
        instanceIds[counter] = instances.get(counter).getId().toString();
      
      instanceIds[instanceIds.length - 1] = myInstance.getId().toString();
      
      Arrays.sort(instanceIds);
      for(int counter = 0; counter < instanceIds.length; counter ++) {
        if(instanceIds[counter].equals(myInstance.getId().toString())) {
          log.info("Assigning myself slave position " + counter + 1);
          myInstance.setSlaveNumber(counter + 1);
          myOutgoingPing.setSlaveNumber(counter + 1);
          this.broadcaster.setPingData(myOutgoingPing);
          break;
        }
      }
      return true;
    }
    return false;
  }

  public void start() throws Exception {
    this.listener.start();
    Thread.sleep(5000);
    this.broadcaster.setPingData(myOutgoingPing);
    this.broadcaster.start();
    this.pollingThread.start();
  }
  
  public void stop() {
    this.broadcaster.stop();
    this.listener.stop();
    this.pollingThread.stop();
  }

  @Override
  public void masterPinged(Ping ping) {
    if(currentMaster == null)
      currentMaster = new OnlineInstance(ping.getInstanceId());
    
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
    if(ping.getInstanceId().equals(myInstance.getId())) {
      myInstance.setLastContact(System.currentTimeMillis());
    } else {
      OnlineInstance pingSource = this.getInstanceFromPing(ping);
      if(pingSource ==  null) {
        pingSource = new OnlineInstance(ping.getInstanceId());
        pingSource.setInstanceType(SLAVE);
        instances.add(pingSource);
      }
      pingSource.setLastContact(System.currentTimeMillis());
      pingSource.setSlaveNumber(ping.getSlaveNumber());
    }
  }
  
  private OnlineInstance getInstanceFromPing(Ping ping) {
    OnlineInstance returnedInstance = null;
    for(OnlineInstance onlineInstance : instances) {
      if(onlineInstance.getId().equals(ping.getInstanceId())) {
        returnedInstance = onlineInstance;
        break;
      }
    }
      
    return returnedInstance;
  }

  private void handleMasterConflict() {
    log.info("Another instance is already master, shutting down");
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
      changeEventListener.promoteSlave(myInstance.getSlaveNumber());
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

  public int getInstanceTimeoutSeconds() {
    return instanceTimeoutSeconds;
  }

  public void setInstanceTimeoutSeconds(int instanceTimeoutSeconds) {
    this.instanceTimeoutSeconds = instanceTimeoutSeconds;
  }

}
