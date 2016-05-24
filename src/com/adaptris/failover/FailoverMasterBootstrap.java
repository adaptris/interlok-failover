package com.adaptris.failover;

import java.util.Properties;

public class FailoverMasterBootstrap extends FailoverBootstrap {

  @Override
  protected void startFailover(Properties bootstrapProperties) {
    // TODO Auto-generated method stub
    
  }
  
  public static final void main(String[] arguments) {
    if(arguments.length != 1) {
      doUsage();
    } else
      new FailoverMasterBootstrap().doBootstrap(arguments[1]);
  }
}
