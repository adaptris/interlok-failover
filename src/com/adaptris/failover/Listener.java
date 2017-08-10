package com.adaptris.failover;

import java.io.IOException;

public interface Listener extends PingEventSender {

  public void start() throws IOException;
  
  public void stop();
  
}
