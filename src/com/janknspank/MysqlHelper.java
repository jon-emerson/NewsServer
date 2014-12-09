package com.janknspank;

import java.sql.*;

public class MysqlHelper {
  private static MysqlHelper singleton = null;

  // JDBC driver name and database URL
  private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  private static final String DB_URL =
      "jdbc:mysql://newsserver.ceibyxjobuqr.us-west-2.rds.amazonaws.com/newsserver";

  private final Connection conn;

  private MysqlHelper(String user, String password) {
    try {
      // Register JDBC driver.
      Class.forName(JDBC_DRIVER);

      // Open a connection.
      System.out.println("Connecting to database...");
      conn = DriverManager.getConnection(DB_URL, user, password);
      System.out.println("Connected database successfully.");
    } catch (ClassNotFoundException | SQLException e) {
      // TODO(jonemerson): Handle errors.
      throw new RuntimeException(e);
    }
  }

  public static MysqlHelper getInstance() {
    if (singleton == null) {
      String mysqlUser = System.getenv("MYSQL_USER");
      if (mysqlUser == null) {
        throw new RuntimeException("$MYSQL_USER is undefined");
      }
      String mysqlPassword = System.getenv("MYSQL_PASSWORD");
      if (mysqlPassword == null) {
        throw new RuntimeException("$MYSQL_PASSWORD is undefined");
      }
      singleton = new MysqlHelper(mysqlUser, mysqlPassword);
    }
    return singleton;
  }

  public Statement getStatement() throws SQLException {
    return conn.createStatement();
  }

  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return conn.prepareStatement(sql);
  }
}