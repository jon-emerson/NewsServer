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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.janknspank.common.Asserts;
import com.janknspank.proto.Extensions;
import com.janknspank.proto.Extensions.Required;
import com.janknspank.proto.Extensions.StorageMethod;
import com.janknspank.proto.Validator;

public class Database {
  // JDBC driver name and database URL
  private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  private static final String DB_URL =
      "jdbc:mysql://newsserver.ceibyxjobuqr.us-west-2.rds.amazonaws.com:4406/newsserver?"
      + "useTimezone=true&rewriteBatchedStatements=true";
//      "jdbc:mysql://localhost/test?useTimezone=true&rewriteBatchedStatements=true";
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
        int stringLength = fieldDescriptor.getOptions().getExtension(Extensions.stringLength);
        if (stringLength == -1) {
          throw new IllegalStateException("String length undefined for "
              + fieldDescriptor.getName());
        } else if (stringLength <= 0) {
          throw new IllegalStateException("Unsupported string length " + stringLength + " on "
              + fieldDescriptor.getName());
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
      throw new IllegalStateException("Could not reflect on Message type: " + e.getMessage(), e);
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
      StorageMethod storageMethod = field.getOptions().getExtension(Extensions.storageMethod);
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
        if (Required.YES == field.getOptions().getExtension(Extensions.required)) {
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

    // Clear out any DO_NOT_STORE fields.
    Message.Builder messageBuilder = message.toBuilder();
    for (FieldDescriptor field : fieldMap.keySet()) {
      StorageMethod storageMethod = fieldMap.get(field);
      if (storageMethod == StorageMethod.DO_NOT_STORE) {
        messageBuilder.clearField(field);
      }
    }
    statement.setBytes(offset + 1, messageBuilder.build().toByteArray());
  }

  /**
   * Returns an INSERT INTO statement for inserting the given protocol buffer
   * message into its respective MySQL table.
   */
  public static PreparedStatement getRawInsertStatement(Message message) throws SQLException {
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
    // Add an extra one for the proto serialization.
    for (int i = 0; i < pulledOutFieldCount + 1; i++) {
      sql.append("?");
      if (i != pulledOutFieldCount) {
        sql.append(", ");
      }
    }
    sql.append(")");

    System.out.println(sql.toString());

    // Prepare the statement!
    return getConnection().prepareStatement(sql.toString());
    //prepareInsertOrUpdateStatement(statement, message);

    //return statement;
  }

  /**
   * Inserts the passed message into the database.
   */
  public static void insert(Message message) throws ValidationException, DataInternalException {
    Validator.assertValid(message);

    try {
      PreparedStatement stmt = Database.getRawInsertStatement(message);
      prepareInsertOrUpdateStatement(stmt, message);
      stmt.execute();
    } catch (SQLException e) {
      throw new DataInternalException("Insert failed: " + e.getMessage(), e);
    }
  }

 /**
   * Inserts the passed messages into the database.  All messages passed must be
   * of the same type.
   */
  public static <T extends Message> int insert(Iterable<T> messages)
      throws ValidationException, DataInternalException {
    if (Iterables.isEmpty(messages)) {
      return 0;
    }

    Message firstMessage = Iterables.getFirst(messages, null);
    try {
      PreparedStatement stmt = Database.getRawInsertStatement(firstMessage);
      for (T message : messages) {
        Validator.assertValid(message);
        Asserts.assertTrue(firstMessage.getClass().equals(message.getClass()),
            "Types do not match");
        prepareInsertOrUpdateStatement(stmt, message);
        stmt.addBatch();
      }
      return Database.sumIntArray(stmt.executeBatch());

    } catch (SQLException e) {
      throw new DataInternalException(
          "Could not insert " + getTableName(firstMessage.getClass()) + ": " + e.getMessage(), e);
    }
  }

  /**
   * Either updates the message in the database, or inserts it if it doesn't
   * exist.
   * NOTE(jonemerson): This implementation might not be amazingly efficient
   * against MySQL, but I'm still providing this API in case we switch to a
   * backend that handles upserts better.
   */
  public static void upsert(Message message) throws ValidationException, DataInternalException {
    if (!update(message)) {
      insert(message);
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
   * Returns the value of the passed {@code Message}'s primary key.
   */
  private static <T extends Message> Iterable<String> getPrimaryKeys(Iterable<T> messages)
      throws ValidationException {
    List<String> primaryKeys = Lists.newArrayList();
    T firstMessage = Iterables.getFirst(messages, null);
    for (T message : messages) {
      Asserts.assertTrue(firstMessage.getClass().equals(message.getClass()),
          "Types do not match");
      primaryKeys.add(getPrimaryKey(message));
    }
    return primaryKeys;
  }

  /**
   * Returns the number of columns in the table this passed proto message
   * class is stored in.
   */
  private static <T extends Message> int getColumnCount(Class<T> clazz) {
    // Start with 2, since there's always 'proto' and 'timestamp', regardless
    // of the schema.
    int columnCount = 2;

    Map<FieldDescriptor, StorageMethod> fieldMap = getFieldMap(clazz);
    for (FieldDescriptor field : fieldMap.keySet()) {
      StorageMethod storageMethod = fieldMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX ||
          storageMethod == StorageMethod.PULL_OUT) {
        ++columnCount;
      }
    }
    return columnCount;
  }

  /**
   * Returns an UPDATE statement for updating the given protocol buffer message
   * in its respective MySQL table.
   */
  public static PreparedStatement getRawUpdateStatement(Message message) throws SQLException {
    LinkedHashMap<FieldDescriptor, StorageMethod> fieldMap = getFieldMap(message.getClass());

    // Start creating the SQL statement.
    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE " + getTableName(message.getClass()) + " SET ");

    // Add fields.
    for (FieldDescriptor field : fieldMap.keySet()) {
      StorageMethod storageMethod = fieldMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX ||
          storageMethod == StorageMethod.PULL_OUT) {
        sql.append(field.getName() + "=?, ");
      }
    }
    sql.append(PROTO_COLUMN_NAME + "=? ");
    sql.append(" WHERE " + getPrimaryKeyField(message.getClass()) + " =? ");

    System.out.println(sql.toString());

    return getConnection().prepareStatement(sql.toString());
  }

  /**
   * Updates the instance of the passed message using the values in {@code
   * message}, overwriting whatever was stored in the database before.  Returns
   * false if no object was updated (since it doesn't yet exist).
   */
  public static boolean update(Message message) throws ValidationException, DataInternalException {
    return update(ImmutableList.of(message)) == 1;
  }

  /**
   * Updates the passed messages, overwriting whatever was stored in the
   * database before.  Returns the number of modified objects.
   */
  public static <T extends Message> int update(Iterable<T> messages)
      throws ValidationException, DataInternalException {
    if (Iterables.isEmpty(messages)) {
      return 0;
    }

    T firstMessage = Iterables.getFirst(messages, null);
    int columnCount = getColumnCount(firstMessage.getClass());
    try {
      PreparedStatement stmt = Database.getRawUpdateStatement(firstMessage);
      for (T message : messages) {
        Validator.assertValid(message);
        Asserts.assertTrue(firstMessage.getClass().equals(message.getClass()),
            "Types do not match");
        prepareInsertOrUpdateStatement(stmt, message);
        stmt.setString(columnCount, getPrimaryKey(message));
        stmt.addBatch();
      }
      return Database.sumIntArray(stmt.executeBatch());

    } catch (SQLException e) {
      throw new DataInternalException(
          "Could not insert " + getTableName(firstMessage.getClass()) + ": " + e.getMessage(), e);
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
    return Iterables.getFirst(get(ImmutableList.of(primaryKey), clazz), null);
  }

  /**
   * Gets Messages with the specified class {@code clazz} and the given
   * primary keys {@code primaryKeys}, if they exist.
   */
  public static <T extends Message> List<T> get(Iterable<String> primaryKeys, Class<T> clazz)
      throws DataInternalException {

    try {
      String questionMarks = Joiner.on(",").join(
          Iterables.limit(Iterables.cycle("?"), Iterables.size(primaryKeys)));
      PreparedStatement stmt = getConnection().prepareStatement(
          "SELECT * FROM " + getTableName(clazz) + " WHERE " + getPrimaryKeyField(clazz)
          + " IN (" + questionMarks + ")");
      int i = 0;
      for (String primaryKey : primaryKeys) {
        stmt.setString(++i, primaryKey);
      }
      if (i == 0) {
        throw new DataInternalException("No primary keys given to fetch");
      }
      return createListFromResultSet(stmt.executeQuery(), clazz);
    } catch (SQLException e) {
      throw new DataInternalException("Could not execute get: " + e.getMessage()
          + ": " + e.getMessage(), e);
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

  public static <T extends Message> List<T> createListFromResultSet(
      ResultSet result, Class<T> clazz) throws DataInternalException {
    List<T> messages = Lists.newArrayList();
    try {
      while (!result.isAfterLast()) {
        T message = Database.createFromResultSet(result, clazz);
        if (message == null) {
          break;
        }
        messages.add(message);
      }
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching favorites: " + e.getMessage(), e);
    }
    return messages;
  }

  /**
   * Deletes the object with the specified primary key from the table specified
   * by the passed-in class.
   * @throws DataInternalException if the object could not be deleted, including
   *     if it could not be found
   */
  public static <T extends Message> void deletePrimaryKey(String primaryKey, Class<T> clazz)
      throws DataInternalException {
    if (deletePrimaryKeys(ImmutableList.of(primaryKey), clazz) != 1) {
      throw new DataInternalException("Could not find object to delete, primary key = "
          + primaryKey);
    }
  }

  /**
   * Deletes objects with the specified primary keys from the table specified
   * by the passed-in class.
   */
  public static <T extends Message> int deletePrimaryKeys(
      Iterable<String> primaryKeys, Class<T> clazz) throws DataInternalException {
    PreparedStatement stmt;
    try {
      stmt = getConnection().prepareStatement(
          "DELETE FROM " + getTableName(clazz)
          + " WHERE " + getPrimaryKeyField(clazz) + " =? LIMIT 1");
      for (String primaryKey : primaryKeys) {
        stmt.setString(1, primaryKey);
        stmt.addBatch();
      }
      int numModified = 0;
      for (int modCount : stmt.executeBatch()) {
        numModified += modCount;
      }
      return numModified;

    } catch (SQLException e) {
      throw new DataInternalException("Error executing delete: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes the passed object from the database.
   * @throws DataInternalException if the object could not be deleted, including
   *     if it could not be found
   */
  public static void delete(Message message) throws DataInternalException {
    deletePrimaryKey(getPrimaryKey(message), message.getClass());
  }

  /**
   * Deletes the passed object from the database.
   * @throws DataInternalException if the object could not be deleted, including
   *     if it could not be found
   */
  public static <T extends Message> void delete(Iterable<T> messages) throws DataInternalException {
    if (!Iterables.isEmpty(messages)) {
      try {
        deletePrimaryKeys(getPrimaryKeys(messages), Iterables.getFirst(messages, null).getClass());
      } catch (ValidationException e) {
        throw new DataInternalException("Internal error: " + e.getMessage(), e);
      }
    }
  }

  /**
   * Returns the sum of all the integers in the passed array.  Useful for
   * collating # of rows modified by batch statements.
   */
  public static int sumIntArray(int[] array) {
    int sum = 0;
    for (int i = 0; i < array.length; i++) {
      sum += array[i];
    }
    return sum;
  }
}
