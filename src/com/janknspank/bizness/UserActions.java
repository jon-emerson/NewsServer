package com.janknspank.bizness;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.UserAction;

public class UserActions {
  public static Iterable<UserAction> getAllActions() throws DatabaseSchemaException {
    return Database.with(UserAction.class).get();
  }
}
