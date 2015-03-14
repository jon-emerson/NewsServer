package com.janknspank.common;

import java.net.UnknownHostException;

/**
 * Helper class for figuring out the name of the host the application is
 * currently running on.  If it's being run in Heroku, the "DYNO" system
 * environment variable is returned.  Else, we use the local host that
 * Java tells us about.
 */
public class Host {
  public static String get() {
    String host = System.getenv("DYNO");
    if (host != null) {
      return host;
    }
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new Error(e);
    }
  }
}
