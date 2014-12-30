package com.janknspank.data;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.janknspank.proto.Core;
import com.janknspank.proto.Core.Required;
import com.janknspank.proto.Core.StorageMethod;
import com.janknspank.proto.Validator;

public class Database {
  // JDBC driver name and database URL
  private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  private static final String DB_URL =
      "jdbc:mysql://newsserver.ceibyxjobuqr.us-west-2.rds.amazonaws.com:4406/newsserver?useTimezone=true";
//      "jdbc:mysql://localhost/test?useTimezone=true";
  private static final String PROTO_COLUMN_NAME = "proto";
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
        throw new IllegalStateException("$MYSQL_USER is undefined");
      }
      String mysqlPassword = System.getenv("MYSQL_PASSWORD");
      if (mysqlPassword == null) {
        throw new IllegalStateException("$MYSQL_PASSWORD is undefined");
      }
      conn = DriverManager.getConnection(DB_URL, mysqlUser, mysqlPassword); // "hello", "");

      System.out.println("Connected database successfully.");
    }
    return conn;
  }

  private static String getSqlTypeForField(FieldDescriptor fieldDescriptor) {
    switch (fieldDescriptor.getJavaType()) {
      case STRING:
        int stringLength = fieldDescriptor.getOptions().getExtension(Core.stringLength);
        if (stringLength == -1) {
          throw new IllegalStateException("String length undefined for " +
              fieldDescriptor.getName());
        } else if (stringLength <= 0) {
          throw new IllegalStateException("Unsupported string length " + stringLength + " on " +
              fieldDescriptor.getName());
        }
        return (stringLength > 767) ? "BLOB" : "VARCHAR(" + stringLength + ")";
      case LONG:
        return "BIGINT";
      case INT:
        return "INT";
      default:
        throw new IllegalStateException("Unsupported type: " + fieldDescriptor.getJavaType().name());
    }
  }

  private static <T extends Message> Message getDefaultInstance(Class<T> clazz) {
    try {
      return (Message) clazz.getMethod("getDefaultInstance").invoke(null);
    } catch (IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException("Could not reflect on Message type", e);
    }
  }

  /**
   * Returns a map of protocol buffer field descriptors to their StorageMethod
   * types, as defined in our protocol buffer extensions.
   */
  private static <T extends Message> LinkedHashMap<FieldDescriptor, StorageMethod> getFieldMap(
      Class<T> clazz) {
    LinkedHashMap<FieldDescriptor, StorageMethod> fieldMap = Maps.newLinkedHashMap();

    int primaryKeyCount = 0;
    Message message = getDefaultInstance(clazz);
    for (FieldDescriptor field : message.getDescriptorForType().getFields()) {
      StorageMethod storageMethod = field.getOptions().getExtension(Core.storageMethod);
      switch (storageMethod) {
        case PRIMARY_KEY:
          primaryKeyCount++;
          fieldMap.put(field, storageMethod);
          break;

        case BLOB:
        case INDEX:
        case UNIQUE_INDEX:
        case PULL_OUT:
          fieldMap.put(field, storageMethod);
          break;

        default:
          throw new IllegalStateException("Unsupported storage method: " + storageMethod.name());
      }
    }

    // Validate proto message definition.
    if (primaryKeyCount > 1) {
      throw new IllegalStateException("Tables with multiple primary keys are not yet supported");
    }

    return fieldMap;
  }

  /**
   * Returns the MySQL table name that Messages of the passed type should be
   * stored in.
   */
  public static <T extends Message> String getTableName(Class<T> clazz) {
    return clazz.getSimpleName();
  }

  /**
   * Given a protocol buffer message, returns the MySQL statement for creating
   * an appropriate table for storing it.
   */
  public static <T extends Message> String getCreateTableStatement(Class<T> clazz) {
    LinkedHashMap<FieldDescriptor, StorageMethod> fieldMap = getFieldMap(clazz);

    // Start creating the SQL statement.
    StringBuilder sql = new StringBuilder();
    sql.append("CREATE TABLE " + getTableName(clazz) + " (");

    // Add fields.
    for (FieldDescriptor field : fieldMap.keySet()) {
      StorageMethod storageMethod = fieldMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX ||
          storageMethod == StorageMethod.PULL_OUT) {
        sql.append(field.getName() + " " + getSqlTypeForField(field));
        if (storageMethod == StorageMethod.PRIMARY_KEY) {
          sql.append(" PRIMARY KEY");
        }
        if (Required.YES == field.getOptions().getExtension(Core.required)) {
          sql.append(" NOT NULL");
        }
        sql.append(", ");
      }
    }

    // Create an opaque blob field for storing the raw proto.  For consistency,
    // we do this even if all the fields are pulled out.
    sql.append(PROTO_COLUMN_NAME + " BLOB NOT NULL, ");

    // Also keep a timestamp, so we know when rows are updated (why not?).
    sql.append("timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

    return sql.toString();
  }

  /**
   * Given a protocol buffer message, returns a List of MySQL statements for
   * creating indexes on the requested fields.
   */
  public static <T extends Message> List<String> getCreateIndexesStatement(Class<T> clazz) {
    LinkedHashMap<FieldDescriptor, StorageMethod> fieldMap = getFieldMap(clazz);

    List<String> statements = Lists.newArrayList();
    for (FieldDescriptor field : fieldMap.keySet()) {
      StorageMethod storageMethod = fieldMap.get(field);
      if (storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX) {
        StringBuilder sql = new StringBuilder();
        String fieldName = field.getName();
        sql.append("CREATE ");
        if (storageMethod == StorageMethod.UNIQUE_INDEX) {
          sql.append("UNIQUE ");
        }
        sql.append("INDEX " + fieldName + "_index ON " + getTableName(clazz));
        sql.append(" (" + fieldName + ") USING HASH");
        statements.add(sql.toString());
      }
    }
    return statements;
  }

  /**
   * Prepares an INSERT or UPDATE SQL statement by setting the variable fields
   * with the relevant values from the passed proto.  Any primary key, index,
   * or pull-out column is updates in the order it exists in the proto
   * definition.
   */
  private static void prepareInsertOrUpdateStatement(PreparedStatement statement, Message message)
      throws SQLException {
    LinkedHashMap<FieldDescriptor, StorageMethod> fieldMap = getFieldMap(message.getClass());
    int offset = 0;
    for (FieldDescriptor field : fieldMap.keySet()) {
      StorageMethod storageMethod = fieldMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX ||
          storageMethod == StorageMethod.PULL_OUT) {
        ++offset;
        switch (field.getJavaType()) {
          case STRING:
            if (message.hasField(field)) {
              statement.setString(offset, (String) message.getField(field));
            } else {
              statement.setNull(offset, Types.VARCHAR);
            }
            break;
          case INT:
            if (message.hasField(field)) {
              statement.setInt(offset, (int) message.getField(field));
            } else {
              statement.setNull(offset, Types.INTEGER);
            }
            break;
          case LONG:
            if (message.hasField(field)) {
              statement.setLong(offset, (long) message.getField(field));
            } else {
              statement.setNull(offset, Types.BIGINT);
            }
            break;
          default:
            throw new IllegalStateException("Unsupported type: " + field.getJavaType().name());
        }
      }
    }
    statement.setBytes(offset + 1, message.toByteArray());
  }

  /**
   * Returns an INSERT INTO statement for inserting the given protocol buffer
   * message into its respective MySQL table.
   */
  public static PreparedStatement getInsertStatement(Message message) throws SQLException {
    LinkedHashMap<FieldDescriptor, StorageMethod> fieldMap = getFieldMap(message.getClass());

    // Start creating the SQL statement.
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO " + getTableName(message.getClass()) + " (");

    // Add fields.
    int pulledOutFieldCount = 0;
    for (FieldDescriptor field : fieldMap.keySet()) {
      StorageMethod storageMethod = fieldMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX ||
          storageMethod == StorageMethod.PULL_OUT) {
        ++pulledOutFieldCount;
        sql.append(field.getName() + ", ");
      }
    }
    sql.append(PROTO_COLUMN_NAME + ") VALUES (");

    // Add prepared statement fill-in marks (whatever these are called).
    for (int i = 0; i < pulledOutFieldCount + 1; i++) {
      sql.append("?");
      if (i != pulledOutFieldCount) {
        sql.append(", ");
      }
    }
    sql.append(")");

    System.out.println(sql.toString());

    // Prepare the statement!
    PreparedStatement statement = getConnection().prepareStatement(sql.toString());
    prepareInsertOrUpdateStatement(statement, message);

    return statement;
  }

  /**
   * Inserts the passed message into the database.
   */
  public static void insert(Message message) throws ValidationException, DataInternalException {
    Validator.assertValid(message);

    try {
      Database.getInsertStatement(message).execute();
    } catch (SQLException e) {
      throw new DataInternalException("Could not insert article", e);
    }
  }

  /**
   * Returns the value of the passed {@code Message}'s primary key.
   */
  private static String getPrimaryKey(Message message) {
    Map<FieldDescriptor, StorageMethod> fieldMap = getFieldMap(message.getClass());
    for (FieldDescriptor field : fieldMap.keySet()) {
      if (fieldMap.get(field) == StorageMethod.PRIMARY_KEY) {
        return (String) message.getField(field);
      }
    }
    throw new IllegalStateException(
        "Class " + message.getClass().getName() + " has no primary key");
  }

  /**
   * Returns an UPDATE statement for updating the given protocol buffer message
   * in its respective MySQL table.
   */
  public static PreparedStatement getUpdateStatement(Message message) throws SQLException {
    LinkedHashMap<FieldDescriptor, StorageMethod> fieldMap = getFieldMap(message.getClass());

    // Start creating the SQL statement.
    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE " + getTableName(message.getClass()) + " SET ");

    // Add fields.
    int pulledOutFieldCount = 0;
    for (FieldDescriptor field : fieldMap.keySet()) {
      StorageMethod storageMethod = fieldMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX ||
          storageMethod == StorageMethod.PULL_OUT) {
        ++pulledOutFieldCount;
        sql.append(field.getName() + "=?, ");
      }
    }
    sql.append(PROTO_COLUMN_NAME + " =? ");
    sql.append(" WHERE " + getPrimaryKeyField(message.getClass()) + " =? ");

    System.out.println(sql.toString());

    // Prepare the statement!
    PreparedStatement statement = getConnection().prepareStatement(sql.toString());
    prepareInsertOrUpdateStatement(statement, message);
    statement.setString(pulledOutFieldCount + 2, getPrimaryKey(message));

    return statement;
  }

  /**
   * Updates the instance of the passed message using the values in {@code
   * message}, overwriting whatever was stored in the database before.  Returns
   * false if no object was updated (since it didn't yet exist).
   */
  public static boolean update(Message message) throws ValidationException, DataInternalException {
    Validator.assertValid(message);

    try {
      return Database.getUpdateStatement(message).executeUpdate() == 1;
    } catch (SQLException e) {
      throw new DataInternalException("Could not insert article", e);
    }
  }

  /**
   * Returns the column name of the primary key for the passed protocol buffer.
   */
  private static <T extends Message> String getPrimaryKeyField(Class<T> clazz) {
    Map<FieldDescriptor, StorageMethod> fieldMap = getFieldMap(clazz);
    for (FieldDescriptor field : fieldMap.keySet()) {
      if (fieldMap.get(field) == StorageMethod.PRIMARY_KEY) {
        return field.getName();
      }
    }
    throw new IllegalStateException("Class " + clazz.getName() + " has no primary key");
  }

  /**
   * Gets the Message with the specified class {@code clazz} and the given
   * primary key {@code primaryKey}, if one exists.
   */
  public static <T extends Message> T get(String primaryKey, Class<T> clazz)
      throws DataInternalException {
    try {
      PreparedStatement stmt = getConnection().prepareStatement(
          "SELECT * FROM " + getTableName(clazz) + " WHERE " + getPrimaryKeyField(clazz) +
          " =? LIMIT 1");
      stmt.setString(1, primaryKey);
      return createFromResultSet(stmt.executeQuery(), clazz);
    } catch (SQLException e) {
      throw new DataInternalException("Could not execute get", e);
    }
  }

  /**
   * Through reflection, returns a protocol buffer message of the type specified
   * in {@code clazz} using the passed MySQL result set.
   */
  public static <T extends Message> T createFromResultSet(ResultSet result, Class<T> clazz)
      throws SQLException, DataInternalException {
    if (result.next()) {
      try {
        Method parseFromMethod = clazz.getMethod("parseFrom", InputStream.class);

        @SuppressWarnings("unchecked")
        T message = (T) parseFromMethod.invoke(null,
            result.getBlob(PROTO_COLUMN_NAME).getBinaryStream());

        Validator.assertValid(message);
        return message;
      } catch (ValidationException|NoSuchMethodException|IllegalAccessException
          |IllegalArgumentException|InvocationTargetException e) {
        throw new DataInternalException("Could not create article object: " + e.getMessage(), e);
      }
    }
    return null;
  }

  /**
   * Deletes the passed object from the database.
   * @throws DataInternalException if the object could not be deleted
   */
  public static void delete(Message message) throws DataInternalException {
    String primaryKey = getPrimaryKey(message);
    PreparedStatement stmt;
    try {
      stmt = getConnection().prepareStatement(
          "DELETE FROM " + getTableName(message.getClass()) +
          " WHERE " + getPrimaryKeyField(message.getClass()) + " =? LIMIT 1");
      stmt.setString(1, primaryKey);
      if (stmt.executeUpdate() != 1) {
        throw new DataInternalException("Could not find object to delete, primary key = " +
            primaryKey);
      }
    } catch (SQLException e) {
      throw new DataInternalException("Error executing delete, primary key = " + primaryKey, e);
    }
  }
}