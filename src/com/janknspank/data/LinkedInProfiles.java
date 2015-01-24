package com.janknspank.data;

import com.janknspank.proto.Core.LinkedInProfile;

public class LinkedInProfiles {
  
  public static LinkedInProfile getByUserId(String userId) throws DataInternalException {
    return Database.with(LinkedInProfile.class).get(userId);
  }
  
  /** Helper method for creating the LinkedInProfile table. */
  public static void main(String args[]) throws Exception {
    Database.with(LinkedInProfile.class).createTable();
  }
}
