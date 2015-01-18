package com.janknspank.data;

import com.janknspank.proto.Core.LinkedInConnections;

public class LinkedInConnectionss {
  /** Helper method for creating the LinkedInConnections table. */
  public static void main(String args[]) throws Exception {
    Database database = Database.getInstance();
    database.prepareStatement(database.getCreateTableStatement(
        LinkedInConnections.class)).execute();
    for (String statement : database.getCreateIndexesStatement(LinkedInConnections.class)) {
      database.prepareStatement(statement).execute();
    }
  }
}
