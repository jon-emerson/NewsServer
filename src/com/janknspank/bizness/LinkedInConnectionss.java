package com.janknspank.bizness;

import com.janknspank.database.Database;
import com.janknspank.proto.UserProto.LinkedInConnections;

public class LinkedInConnectionss {
  /** Helper method for creating the LinkedInConnections table. */
  public static void main(String args[]) throws Exception {
    Database.with(LinkedInConnections.class).createTable();
  }
}
