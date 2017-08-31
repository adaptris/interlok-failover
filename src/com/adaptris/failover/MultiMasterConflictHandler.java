package com.adaptris.failover;

public interface MultiMasterConflictHandler {

  void handle(OnlineInstance onlineInstance, Ping ping);
  
}
