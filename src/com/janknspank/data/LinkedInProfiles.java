package com.janknspank.data;

import com.janknspank.proto.Core.LinkedInProfile;

public class LinkedInProfiles {
  /** Helper method for creating the LinkedInProfile table. */
  public static void main(String args[]) throws Exception {
    Database.with(LinkedInProfile.class).createTable();
  }
}
