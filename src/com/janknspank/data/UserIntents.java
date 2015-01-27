package com.janknspank.data;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.janknspank.proto.Core.UserIntent;

public class UserIntents {
  public static final Map<String, UserIntent> USER_INTENT_CODE_MAP = Maps.uniqueIndex(
      ImmutableList.of(
          UserIntent.newBuilder()
              .setIntentCode("indst")
              .setDescription("Stay on top of your industry")
              .build(),
          UserIntent.newBuilder()
              .setIntentCode("sklls")
              .setDescription("Improve your skills")
              .build(),
          UserIntent.newBuilder()
              .setIntentCode("swtch")
              .setDescription("Switch careers")
              .build(),
          UserIntent.newBuilder()
              .setIntentCode("start")
              .setDescription("Start a company")
              .build()),
      new Function<UserIntent, String>() {
        @Override
        public String apply(UserIntent userIntent) {
          return userIntent.getIntentCode();
        }
      });
  
  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(UserIntent.class).createTable();
  }
}
