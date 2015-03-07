package com.janknspank.common;

import java.net.UnknownHostException;

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
