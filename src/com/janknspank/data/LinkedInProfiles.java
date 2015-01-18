package com.janknspank.data;

import com.janknspank.proto.Core.LinkedInProfile;

public class LinkedInProfiles {
  /** Helper method for creating the LinkedInProfile table. */
  public static void main(String args[]) throws Exception {
    Database database = Database.getInstance();
    database.prepareStatement(database.getCreateTableStatement(LinkedInProfile.class)).execute();
    for (String statement : database.getCreateIndexesStatement(LinkedInProfile.class)) {
      database.prepareStatement(statement).execute();
    }
  }
}
