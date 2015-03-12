package com.janknspank.database;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.janknspank.common.Asserts;
import com.janknspank.common.Logger;
import com.janknspank.database.ExtensionsProto.Required;
import com.janknspank.database.ExtensionsProto.StorageMethod;
import com.janknspank.database.ExtensionsProto.StringCharset;
import com.janknspank.database.QueryOption.DescendingSort;
import com.janknspank.database.QueryOption.Limit;
import com.janknspank.database.QueryOption.LimitWithOffset;
import com.janknspank.database.QueryOption.Sort;
import com.janknspank.database.QueryOption.WhereEquals;
import com.janknspank.database.QueryOption.WhereEqualsEnum;
import com.janknspank.database.QueryOption.WhereEqualsIgnoreCase;
import com.janknspank.database.QueryOption.WhereEqualsNumber;
import com.janknspank.database.QueryOption.WhereFalse;
import com.janknspank.database.QueryOption.WhereGreaterThan;
import com.janknspank.database.QueryOption.WhereGreaterThanOrEquals;
import com.janknspank.database.QueryOption.WhereInequality;
import com.janknspank.database.QueryOption.WhereLessThan;
import com.janknspank.database.QueryOption.WhereLessThanOrEquals;
import com.janknspank.database.QueryOption.WhereLike;
import com.janknspank.database.QueryOption.WhereLikeIgnoreCase;
import com.janknspank.database.QueryOption.WhereNotEquals;
import com.janknspank.database.QueryOption.WhereNotEqualsEnum;
import com.janknspank.database.QueryOption.WhereNotEqualsNumber;
import com.janknspank.database.QueryOption.WhereNotLike;
import com.janknspank.database.QueryOption.WhereNotLikeIgnoreCase;
import com.janknspank.database.QueryOption.WhereNotNull;
import com.janknspank.database.QueryOption.WhereNull;
import com.janknspank.database.QueryOption.WhereOption;
import com.janknspank.database.QueryOption.WhereTrue;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

public class SqlCollection<T extends Message> extends Collection<T> {
  private static final Logger LOG = new Logger(SqlCollection.class);
  private static final String PROTO_COLUMN_NAME = "proto";
  private Connection __connection; // DO NOT USE THIS DIRECTLY!!

  public SqlCollection(Class<T> clazz) {
    super(clazz);
  }

  protected Connection getConnection() throws DatabaseSchemaException {
    if (__connection == null) {
      __connection = SqlConnection.getConnection();
    }
    return __connection;
  }

  @Override
  public void createTable() throws DatabaseSchemaException {
    try {
      getConnection().prepareStatement(getCreateTableSql()).execute();
      for (String statement : getCreateIndexesSql(clazz)) {
        getConnection().prepareStatement(statement).execute();
      }
    } catch (SQLException e) {
      throw new DatabaseSchemaException(
          "Could not create table " + clazz.getSimpleName() + ": " + e.getMessage(), e);
    }
    LOG.info("Table created: " + getTableName());
  }

  private String getSqlTypeForField(FieldDescriptor fieldDescriptor)
      throws DatabaseSchemaException {
    switch (fieldDescriptor.getJavaType()) {
      case STRING:
        int stringLength = fieldDescriptor.getOptions().getExtension(ExtensionsProto.stringLength);
        if (stringLength == -1) {
          throw new IllegalStateException("String length undefined for "
              + fieldDescriptor.getName());
        } else if (stringLength <= 0) {
          throw new IllegalStateException("Unsupported string length " + stringLength + " on "
              + fieldDescriptor.getName());
        } else if (stringLength > 65535) {
          throw new IllegalStateException("MySQL only allows strings up to is 65535 chars long");
        }
        StringCharset charset =
            fieldDescriptor.getOptions().getExtension(ExtensionsProto.stringCharset);
        String sqlType = (stringLength > 767)
            ? "BLOB"
            : "VARCHAR(" + stringLength + ")";
        if (charset == StringCharset.UTF8) {
          return sqlType + " CHARACTER SET utf8 COLLATE utf8_bin";
        } else {
          return sqlType + " CHARACTER SET latin1 COLLATE latin1_bin";
        }
      case LONG:
        return "BIGINT";
      case ENUM:
      case INT:
        return "INT";
      case BOOLEAN:
        return "BOOLEAN";
      case DOUBLE:
        return "DOUBLE";
      default:
        throw new DatabaseSchemaException(
            "Unsupported type: " + fieldDescriptor.getJavaType().name());
    }
  }

