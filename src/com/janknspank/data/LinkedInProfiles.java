package com.janknspank.data;

import com.janknspank.proto.Core.LinkedInProfile;
import com.janknspank.proto.Core.User;

public class LinkedInProfiles {
  
  public static LinkedInProfile getByUserId(String userId) throws DataInternalException {
    return Database.getInstance().getFirst(LinkedInProfile.class,
        new QueryOption.WhereEquals("user_id", userId));
  }
  
  /** Helper method for creating the LinkedInProfile table. */
  public static void main(String args[]) throws Exception {
    Database.getInstance().createTable(LinkedInProfile.class);
  }
}
