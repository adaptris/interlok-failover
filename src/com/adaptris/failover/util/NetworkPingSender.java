package com.adaptris.failover.util;

import java.io.IOException;

public interface NetworkPingSender {

  public void initialize(String host, int port) throws IOException;
  
  public void sendData(byte[] data) throws Exception;
  
}
