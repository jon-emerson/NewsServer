package com.janknspank.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;

class LocalSqlCollection<T extends Message> extends SqlCollection<T> {
  private static final Logger LOG = Logger.getLogger(LocalSqlCollection.class.getName());
  private static final String DB_URL =
      "jdbc:mysql://localhost/test?"
          + Joiner.on("&").join(ImmutableList.of(
              "useTimezone=true",
              "rewriteBatchedStatements=true",
              "useUnicode=true",
              "characterEncoding=UTF-8",
              "characterSetResults=utf8",
              "connectionCollation=utf8_bin"));
  private static Connection connection;

  public LocalSqlCollection(Class<T> clazz) {
    super(clazz);
  }

  @Override
  protected Connection getConnection() throws DatabaseSchemaException {
    if (connection == null) {
      LOG.info("Connecting to local database...");
      try {
        connection = DriverManager.getConnection(DB_URL, "hello", "");
      } catch (SQLException e) {
        throw new DatabaseSchemaException("Could not connect to local database", e);
      }
      LOG.info("Connected to local database successfully.");
    }
    return connection;
  }
}
