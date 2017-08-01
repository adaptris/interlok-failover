package com.adaptris.failover;

public interface Broadcaster {

   public void start() throws Exception;
   
   public void stop();
   
   public void setPingData(Ping data);

   public void setSendDelaySeconds(int parseInt);
  
}
