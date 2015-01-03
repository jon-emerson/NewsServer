package com.janknspank.data;

import java.sql.Connection;

import com.janknspank.proto.Core.LinkedInProfile;

public class LinkedInProfiles {
  /** Helper method for creating the LinkedInData table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(LinkedInProfile.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(LinkedInProfile.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
