package com.adaptris.failover;

import static com.adaptris.failover.util.Constants.FAILOVER_DEFAULT_RESOURCE;

public class SimpleBootstrap extends FailoverBootstrap {

  public static void main(String[] args) throws Exception {
    new SimpleBootstrap().doBootstrap(parseArguments(args));
  }

  protected static String parseArguments(String[] args) throws Exception {
    if (args.length > 0) {
      return args[0];
    }
    // No args; assume default.
    return FAILOVER_DEFAULT_RESOURCE;
  }

}
