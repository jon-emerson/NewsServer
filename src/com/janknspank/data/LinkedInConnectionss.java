package com.janknspank.data;

import com.janknspank.proto.Core.LinkedInConnections;

public class LinkedInConnectionss {
  /** Helper method for creating the LinkedInConnections table. */
  public static void main(String args[]) throws Exception {
    Database.getInstance().createTable(LinkedInConnections.class);
  }
}
