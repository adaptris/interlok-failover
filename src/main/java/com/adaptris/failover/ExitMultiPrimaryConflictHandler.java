package com.adaptris.failover;

public class ExitMultiPrimaryConflictHandler implements MultiPrimaryConflictHandler {

  @Override
  public void handle(OnlineInstance onlineInstance, Ping ping) {
    System.exit(1);
  }

}
