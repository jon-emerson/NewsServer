package com.janknspank.utils;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.CoreProto.UrlRating;

public class FixUserRatingsCollection {
  public static void main(String args[]) throws DatabaseRequestException, DatabaseSchemaException {
    Database.update(Database.with(UrlRating.class).get());
  }
}
