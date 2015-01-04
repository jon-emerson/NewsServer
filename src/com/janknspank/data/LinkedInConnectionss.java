package com.janknspank.data;

import java.sql.Connection;

import com.janknspank.proto.Core.LinkedInConnections;

public class LinkedInConnectionss {
  /** Helper method for creating the LinkedInConnections table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(
        LinkedInConnections.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(LinkedInConnections.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
