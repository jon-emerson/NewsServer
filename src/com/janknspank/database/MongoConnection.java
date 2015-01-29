package com.janknspank.database;

import java.net.UnknownHostException;
import java.util.Arrays;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class MongoConnection {
  private static final String MONGO_DATABASE = "newsserver";
  private static final String MONGO_USER;
  private static final String MONGO_PASSWORD;
  static {
    MONGO_USER = System.getenv("MONGO_USER");
    if (MONGO_USER == null) {
      throw new Error("$MONGO_USER is undefined");
    }
    MONGO_PASSWORD = System.getenv("MONGO_PASSWORD");
    if (MONGO_PASSWORD == null) {
      throw new Error("$MONGO_PASSWORD is undefined");
    }
  }

  private static MongoClient CLIENT = null;

  public static synchronized MongoClient getClient() throws DatabaseSchemaException {
    if (CLIENT == null) {
      try {
        MongoCredential credential = MongoCredential.createMongoCRCredential(
            MONGO_USER, MONGO_DATABASE, MONGO_PASSWORD.toCharArray());
        CLIENT = new MongoClient(new ServerAddress("ds039431.mongolab.com", 39431),
            Arrays.asList(credential));
      } catch (UnknownHostException e) {
        throw new DatabaseSchemaException("Could not connect to MongoDB: " + e.getMessage(), e);
      }
    }
    return CLIENT;
  }

  public static DB getDatabase() throws DatabaseSchemaException {
    return getClient().getDB(MONGO_DATABASE);
  }
}
