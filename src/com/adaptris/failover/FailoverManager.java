package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.MASTER;
import static com.adaptris.failover.util.Constants.SLAVE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.failover.util.Constants;

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
  
  private MultiMasterConflictHandler multiMasterConflictHandler;
  
  public FailoverManager(String myHost, String myPort, Listener listener, Broadcaster broadcaster, boolean master, int slavePosition) {
    this.listener = listener;
    this.listener.registerListener(this);
    
    this.broadcaster = broadcaster;
    
    this.setUniqueId(UUID.randomUUID());
    
    instanceTimeoutSeconds = DEFAULT_INSTANCE_TIMEOUT_SECONDS;
    
    this.setPollingThread(new MonitorThread(this));
    this.setMultiMasterConflictHandler(new ExitMultiMasterConflictHandler());
    
    listeners = new ArrayList<StateChangeEventListener>();
    this.setInstances(new ArrayList<OnlineInstance>());
    
    myOutgoingPing = new Ping();
    myOutgoingPing.setInstanceId(getUniqueId());
    myOutgoingPing.setSourceHost(myHost);
    myOutgoingPing.setSourcePort(myPort);
    
    if(master) {
      this.setCurrentMaster(new OnlineInstance(this.getUniqueId()));
      this.getCurrentMaster().setInstanceType(MASTER);
      this.setMyInstance(this.getCurrentMaster());
      myOutgoingPing.setInstanceType(MASTER);
      myOutgoingPing.setSlaveNumber(0);
    } else {
      this.setMyInstance(new OnlineInstance(getUniqueId()));
      this.getMyInstance().setInstanceType(SLAVE);
      myOutgoingPing.setInstanceType(SLAVE);
      if(slavePosition > 0) {
        this.getMyInstance().setSlaveNumber(slavePosition);
        myOutgoingPing.setSlaveNumber(slavePosition);
      }
    }
  }
  
  @Override
  public void pollTriggered() {
    if(this.getMyInstance().getInstanceType() != MASTER) { // if we are master, we don't need to do anything
      if(!this.assignSlaveNumber()) { // if we don't have to assign slave numbers continue, otherwise assign and wait for next poll.
        checkPromotion();
      }
    }
    purgeOldSlaveInstances();
    
    logState();
  }

  private void purgeOldSlaveInstances() {
    for(int counter = this.getInstances().size() - 1; counter >= 0; counter --) {
      if(timedOut(this.getInstances().get(counter).getLastContact())) {
        log.info("Removing timed out slave: {}", this.getInstances().get(counter).getId().toString());
        this.getInstances().remove(counter);
      }
    }
  }

  private void checkPromotion() {
    if(this.getMyInstance().getSlaveNumber() == 1) {
      if(masterNotAvailable()) {
        log.trace("Master not available, promoting myself to master.");
        this.getMyInstance().setInstanceType(MASTER);
        myOutgoingPing.setInstanceType(MASTER);
        myOutgoingPing.setSlaveNumber(0);
        this.broadcaster.setPingData(myOutgoingPing);
        this.getMyInstance().setSlaveNumber(0);
        this.setCurrentMaster(this.getMyInstance());
        
        this.notifyPromoteToMaster();
      }
    } else { // do we need to promote this slave?
      if(slaveNotAvailable(this.getMyInstance().getSlaveNumber() - 1)) {
        log.trace("Slave ({}) not available, promoting self", (this.getMyInstance().getSlaveNumber() - 1));
        this.getMyInstance().setSlaveNumber(this.getMyInstance().getSlaveNumber() - 1);
        myOutgoingPing.setSlaveNumber(this.getMyInstance().getSlaveNumber());
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
      return timedOut(slaveInstance.getLastContact()); 
    } else 
      return true; // we can't find this slave, it may have been purged.
  }

  private boolean masterNotAvailable() {
    if(this.getCurrentMaster() == null)
      return true;
    else
      return timedOut(this.getCurrentMaster().getLastContact());
  }

  private boolean timedOut(long lastContact) {
    return (lastContact < (System.currentTimeMillis() - (this.getInstanceTimeoutSeconds() * 1000)));
  }
  
  private boolean assignSlaveNumber() {
    boolean needToAssignNumbers = false;
    
    if(this.getMyInstance().getSlaveNumber() == 0)
      needToAssignNumbers = true;
    
    if(!needToAssignNumbers) {
      for(OnlineInstance instance : this.getInstances()) {
        // if any instances do not have a slave number, or if it is the same as ours, lets re-assign.
        if((instance.getSlaveNumber() == 0) || instance.getSlaveNumber() == this.getMyInstance().getSlaveNumber()) {
          needToAssignNumbers = true;
          break;
        }
      }
    }
    
    if(needToAssignNumbers) {
      // we're going to use the UUID, order those to decide the order of the slaves.
      String[] instanceIds = new String[this.getInstances().size() + 1];
      for(int counter = 0; counter < this.getInstances().size(); counter ++)
        instanceIds[counter] = this.getInstances().get(counter).getId().toString();
      
      instanceIds[instanceIds.length - 1] = this.getMyInstance().getId().toString();
      
      Arrays.sort(instanceIds);
      for(int counter = 0; counter < instanceIds.length; counter ++) {
        if(instanceIds[counter].equals(this.getMyInstance().getId().toString())) {
          log.debug("Assigning myself slave position {}", (counter + 1));
          this.getMyInstance().setSlaveNumber(counter + 1);
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
    this.getPollingThread().start();
    this.broadcaster.setPingData(myOutgoingPing);
    this.broadcaster.start();
  }
  
  public void stopFailoverManager() {
    log.info("Interlok instance stopped, destroying instance.");
    this.broadcaster.stop();
    this.listener.stop();
    this.getPollingThread().stop();
  }
  
  public void stop() {
    stopFailoverManager();
    notifyAdapterStopped();
  }

  @Override
  public void masterPinged(Ping ping) {
    if(this.getCurrentMaster() == null)
      this.setCurrentMaster(new OnlineInstance(ping.getInstanceId()));
    
    if(this.getCurrentMaster().getId().equals(uniqueId)) { // we are master!
      if(!ping.getInstanceId().equals(getUniqueId())) // someone else thinks they are master
        handleMasterConflict(this.getMyInstance(), ping);
    } else {
      this.getCurrentMaster().setInstanceType(MASTER);
      this.getCurrentMaster().setLastContact(System.currentTimeMillis());
      this.getCurrentMaster().setSlaveNumber(ping.getSlaveNumber());
    }
  }

  @Override
  public void slavePinged(Ping ping) {
    if(ping.getInstanceId().equals(this.getMyInstance().getId())) {
      this.getMyInstance().setLastContact(System.currentTimeMillis());
    } else {
      OnlineInstance pingSource = this.getInstanceFromPing(ping);
      if(pingSource ==  null) {
        pingSource = new OnlineInstance(ping.getInstanceId());
        pingSource.setInstanceType(SLAVE);
        this.getInstances().add(pingSource);

        this.broadcaster.getPeers().add(new Peer(ping.getSourceHost(), Integer.parseInt(ping.getSourcePort())));
      }
      pingSource.setLastContact(System.currentTimeMillis());
      pingSource.setSlaveNumber(ping.getSlaveNumber());
    }
  }
  
  private OnlineInstance getInstanceFromPing(Ping ping) {
    OnlineInstance returnedInstance = null;
    for(OnlineInstance onlineInstance : this.getInstances()) {
      if(onlineInstance.getId().equals(ping.getInstanceId())) {
        returnedInstance = onlineInstance;
        break;
      }
    }
      
    return returnedInstance;
  }

  private void handleMasterConflict(OnlineInstance onlineInstance, Ping ping) {
    log.warn("Another instance is already master, shutting down");
    this.getMultiMasterConflictHandler().handle(onlineInstance, ping);
  }
  
  private void logState() {
    if (Constants.DEBUG && log.isTraceEnabled()) {
      ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("Self", getMyInstance());
      if (getMyInstance().getInstanceType() == SLAVE) {
        builder.append("master", getCurrentMaster());
      }
      builder.append("slaves", getInstances());
      log.trace(builder.toString());
    }
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
      changeEventListener.promoteSlave(this.getMyInstance().getSlaveNumber());
  }

  @Override
  public void notifyAdapterStopped() {
    for(StateChangeEventListener changeEventListener : this.listeners)
      changeEventListener.adapterStopped();
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

  public MonitorThread getPollingThread() {
    return pollingThread;
  }

  public void setPollingThread(MonitorThread pollingThread) {
    this.pollingThread = pollingThread;
  }

  public OnlineInstance getMyInstance() {
    return myInstance;
  }

  public void setMyInstance(OnlineInstance myInstance) {
    this.myInstance = myInstance;
  }

  public OnlineInstance getCurrentMaster() {
    return currentMaster;
  }

  public void setCurrentMaster(OnlineInstance currentMaster) {
    this.currentMaster = currentMaster;
  }

  public List<OnlineInstance> getInstances() {
    return instances;
  }

  public void setInstances(List<OnlineInstance> instances) {
    this.instances = instances;
  }

  public MultiMasterConflictHandler getMultiMasterConflictHandler() {
    return multiMasterConflictHandler;
  }

  public void setMultiMasterConflictHandler(MultiMasterConflictHandler multiMasterConflictHandler) {
    this.multiMasterConflictHandler = multiMasterConflictHandler;
  }

}
