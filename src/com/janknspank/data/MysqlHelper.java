package com.janknspank.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlHelper {
  // JDBC driver name and database URL
  private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  private static final String DB_URL =
      "jdbc:mysql://newsserver.ceibyxjobuqr.us-west-2.rds.amazonaws.com:4406/newsserver";
  static {
    try {
      // Make sure the MySQL JDBC driver is loaded.
      Class.forName(JDBC_DRIVER);
    } catch (ClassNotFoundException e) {
      throw new Error(e);
    }
  }

  // The connection this class wraps.
  private static Connection conn = null;

  public static synchronized Connection getConnection() throws SQLException {
    if (conn == null || conn.isClosed()) {
      System.out.println("Connecting to database...");

      // Create a new connection.
      String mysqlUser = System.getenv("MYSQL_USER");
      if (mysqlUser == null) {
        throw new RuntimeException("$MYSQL_USER is undefined");
      }
      String mysqlPassword = System.getenv("MYSQL_PASSWORD");
      if (mysqlPassword == null) {
        throw new RuntimeException("$MYSQL_PASSWORD is undefined");
      }
      conn = DriverManager.getConnection(DB_URL, mysqlUser, mysqlPassword);

      System.out.println("Connected database successfully.");
    }

    return conn;
  }
}