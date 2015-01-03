package com.janknspank.data;

import java.sql.Connection;

import com.janknspank.proto.Core.LinkedInConnectionsData;

public class LinkedInConnectionsDatas {
  /** Helper method for creating the LinkedInConnectionsData table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(
        LinkedInConnectionsData.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(LinkedInConnectionsData.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
