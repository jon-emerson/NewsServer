package com.janknspank.data;

import java.sql.Connection;

import com.janknspank.proto.Core.LinkedInData;

public class LinkedInDatas {
  /** Helper method for creating the LinkedInData table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(LinkedInData.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(LinkedInData.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
