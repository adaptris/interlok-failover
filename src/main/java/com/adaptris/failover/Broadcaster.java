package com.adaptris.failover;

import java.util.List;

public interface Broadcaster {

   public void start() throws Exception;
   
   public void stop();
   
   public void setPingData(Ping data);

   public void setSendDelaySeconds(int parseInt);
   
   public List<Peer> getPeers();
  
}