  /**
   * Given a protocol buffer message, returns the MySQL statement for creating
   * an appropriate table for storing it.
   */
  private String getCreateTableSql() throws DatabaseSchemaException {
    // Start creating the SQL statement.
    StringBuilder sql = new StringBuilder();
    sql.append("CREATE TABLE " + getTableName() + " (");

    // Add fields.
    for (FieldDescriptor field : storageMethodMap.keySet()) {
      StorageMethod storageMethod = storageMethodMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX ||
          storageMethod == StorageMethod.PULL_OUT) {
        sql.append(field.getName() + " " + getSqlTypeForField(field));
        if (storageMethod == StorageMethod.PRIMARY_KEY) {
          sql.append(" PRIMARY KEY");
        }
        if (Required.YES == field.getOptions().getExtension(ExtensionsProto.required)) {
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
  private List<String> getCreateIndexesSql(Class<T> clazz) {
    List<String> statements = Lists.newArrayList();
    for (FieldDescriptor field : storageMethodMap.keySet()) {
      StorageMethod storageMethod = storageMethodMap.get(field);
      if (storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX) {
        StringBuilder sql = new StringBuilder();
        String fieldName = field.getName();
        sql.append("CREATE ");
        if (storageMethod == StorageMethod.UNIQUE_INDEX) {
          sql.append("UNIQUE ");
        }
        sql.append("INDEX " + fieldName + "_index ON " + getTableName());
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
  private void prepareInsertOrUpdateStatement(PreparedStatement statement, Message message)
      throws SQLException, DatabaseSchemaException {
    int offset = 0;
    for (FieldDescriptor field : storageMethodMap.keySet()) {
      StorageMethod storageMethod = storageMethodMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX ||
          storageMethod == StorageMethod.PULL_OUT) {
        ++offset;
        if (!message.hasField(field)) {
          statement.setNull(offset, Types.VARCHAR);
        } else {
          switch (field.getJavaType()) {
            case STRING:
              statement.setString(offset, (String) message.getField(field));
              break;
            case INT:
              statement.setInt(offset, (int) message.getField(field));
              break;
            case LONG:
              statement.setLong(offset, (long) message.getField(field));
              break;
            case ENUM:
              EnumValueDescriptor v = (EnumValueDescriptor) message.getField(field);
              statement.setInt(offset, v.getNumber());
              break;
            case BOOLEAN:
              statement.setBoolean(offset, (boolean) message.getField(field));
              break;
            case DOUBLE:
              statement.setDouble(offset, (double) message.getField(field));
              break;
            default:
              throw new DatabaseSchemaException("Unsupported type: " + field.getJavaType().name());
          }
        }
      }
    }

    statement.setBytes(offset + 1, cleanDoNotStoreFields(message).toByteArray());
  }

  /**
   * Returns an INSERT INTO statement for inserting the given protocol buffer
   * message into its respective MySQL table.
   */
  private PreparedStatement getRawInsertStatement(T message)
      throws DatabaseSchemaException, SQLException {
    // Start creating the SQL statement.
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO " + getTableName() + " (");

    // Add fields.
    int pulledOutFieldCount = 0;
    for (FieldDescriptor field : storageMethodMap.keySet()) {
      StorageMethod storageMethod = storageMethodMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY
          || storageMethod == StorageMethod.INDEX
          || storageMethod == StorageMethod.UNIQUE_INDEX
          || storageMethod == StorageMethod.PULL_OUT) {
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

    LOG.fine(sql.toString());

    // Prepare the statement!
    return getConnection().prepareStatement(sql.toString());
  }

  /**
   * Inserts the passed messages into the database.  All messages passed must be
   * of the same type.
   */
  @Override
  public int insert(Iterable<T> messages) throws DatabaseSchemaException, DatabaseRequestException {
    if (Iterables.isEmpty(messages)) {
      return 0;
    }

    T firstMessage = Iterables.getFirst(messages, null);
    PreparedStatement statement = null;
    try {
      statement = getRawInsertStatement(firstMessage);
      for (T message : messages) {
        Validator.assertValid(message);
        Asserts.assertTrue(firstMessage.getClass().equals(message.getClass()),
            "Types do not match", DatabaseRequestException.class);
        prepareInsertOrUpdateStatement(statement, message);
        statement.addBatch();
      }
      return sumIntArray(statement.executeBatch());

    } catch (SQLException e) {
      throw new DatabaseSchemaException(
          "Could not insert " + getTableName() + ": " + e.getMessage(), e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {}
      }
    }
  }

  /**
   * Returns the number of columns in the table this passed proto message
   * class is stored in.
   */
  private int getColumnCount() {
    // Start with 2, since there's always 'proto' and 'timestamp', regardless
    // of the schema.
    int columnCount = 2;

    for (FieldDescriptor field : storageMethodMap.keySet()) {
      StorageMethod storageMethod = storageMethodMap.get(field);
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
  private PreparedStatement getRawUpdateStatement(WhereOption... whereOptions)
      throws DatabaseSchemaException, SQLException {
    // Start creating the SQL statement.
    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE " + getTableName() + " SET ");

    // Add fields.
    for (FieldDescriptor field : storageMethodMap.keySet()) {
      StorageMethod storageMethod = storageMethodMap.get(field);
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX ||
          storageMethod == StorageMethod.PULL_OUT) {
        sql.append(field.getName() + "=?, ");
      }
    }
    sql.append(PROTO_COLUMN_NAME + "=?");
    sql.append(getWhereClauseSql(whereOptions));

    LOG.fine(sql.toString());

    return getConnection().prepareStatement(sql.toString());
  }

  /**
   * Updates the passed messages, overwriting whatever was stored in the
   * database before.  Returns the number of modified objects.
   */
  @Override
  public int update(Iterable<T> messages, WhereOption... whereOptions)
      throws DatabaseSchemaException, DatabaseRequestException {
    if (Iterables.isEmpty(messages)) {
      return 0;
    }

    T firstMessage = Iterables.getFirst(messages, null);
    for (WhereOption whereOption : whereOptions) {
      if (primaryKeyField != null && primaryKeyField.equals(whereOption.getFieldName())) {
        throw new IllegalStateException("Update WhereEquals options cannot put "
            + "additional constraints on the primary key");
      }
    }
    whereOptions = ObjectArrays.concat(
        new WhereEquals(primaryKeyField, Database.getPrimaryKey(firstMessage)),
        whereOptions);

    int columnCount = getColumnCount();
    PreparedStatement statement = null;
    try {
      statement = getRawUpdateStatement(whereOptions);
      for (T message : messages) {
        Validator.assertValid(message);
        Asserts.assertTrue(firstMessage.getClass().equals(message.getClass()),
            "Types do not match", DatabaseRequestException.class);
        whereOptions[0] = new WhereEquals(primaryKeyField,
            Database.getPrimaryKey(message));
        prepareInsertOrUpdateStatement(statement, message);
        int i = 0;
        for (Object whereValue : getWhereValues(whereOptions)) {
          setObject(statement, columnCount + (i++), whereValue);
        }
        statement.addBatch();
      }
      return SqlCollection.sumIntArray(statement.executeBatch());

    } catch (SQLException e) {
      throw new DatabaseSchemaException(
          "Could not insert " + getTableName() + ": " + e.getMessage(), e);
    } finally {
      try {
        statement.close();
      } catch (SQLException e) {}
    }
  }

  /**
   * Convenience method for setting a string/long/double/int etc. at a
   * particular index in a Prepared Statement.
   */
  private void setObject(PreparedStatement statement, int parameterIndex, Object whereValue)
      throws SQLException {
    if (whereValue instanceof String) {
      statement.setString(parameterIndex, (String) whereValue);
    } else if (whereValue instanceof Long) {
      statement.setLong(parameterIndex, (Long) whereValue);
    } else if (whereValue instanceof Double) {
      statement.setDouble(parameterIndex, (Double) whereValue);
    } else if (whereValue instanceof Integer) {
      statement.setInt(parameterIndex, (Integer) whereValue);
    } else {
      // Go ahead and add more type support above if you need to.
      throw new IllegalStateException(
          "Unsupported Number type: " + whereValue.getClass().getSimpleName());
    }
  }

  private String getLimitSql(QueryOption[] options) {
    List<Limit> queryOptionList = QueryOption.getList(options, Limit.class);
    if (queryOptionList.size() > 1) {
      throw new IllegalStateException("Duplicate definitions of Limit not allowed");
    }
    if (queryOptionList.isEmpty()) {
      return "";
    }
    Limit limitOption = (Limit) queryOptionList.get(0);
    StringBuilder sql = new StringBuilder();
    sql.append(" LIMIT ");
    if (limitOption instanceof LimitWithOffset) {
      sql.append(((LimitWithOffset) limitOption).getOffset()).append(", ");
    }
    sql.append(limitOption.getLimit());
    return sql.toString();
  }

  private String getWhereClauseSql(QueryOption[] options) {
    StringBuilder sql = new StringBuilder();
    for (WhereOption whereEquals :
        Iterables.concat(
            QueryOption.getList(options, WhereEquals.class),
            QueryOption.getList(options, WhereEqualsEnum.class),
            QueryOption.getList(options, WhereEqualsNumber.class))) {
      int size = whereEquals.getFieldCount();
      if (size == 0
          && (whereEquals instanceof WhereEquals
              || whereEquals instanceof WhereEqualsEnum
              || whereEquals instanceof WhereEqualsNumber)) {
        throw new IllegalStateException("Where clause contains no values - "
            + "This should have been caught earlier.");
      }
      if (size == 0
          && (whereEquals instanceof WhereNotEquals
              || whereEquals instanceof WhereNotEqualsEnum
              || whereEquals instanceof WhereNotEqualsNumber)) {
        // OK, don't write anything - Everything doesn't equal nothing.
        continue;
      }
      if (whereEquals instanceof WhereEqualsIgnoreCase) {
        sql.append(sql.length() == 0 ? " WHERE " : " AND ")
            .append("LOWER(").append(whereEquals.getFieldName()).append(")")
            .append(" IN (")
            .append(Joiner.on(",").join(Iterables.limit(Iterables.cycle("?"), size)))
            .append(")");
      } else {
        sql.append(sql.length() == 0 ? " WHERE " : " AND ")
            .append(whereEquals.getFieldName())
            .append(whereEquals instanceof WhereNotEquals ? " NOT" : "")
            .append(" IN (")
            .append(Joiner.on(",").join(Iterables.limit(Iterables.cycle("?"), size)))
            .append(")");
      }
    }
    for (WhereLike whereLike :
        QueryOption.getList(options, WhereLike.class)) {
      if (whereLike instanceof WhereLikeIgnoreCase
          || whereLike instanceof WhereNotLikeIgnoreCase) {
        sql.append(sql.length() == 0 ? " WHERE " : " AND ")
            .append("LOWER(").append(whereLike.getFieldName()).append(")")
            .append(whereLike instanceof WhereNotLike ? " NOT" : "")
            .append(" LIKE ?");
      } else {
        sql.append(sql.length() == 0 ? " WHERE " : " AND ")
            .append(whereLike.getFieldName())
            .append(whereLike instanceof WhereNotLike ? " NOT" : "")
            .append(" LIKE ?");
      }
    }
    for (WhereTrue whereTrue : QueryOption.getList(options, WhereTrue.class)) {
      sql.append(sql.length() == 0 ? " WHERE " : " AND ")
          .append(whereTrue.getFieldName())
          .append(" IS TRUE");
    }
    for (WhereFalse whereFalse : QueryOption.getList(options, WhereFalse.class)) {
      sql.append(sql.length() == 0 ? " WHERE " : " AND ")
          .append(whereFalse.getFieldName())
          .append(" IS FALSE");
    }
    for (WhereNull whereNull : QueryOption.getList(options, WhereNull.class)) {
      sql.append(sql.length() == 0 ? " WHERE " : " AND ")
          .append(whereNull.getFieldName())
          .append(" IS")
          .append(whereNull instanceof WhereNotNull ? " NOT" : "")
          .append(" NULL");
    }
    for (WhereInequality whereInequality :
        QueryOption.getList(options, WhereInequality.class)) {
      String comparatorStr;
      if (whereInequality instanceof WhereGreaterThan) {
        comparatorStr = ">";
      } else if (whereInequality instanceof WhereGreaterThanOrEquals) {
        comparatorStr = ">=";
      } else if (whereInequality instanceof WhereLessThan) {
        comparatorStr = "<";
      } else if (whereInequality instanceof WhereLessThanOrEquals) {
        comparatorStr = "<=";
      } else {
        throw new IllegalStateException("Unexpected inequality: " + whereInequality.getClass());
      }
      sql.append(sql.length() == 0 ? " WHERE " : " AND ")
          .append(whereInequality.getFieldName())
          .append(" " + comparatorStr + " ?");
    }
    return sql.toString();
  }

  private Iterable<Object> getWhereValues(QueryOption[] options) {
    List<Object> values = Lists.newArrayList();
    for (WhereOption whereEquals :
        Iterables.concat(
            QueryOption.getList(options, WhereEquals.class),
            QueryOption.getList(options, WhereEqualsEnum.class),
            QueryOption.getList(options, WhereEqualsNumber.class))) {
      if (whereEquals instanceof WhereEqualsIgnoreCase) {
        for (String value : ((WhereEqualsIgnoreCase) whereEquals).getValues()) {
          values.add(value.toLowerCase());
        }
      } else if (whereEquals instanceof WhereEquals) {
        Iterables.addAll(values, ((WhereEquals) whereEquals).getValues());
      } else if (whereEquals instanceof WhereEqualsEnum) {
        Iterables.addAll(values, Iterables.transform(((WhereEqualsEnum) whereEquals).getValues(),
            new Function<Enum<?>, String>() {
              @Override
              public String apply(Enum<?> e) {
                return e.name();
              }
            }));
      } else if (whereEquals instanceof WhereEqualsNumber) {
        Iterables.addAll(values, ((WhereEqualsNumber) whereEquals).getValues());
      } else {
        throw new IllegalStateException(
            "Unknown WhereOption: " + whereEquals.getClass().getSimpleName());
      }
    }
    for (WhereLike whereLike :
        QueryOption.getList(options, WhereLike.class)) {
      if (whereLike instanceof WhereLikeIgnoreCase
          || whereLike instanceof WhereNotLikeIgnoreCase) {
        values.add(whereLike.getValue().toLowerCase());
      } else {
        values.add(whereLike.getValue());
      }
    }
    for (WhereInequality whereInequality :
      QueryOption.getList(options, WhereInequality.class)) {
    values.add(whereInequality.getValue());
  }
    return values;
  }

  private String getOrderBySql(QueryOption[] options) {
    StringBuilder sb = new StringBuilder();
    for (Sort sort : QueryOption.getList(options, Sort.class)) {
      sb.append((sb.length() == 0) ? " ORDER BY " : ", ");
      sb.append(sort.getFieldName());
      if (sort instanceof DescendingSort) {
        sb.append(" DESC");
      }
    }
    return sb.toString();
  }

  /**
   * Gets Messages with the specified class {@code clazz} and the field values,
   * if they exist.
   */
  public Iterable<T> get(QueryOption... options) throws DatabaseSchemaException {
    if (QueryOption.isWhereClauseEmpty(options)) {
      return ImmutableList.of();
    }

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM " + getTableName());
    sql.append(getWhereClauseSql(options));
    sql.append(getOrderBySql(options));
    sql.append(getLimitSql(options));

    PreparedStatement statement = null;
    try {
      statement = getConnection().prepareStatement(sql.toString());
      int i = 0;
      for (Object whereValue : getWhereValues(options)) {
        setObject(statement, ++i, whereValue);
      }
      return createListFromResultSet(statement.executeQuery());
    } catch (MySQLSyntaxErrorException e) {
      throw new DatabaseSchemaException("Invalid query: " + sql, e);
    } catch (SQLException e) {
      throw new DatabaseSchemaException("Could not execute get: " + e.getMessage(), e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {}
      }
    }
  }

  /**
   * Deletes objects from the specified object {@code clazz} that match the
   * given query options.
   */
  @Override
  public int delete(QueryOption... options) throws DatabaseSchemaException {
    StringBuilder sql = new StringBuilder();
    sql.append("DELETE FROM " + getTableName());
    sql.append(getWhereClauseSql(options));
    sql.append(getOrderBySql(options));
    sql.append(getLimitSql(options));

    PreparedStatement statement = null;
    try {
      statement = getConnection().prepareStatement(sql.toString());
      int i = 0;
      for (Object whereValue : getWhereValues(options)) {
        setObject(statement, ++i, whereValue);
      }
      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseSchemaException("Could not execute get: " + e.getMessage()
          + ": " + e.getMessage(), e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {}
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

  /**
   * Returns the MySQL table name that this collection should be stored in.
   */
  public String getTableName() {
    return clazz.getSimpleName();
  }

  /**
   * Through reflection, returns a protocol buffer message of the type specified
   * in {@code clazz} using the passed MySQL result set.
   */
  public T createFromResultSet(ResultSet result)
      throws SQLException, DatabaseSchemaException {
    if (result.next()) {
      try {
        Method parseFromMethod = clazz.getMethod("parseFrom", InputStream.class);

        @SuppressWarnings("unchecked")
        T message = (T) parseFromMethod.invoke(null,
            result.getBlob(PROTO_COLUMN_NAME).getBinaryStream());

        Validator.assertValid(message);
        return message;
      } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | DatabaseRequestException e) {
        throw new DatabaseSchemaException(
            "Could not create " + clazz.getSimpleName() + " object: " + e.getMessage(), e);
      }
    }
    return null;
  }

  public List<T> createListFromResultSet(ResultSet result) throws DatabaseSchemaException {
    List<T> messages = Lists.newArrayList();
    try {
      while (!result.isAfterLast()) {
        T message = createFromResultSet(result);
        if (message == null) {
          break;
        }
        messages.add(message);
      }
    } catch (SQLException e) {
      throw new DatabaseSchemaException("Could not read message: " + e.getMessage(), e);
    }
    return messages;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T set(T message, String fieldName, Object value)
      throws DatabaseSchemaException, DatabaseRequestException {
    if (value instanceof Iterable<?>) {
      return setIterable(message, fieldName, (Iterable<Object>) value);
    }
    String classAndField = message.getClass().getSimpleName() + "." + fieldName;
    FieldDescriptor field = Database.getFieldDescriptor(message.getClass(), fieldName);
    Collection.validateType(field, value);

    Message.Builder messageBuilder = message.toBuilder();
    messageBuilder.setField(field, value);
    message = (T) messageBuilder.build();
    if (update(ImmutableList.of(message)) != 1) {
      throw new DatabaseSchemaException("Object not found: " + classAndField
          + " (id=" + Database.getPrimaryKey(message) + ")");
    }
    return message;
  }

  @SuppressWarnings("unchecked")
  private T setIterable(T message, String fieldName, Iterable<Object> values)
      throws DatabaseSchemaException, DatabaseRequestException {
    String classAndField = message.getClass().getSimpleName() + "." + fieldName;
    FieldDescriptor field = Database.getFieldDescriptor(message.getClass(), fieldName);
    Collection.validateType(field, values);

    Message.Builder messageBuilder = message.toBuilder();
    messageBuilder.clearField(field);
    for (Object o : values) {
      messageBuilder.addRepeatedField(field, o);
    }
    message = (T) messageBuilder.build();
    if (update(ImmutableList.of(message)) != 1) {
      throw new DatabaseSchemaException("Object not found: " + classAndField
          + " (id=" + Database.getPrimaryKey(message) + ")");
    }
    return message;
  }

  @Override
  public <U extends Object> void push(T message, String fieldName, Iterable<U> values)
      throws DatabaseSchemaException, DatabaseRequestException {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the number of rows in this table that match the specified
   * QueryOptions.
   */
  @SuppressWarnings("resource")
  public long getSize(WhereOption... whereOptions) throws DatabaseSchemaException {
    PreparedStatement statement = null;
    ResultSet results = null;
    try {
      statement = getConnection().prepareStatement(
          "SELECT count(*) FROM " + this.getTableName()
              + getWhereClauseSql(whereOptions));
      int i = 0;
      for (Object whereValue : getWhereValues(whereOptions)) {
        setObject(statement, ++i, whereValue);
      }
      results = statement.executeQuery();
      while (results.next()) {
        return results.getLong(1);
      }
    } catch (SQLException e) {
      throw new DatabaseSchemaException("Could not find collection size: " + e.getMessage(), e);
    } finally {
      try {
        if (statement != null) {
          statement.close();
        }
        if (results != null) {
          results.close();
        }
      } catch (SQLException e) {}
    }
    throw new DatabaseSchemaException("Failed to get collection size");
  }
}
