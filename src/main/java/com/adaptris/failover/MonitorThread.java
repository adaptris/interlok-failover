package com.adaptris.failover;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MonitorThread {
  
  private static final int DEFAULT_POLL_SECONDS = 5;
  
  private ScheduledExecutorService scheduler;
  
  private ScheduledFuture<?> instanceHandle;
  
  private final Triggerable triggerable;
  
  private int pollingSeconds;

  public MonitorThread(Triggerable triggerable) {
    scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
        return new Thread(runnable, "Failover Monitor Thread");
      }
    });
    pollingSeconds = DEFAULT_POLL_SECONDS;
    this.triggerable = triggerable;
  }
  
  public void runMonitor() {
    final Runnable instanceRunnable = new Runnable() {
      @Override
      public void run() {
        try {
          
          triggerable.pollTriggered();
          
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    
    this.instanceHandle = this.scheduler.scheduleWithFixedDelay(instanceRunnable, pollingSeconds, pollingSeconds, TimeUnit.SECONDS);
  }
  
  public void start() {
    runMonitor();
  }
  
  public void stop() {
    if(instanceHandle != null)
      instanceHandle.cancel(true);
    if(scheduler != null)
      scheduler.shutdownNow();
  }

  public int getPollingSeconds() {
    return pollingSeconds;
  }

  public void setPollingSeconds(int pollingSeconds) {
    this.pollingSeconds = pollingSeconds;
  }
}
