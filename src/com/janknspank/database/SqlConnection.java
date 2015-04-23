package com.janknspank.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class SqlConnection {
  // JDBC driver name and database URL
  private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  private static final String DB_URL =
      "jdbc:mysql://newsserver.ceibyxjobuqr.us-west-2.rds.amazonaws.com:4406/newsserver?"
          + Joiner.on("&").join(ImmutableList.of(
              "rewriteBatchedStatements=true",
              "useUnicode=true",
              "characterEncoding=UTF-8",
              "characterSetResults=utf8",
              "connectionCollation=utf8_bin"));
  private static final BasicDataSource dataSource = new BasicDataSource();
  static {
    String mysqlUser = System.getenv("MYSQL_USER");
    if (mysqlUser == null) {
      throw new Error("$MYSQL_USER is undefined");
    }
    String mysqlPassword = System.getenv("MYSQL_PASSWORD");
    if (mysqlPassword == null) {
      throw new Error("$MYSQL_PASSWORD is undefined");
    }

    // Make sure the MySQL JDBC driver is loaded.
    try {
      Class.forName(JDBC_DRIVER);
    } catch (ClassNotFoundException e) {
      throw new Error(e);
    }

    dataSource.setDriverClassName(JDBC_DRIVER);
    dataSource.setUrl(DB_URL);
    dataSource.setUsername(mysqlUser);
    dataSource.setPassword(mysqlPassword);
  }

  static synchronized Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  // Please don't call this.  It won't work when we switch to MongoDB.
  public static PreparedStatement xXprepareStatement(String sql) throws SQLException {
    return getConnection().prepareStatement(sql);
  }
}
