package com.janknspank;

import java.sql.*;

public class MysqlHelper {
  private static final MysqlHelper singleton = new MysqlHelper();

  // JDBC driver name and database URL
  private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  private static final String DB_URL = "jdbc:mysql://localhost/crawler";

  // Database credentials
  private static final String USER = "root";
  private static final String PASS = "helloworld";

  private final Connection conn;

  private MysqlHelper() {
    try {
      // Register JDBC driver.
      Class.forName(JDBC_DRIVER);

      // Open a connection.
      System.out.println("Connecting to database...");
      conn = DriverManager.getConnection(DB_URL, USER, PASS);
      System.out.println("Connected database successfully.");
    } catch (ClassNotFoundException | SQLException e) {
      // TODO(jonemerson): Handle errors.
      throw new RuntimeException(e);
    }
  }

  public static MysqlHelper getInstance() {
    return singleton;
  }

  public Statement getStatement() throws SQLException {
    return conn.createStatement();
  }

  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return conn.prepareStatement(sql);
  }
}