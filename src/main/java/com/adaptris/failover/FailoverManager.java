package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.PRIMARY;
import static com.adaptris.failover.util.Constants.SECONDARY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
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
  private OnlineInstance currentPrimary;
  private volatile List<OnlineInstance> instances;
  
  private Ping myOutgoingPing;
  
  private int instanceTimeoutSeconds;
  
  private MultiPrimaryConflictHandler multiPrimaryConflictHandler;
  
  public FailoverManager(String myHost, String myPort, Listener listener, Broadcaster broadcaster, boolean primary, int secondaryPosition) {
    this.listener = listener;
    this.listener.registerListener(this);
    
    this.broadcaster = broadcaster;
    
    setUniqueId(UUID.randomUUID());
    
    instanceTimeoutSeconds = DEFAULT_INSTANCE_TIMEOUT_SECONDS;
    
    setPollingThread(new MonitorThread(this));
    setMultiPrimaryConflictHandler(new ExitMultiPrimaryConflictHandler());
    
    listeners = new ArrayList<>();
    setInstances(new ArrayList<OnlineInstance>());
    
    myOutgoingPing = new Ping();
    myOutgoingPing.setInstanceId(getUniqueId());
    myOutgoingPing.setSourceHost(myHost);
    myOutgoingPing.setSourcePort(myPort);
    
    if(primary) {
      setCurrentPrimary(new OnlineInstance(getUniqueId()));
      getCurrentPrimary().setInstanceType(PRIMARY);
      setMyInstance(getCurrentPrimary());
      myOutgoingPing.setInstanceType(PRIMARY);
      myOutgoingPing.setSecondaryNumber(0);
    } else {
      setMyInstance(new OnlineInstance(getUniqueId()));
      getMyInstance().setInstanceType(SECONDARY);
      myOutgoingPing.setInstanceType(SECONDARY);
      if(secondaryPosition > 0) {
        getMyInstance().setSecondaryNumber(secondaryPosition);
        myOutgoingPing.setSecondaryNumber(secondaryPosition);
      }
    }
  }
  
  @Override
  public void pollTriggered() {
    if(getMyInstance().getInstanceType() != PRIMARY) { // if we are primary, we don't need to do anything
      if(!assignSecondaryNumber()) { // if we don't have to assign secondary numbers continue, otherwise assign and wait for next poll.
        checkPromotion();
      }
    }
    purgeOldSecondaryInstances();
    
    logState();
  }

  private void purgeOldSecondaryInstances() {
    for(int counter = getInstances().size() - 1; counter >= 0; counter --) {
      if(timedOut(getInstances().get(counter).getLastContact())) {
        log.info("Removing timed out secondary: {}", getInstances().get(counter).getId().toString());
        getInstances().remove(counter);
      }
    }
  }

  private void checkPromotion() {
    if(getMyInstance().getSecondaryNumber() == 1) {
      if(primaryNotAvailable()) {
        log.trace("Primary not available, promoting myself to primary.");
        getMyInstance().setInstanceType(PRIMARY);
        myOutgoingPing.setInstanceType(PRIMARY);
        myOutgoingPing.setSecondaryNumber(0);
        broadcaster.setPingData(myOutgoingPing);
        getMyInstance().setSecondaryNumber(0);
        setCurrentPrimary(getMyInstance());
        
        notifyPromoteToPrimary();
      }
    } else { // do we need to promote this secondary?
      if(secondaryNotAvailable(getMyInstance().getSecondaryNumber() - 1)) {
        log.trace("Secondary ({}) not available, promoting self", getMyInstance().getSecondaryNumber() - 1);
        getMyInstance().setSecondaryNumber(getMyInstance().getSecondaryNumber() - 1);
        myOutgoingPing.setSecondaryNumber(getMyInstance().getSecondaryNumber());
        broadcaster.setPingData(myOutgoingPing);
        notifyPromoteSecondary();
      }
    }
  }

  private boolean secondaryNotAvailable(int secondaryNumber) {
    OnlineInstance secondaryInstance = null;
    for(OnlineInstance instance : instances) {
      if(instance.getSecondaryNumber() == secondaryNumber) { 
        secondaryInstance = instance;
        break;
      }
    }
    if(secondaryInstance != null) {
      return timedOut(secondaryInstance.getLastContact()); 
    }
    else {
      return true; // we can't find this secondary, it may have been purged.
    }
  }

  private boolean primaryNotAvailable() {
    if(getCurrentPrimary() == null) {
      return true;
    } else {
      return timedOut(getCurrentPrimary().getLastContact());
    }
  }

  private boolean timedOut(long lastContact) {
    return lastContact < System.currentTimeMillis() - getInstanceTimeoutSeconds() * 1000L;
  }
  
  private boolean assignSecondaryNumber() {
    boolean needToAssignNumbers = false;
    
    if(getMyInstance().getSecondaryNumber() == 0) {
      needToAssignNumbers = true;
    }
    
    if(!needToAssignNumbers) {
      for(OnlineInstance instance : getInstances()) {
        // if any instances do not have a secondary number, or if it is the same as ours, lets re-assign.
        if(instance.getInstanceType() != PRIMARY) {
          if(instance.getSecondaryNumber() == 0 || instance.getSecondaryNumber() == getMyInstance().getSecondaryNumber()) {
            needToAssignNumbers = true;
            break;
          }
        }
      }
    }
    
    if(needToAssignNumbers) {
      // we're going to use the UUID, order those to decide the order of the secondaries.
      String[] instanceIds = new String[getInstances().size() + 1];
      for(int counter = 0; counter < getInstances().size(); counter ++) {
        instanceIds[counter] = getInstances().get(counter).getId().toString();
      }
      
      instanceIds[instanceIds.length - 1] = getMyInstance().getId().toString();
      
      Arrays.sort(instanceIds);
      for(int counter = 0; counter < instanceIds.length; counter ++) {
        if(instanceIds[counter].equals(getMyInstance().getId().toString())) {
          log.debug("Assigning myself secondary position {}", counter + 1);
          getMyInstance().setSecondaryNumber(counter + 1);
          myOutgoingPing.setSecondaryNumber(counter + 1);
          broadcaster.setPingData(myOutgoingPing);
          break;
        }
      }
      return true;
    }
    return false;
  }

  public void start() throws Exception {
    listener.start();
    Thread.sleep(5000);
    getPollingThread().start();
    broadcaster.setPingData(myOutgoingPing);
    broadcaster.start();
  }
  
  public void stopFailoverManager() {
    log.info("Interlok instance stopped, destroying instance.");
    broadcaster.stop();
    listener.stop();
    getPollingThread().stop();
  }
  
  public void stop() {
    stopFailoverManager();
    notifyAdapterStopped();
  }

  @Override
  public void primaryPinged(Ping ping) {
    if(getCurrentPrimary() == null) {
      setCurrentPrimary(new OnlineInstance(ping.getInstanceId()));
    }
    
    // see if we need to remove the primary from the list of secondaries.
    synchronized(getInstances()) {
      for(int counter = getInstances().size() - 1; counter >= 0; counter --) {
        if(getInstances().get(counter).getId().equals(ping.getInstanceId())) {
          if (Constants.DEBUG && log.isTraceEnabled()) {
            log.debug("Removing new primary ({}) from list of secondaries.", ping.getInstanceId().toString());
          } 
          
          getInstances().remove(counter);
        }
      }
    }
    
    if(getCurrentPrimary().getId().equals(uniqueId)) { // we are primary!
      if(!ping.getInstanceId().equals(getUniqueId())) {
        handlePrimaryConflict(getMyInstance(), ping);
      }
    } else {
      getCurrentPrimary().setId(ping.getInstanceId());
      getCurrentPrimary().setInstanceType(PRIMARY);
      getCurrentPrimary().setLastContact(System.currentTimeMillis());
      getCurrentPrimary().setSecondaryNumber(ping.getSecondaryNumber());
    }
  }

  @Override
  public void secondaryPinged(Ping ping) {
    if(ping.getInstanceId().equals(getMyInstance().getId())) {
      getMyInstance().setLastContact(System.currentTimeMillis());
    } else {
      OnlineInstance pingSource = getInstanceFromPing(ping);
      if(pingSource ==  null) {
        pingSource = new OnlineInstance(ping.getInstanceId());
        pingSource.setInstanceType(SECONDARY);
        getInstances().add(pingSource);
      }
      pingSource.setLastContact(System.currentTimeMillis());
      pingSource.setSecondaryNumber(ping.getSecondaryNumber());
      
      // check to see if our broadcaster needs to know about this peer.
      Peer peer = new Peer(ping.getSourceHost(), Integer.parseInt(ping.getSourcePort()));
      if(!broadcaster.getPeers().contains(peer)) {
        broadcaster.getPeers().add(peer);
      }
    }
  }
  
  private OnlineInstance getInstanceFromPing(Ping ping) {
    OnlineInstance returnedInstance = null;
    for(OnlineInstance onlineInstance : getInstances()) {
      if(onlineInstance.getId().equals(ping.getInstanceId())) {
        returnedInstance = onlineInstance;
        break;
      }
    }
      
    return returnedInstance;
  }

  private void handlePrimaryConflict(OnlineInstance onlineInstance, Ping ping) {
    log.warn("Another instance is already primary, shutting down");
    getMultiPrimaryConflictHandler().handle(onlineInstance, ping);
  }
  
  private void logState() {
    if (Constants.DEBUG && log.isTraceEnabled()) {
      ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("Self", getMyInstance());
      if (getMyInstance().getInstanceType() == SECONDARY) {
        builder.append("primary", getCurrentPrimary());
      }
      builder.append("secondaries", getInstances());
      log.trace(builder.toString());
    }
  }
  
  @Override
  public boolean equals(PingEventListener eventListener) {
    if(eventListener instanceof FailoverManager) {
      FailoverManager otherInstance = (FailoverManager) eventListener;
      return getUniqueId().equals(otherInstance.getUniqueId());
    } else {
      return false;
    }
  }

  @Override
  public void notifyPromoteToPrimary() {
    for(StateChangeEventListener changeEventListener : listeners) {
      changeEventListener.promoteToPrimary();
    }
  }
  
  @Override
  public void notifyPromoteSecondary() {
    for(StateChangeEventListener changeEventListener : listeners) {
      changeEventListener.promoteSecondary(getMyInstance().getSecondaryNumber());
    }
  }

  @Override
  public void notifyAdapterStopped() {
    for(StateChangeEventListener changeEventListener : listeners) {
      changeEventListener.adapterStopped();
    }
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

  public OnlineInstance getCurrentPrimary() {
    return currentPrimary;
  }

  public void setCurrentPrimary(OnlineInstance currentPrimary) {
    this.currentPrimary = currentPrimary;
  }

  public List<OnlineInstance> getInstances() {
    return instances;
  }

  public void setInstances(List<OnlineInstance> instances) {
    this.instances = instances;
  }

  public MultiPrimaryConflictHandler getMultiPrimaryConflictHandler() {
    return multiPrimaryConflictHandler;
  }

  public void setMultiPrimaryConflictHandler(MultiPrimaryConflictHandler multiPrimaryConflictHandler) {
    this.multiPrimaryConflictHandler = multiPrimaryConflictHandler;
  }

}
