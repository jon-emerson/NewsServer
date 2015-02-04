package com.janknspank.bizness;

import java.util.List;

import com.google.api.client.util.Lists;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.Intent;
import com.janknspank.proto.UserProto.User;

public class Intents {
  public static Iterable<Intent> getIntentsFromCodes(Iterable<String> intentCodes) {
    List<Intent> intents = Lists.newArrayList();
    for(String intentCode : intentCodes) {
      // Validate the intent code strings
      if (IntentCodes.INTENT_CODE_MAP.containsKey(intentCode)) {
        intents.add(Intent.newBuilder()
            .setCode(intentCode)
            .setCreateTime(System.currentTimeMillis())
            .build());
      }
    }
    return intents;
  }
  
  public static User setIntents(User user, Iterable<Intent> intents) 
      throws DatabaseSchemaException, BiznessException {
    try {
      return Database.set(user, "intent", intents);
    } catch (DatabaseRequestException e) {
      throw new BiznessException("Could not set intents: " + e.getMessage(), e);
    }
  }
}
