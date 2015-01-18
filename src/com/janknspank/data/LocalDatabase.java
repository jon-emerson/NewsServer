package com.janknspank.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class LocalDatabase extends Database {
  private static LocalDatabase instance = null;
  private static final String DB_URL =
      "jdbc:mysql://localhost/test?"
          + Joiner.on("&").join(ImmutableList.of(
              "useTimezone=true",
              "rewriteBatchedStatements=true",
              "useUnicode=true",
              "characterEncoding=UTF-8",
              "characterSetResults=utf8",
              "connectionCollation=utf8_bin"));

  protected LocalDatabase(Connection connection) throws DataInternalException {
    super(connection);
  }

  public static LocalDatabase getInstance() throws DataInternalException {
    if (instance == null) {
      System.out.println("Connecting to local database...");
      try {
        instance = new LocalDatabase(DriverManager.getConnection(DB_URL, "hello", ""));
      } catch (SQLException e) {
        throw new DataInternalException("Could not connect to local database", e);
      }
      System.out.println("Connected to local database successfully.");
    }
    return instance;
  }
}
