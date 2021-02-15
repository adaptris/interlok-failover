package com.adaptris.failover;

public interface MultiPrimaryConflictHandler {

  void handle(OnlineInstance onlineInstance, Ping ping);
  
}
