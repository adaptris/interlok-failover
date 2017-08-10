package com.adaptris.failover;

public class ExitMultiMasterConflictHandler implements MultiMasterConflictHandler {

  @Override
  public void handle(OnlineInstance onlineInstance, Ping ping) {
    System.exit(1);
  }

}
