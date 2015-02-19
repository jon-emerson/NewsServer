package com.janknspank.database;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import com.google.api.client.util.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.janknspank.common.Logger;
import com.janknspank.database.ExtensionsProto.StorageMethod;
import com.janknspank.database.QueryOption.LimitWithOffset;
import com.janknspank.database.QueryOption.WhereEquals;
import com.janknspank.database.QueryOption.WhereEqualsEnum;
import com.janknspank.database.QueryOption.WhereEqualsIgnoreCase;
import com.janknspank.database.QueryOption.WhereEqualsNumber;
import com.janknspank.database.QueryOption.WhereLike;
import com.janknspank.database.QueryOption.WhereNotEquals;
import com.janknspank.database.QueryOption.WhereNotEqualsEnum;
import com.janknspank.database.QueryOption.WhereNotEqualsNumber;
import com.janknspank.database.QueryOption.WhereNotLike;
import com.janknspank.database.QueryOption.WhereNotNull;
import com.janknspank.database.QueryOption.WhereNull;
import com.janknspank.database.QueryOption.WhereOption;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

/**
 * An implementation of the Collection abstract class that enables gets,
 * deletes, updates, and inserts against a Mongo DB database.
 */
public class MongoCollection<T extends Message> extends Collection<T> {
  private static final Logger LOG = new Logger(MongoCollection.class);
  private MongoClient __clientInternal = null; // DO NOT USE DIRECTLY!!
  private DB __database = null; // DO NOT USE DIRECTLY!!

  protected MongoCollection(Class<T> clazz) {
    super(clazz);
  }

  protected MongoClient getClient() throws DatabaseSchemaException {
    if (__clientInternal == null) {
      __clientInternal = MongoConnection.getClient();
      __database = MongoConnection.getDatabase();
    }
    return __clientInternal;
  }

  protected DB getDatabase() throws DatabaseSchemaException {
    getClient();
    return __database;
  }

  @VisibleForTesting
  static List<String> getIndexes(Iterable<FieldDescriptor> fields)
      throws DatabaseSchemaException {
    List<String> indexes = Lists.newArrayList();
    for (FieldDescriptor field : fields) {
      StorageMethod storageMethod = field.getOptions().getExtension(ExtensionsProto.storageMethod);
      if (storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX) {
        switch (field.getJavaType()) {
          case STRING:
          case LONG:
          case INT:
          case DOUBLE:
          case FLOAT:
            indexes.add(field.getName());
            break;
          default:
            throw new DatabaseSchemaException("Field type cannot be indexed: "
                + field.getJavaType() + " (in " + field.getFullName() + ")");
        }
      }
      if (field.getJavaType() == JavaType.MESSAGE) {
        for (String subindex : getIndexes(field.getMessageType().getFields())) {
          indexes.add(field.getName() + "." + subindex);
        }
      }
    }
    return indexes;
  }

  @Override
  public void createTable() throws DatabaseSchemaException {
    DBCollection collection = getDatabase().getCollection(getTableName());
    if (collection == null) {
      collection = getDatabase().createCollection(getTableName(), new BasicDBObject());
    }
    for (String index : getIndexes(storageMethodMap.keySet())) {
      collection.createIndex(new BasicDBObject(index, 1));
    }
  }

  /**
   * Returns a primary key-adjusted field name for the specified query
   * option.
   */
  private String getFieldName(QueryOption option) {
    String fieldName;
    if (option instanceof WhereOption) {
      fieldName = ((WhereOption) option).getFieldName();
    } else if (option instanceof QueryOption.Sort) {
      fieldName = ((QueryOption.Sort) option).getFieldName();
    } else {
      throw new IllegalStateException("QueryOption has no field name");
    }
    return (fieldName.equals(this.primaryKeyField)) ? "_id" : fieldName;
  }

  /**
   * TODO(jonemerson): This should throw DatabaseRequestExceptions if any
   * options don't match up to the schema.
   */
  BasicDBObject getQueryObject(QueryOption... options) {
    BasicDBObject dbObject = new BasicDBObject();
    for (WhereOption whereEquals :
        Iterables.concat(
            QueryOption.getList(options, WhereEquals.class),
            QueryOption.getList(options, WhereEqualsEnum.class),
            QueryOption.getList(options, WhereEqualsNumber.class))) {
      int size = whereEquals.getFieldCount();
      if (size == 0 &&
          (whereEquals instanceof WhereEquals || whereEquals instanceof WhereEqualsNumber)) {
        throw new IllegalStateException("Where clause contains no values - "
            + "This should have been caught earlier.");
      }
      if (size == 0 &&
          (whereEquals instanceof WhereNotEquals || whereEquals instanceof WhereNotEqualsNumber)) {
        // OK, don't write anything - Everything doesn't equal nothing.
        continue;
      }
      String fieldName = getFieldName(whereEquals);
      if (size == 1) {
        Object value;
        if (whereEquals instanceof WhereEquals) {
          value = Iterables.getFirst(((WhereEquals) whereEquals).getValues(), null);
        } else if (whereEquals instanceof WhereEqualsEnum) {
          Enum<?> e = Iterables.getFirst(((WhereEqualsEnum) whereEquals).getValues(), null);
          value = e.name();
        } else if (whereEquals instanceof WhereEqualsNumber) {
          value = Iterables.getFirst(((WhereEqualsNumber) whereEquals).getValues(), null);
        } else {
          throw new IllegalStateException();
        }
        if ("_id".equals(fieldName)) {
          value = new ObjectId((String) value);
        }
        if (whereEquals instanceof WhereEqualsIgnoreCase) {
          dbObject.put(fieldName, Pattern.compile((String) value, Pattern.CASE_INSENSITIVE));
        } else if (whereEquals instanceof WhereNotEquals
            || whereEquals instanceof WhereNotEqualsEnum
            || whereEquals instanceof WhereNotEqualsNumber) {
          dbObject.put(fieldName, new BasicDBObject("$ne", value));
        } else {
          dbObject.put(fieldName, value);
        }
      } else {
        if (whereEquals instanceof WhereEqualsNumber) {
          if (whereEquals instanceof WhereNotEqualsNumber) {
            for (Number value : ((WhereNotEqualsNumber) whereEquals).getValues()) {
              dbObject.put(fieldName, new BasicDBObject("$ne", value));
            }
          } else {
            BasicDBList or = new BasicDBList();
            for (Number value : ((WhereEqualsNumber) whereEquals).getValues()) {
              or.add(new BasicDBObject(fieldName, value));
            }
            dbObject.put("$or", or);
          }
        } else if (whereEquals instanceof WhereEqualsEnum) {
          if (whereEquals instanceof WhereNotEqualsEnum) {
            for (Enum<?> value : ((WhereNotEqualsEnum) whereEquals).getValues()) {
              dbObject.put(fieldName, new BasicDBObject("$ne", value.name()));
            }
          } else {
            BasicDBList or = new BasicDBList();
            for (Enum<?> value : ((WhereEqualsEnum) whereEquals).getValues()) {
              or.add(new BasicDBObject(fieldName, value.name()));
            }
            dbObject.put("$or", or);
          }
        } else if (whereEquals instanceof WhereNotEquals) {
          for (String value : ((WhereNotEquals) whereEquals).getValues()) {
            dbObject.put(fieldName, new BasicDBObject("$ne",
                ("_id".equals(fieldName)) ? new ObjectId((String) value) : value));
          }
        } else {
          BasicDBList or = new BasicDBList();
          for (String value : ((WhereEquals) whereEquals).getValues()) {
            if (whereEquals instanceof WhereEqualsIgnoreCase) {
              or.add(new BasicDBObject(fieldName, Pattern.compile(value, Pattern.CASE_INSENSITIVE)));
            } else if ("_id".equals(fieldName)) {
              or.add(new BasicDBObject(fieldName, new ObjectId((String) value)));
            } else {
              or.add(new BasicDBObject(fieldName, value));
            }
          }
          dbObject.put("$or", or);
        }
      }
    }
    for (WhereLike whereLike :
        QueryOption.getList(options, WhereLike.class)) {
      String fieldName = getFieldName(whereLike);
      String escapedValue = Pattern.quote(whereLike.getValue()).substring(2);
      escapedValue = escapedValue.substring(0, escapedValue.length() - 2);
      if (whereLike instanceof WhereNotLike) {
        if (StringUtils.countMatches(escapedValue, "%") != 1 && !escapedValue.endsWith("%")) {
          // If this is important, we can probably think about solutions...
          // (The main issue is that MongoDB doesn't allow $nots on $regex's,
          // though it does allow $nots on /forward-slash/ regexs, but
          // unfortunately there's no way to do the latter with the Java API.)
          throw new IllegalStateException("Due to MongoDB Java library limitations, we "
              + "currently don't support WhereNotLike for strings except when the "
              + "wildcard is at the end of the match.");
        }
        dbObject.put(fieldName,
            Pattern.compile("(?!" + escapedValue.substring(0, escapedValue.length() - 1) + ").*"));
      } else {
        dbObject.put(fieldName, Pattern.compile(escapedValue.replaceAll("%", ".*")));
      }
    }
    for (WhereNull whereNull :
        QueryOption.getList(options, WhereNull.class)) {
      String fieldName = getFieldName(whereNull);
      dbObject.put(fieldName,
          new BasicDBObject("$exists", whereNull instanceof WhereNotNull));
    }
    return dbObject;
  }

  private BasicDBObject getSortObject(QueryOption[] options) {
    BasicDBObject dbObject = new BasicDBObject();
    for (QueryOption.Sort sort : QueryOption.getList(options, QueryOption.Sort.class)) {
      dbObject.put(getFieldName(sort), (sort instanceof QueryOption.AscendingSort) ? 1 : -1);
    }
    return dbObject;
  }

  @Override
  public Iterable<T> get(QueryOption... options) throws DatabaseSchemaException {
    if (QueryOption.isWhereClauseEmpty(options)) {
      return ImmutableList.of();
    }

    DBCursor cursor = null;
    try {
      BasicDBObject query = getQueryObject(options);
      BasicDBObject sort = getSortObject(options);
      cursor = getDatabase().getCollection(this.getTableName()).find(query).sort(sort);

      List<QueryOption.Limit> queryOptionList = QueryOption.getList(options, QueryOption.Limit.class);
      if (queryOptionList.size() > 1) {
        throw new DatabaseSchemaException("Duplicate definitions of QueryOption.Limit not allowed");
      }
      if (!queryOptionList.isEmpty()) {
        cursor.limit(queryOptionList.get(0).getLimit());
        if (queryOptionList.get(0) instanceof LimitWithOffset) {
          cursor.skip(((LimitWithOffset) queryOptionList.get(0)).getOffset());
        }
      }

      return Mongoizer.fromCursor(cursor, this.clazz);

    } finally {
      IOUtils.closeQuietly(cursor);
    }
  }

  @Override
  public int delete(QueryOption... options) throws DatabaseSchemaException {
    if (QueryOption.isWhereClauseEmpty(options)) {
      return 0;
    }

    List<QueryOption.Limit> queryOptionList = QueryOption.getList(options, QueryOption.Limit.class);
    if (queryOptionList.size() > 1) {
      throw new IllegalStateException("Duplicate definitions of QueryOption.Limit not allowed");
    }
    int rows = 0;
    if (!queryOptionList.isEmpty()) {
      // This is fugly but it (hopefully) does the job.  A more MongoDB-
      // native implementation would probably be better long-term, but
      // do note that Mongo doesn't have the best Java API for removes.
      for (T t : get(options)) {
        try {
          rows += getDatabase().getCollection(this.getTableName())
              .remove(Mongoizer.toDBObject(t))
              .getN();
        } catch (DatabaseSchemaException e) {
          LOG.warning(e.getMessage());
        }
      }
    } else {
      rows += getDatabase().getCollection(this.getTableName())
          .remove(getQueryObject(options))
          .getN();
    }
    return rows;
  }

  @Override
  public int insert(Iterable<T> messages)
      throws DatabaseSchemaException, DatabaseRequestException {
    if (Iterables.isEmpty(messages)) {
      return 0;
    }
    List<DBObject> dbObjectList = Mongoizer.toDBObjectList(messages);

    // Do some logging, so devs can see something happening.
    StringBuilder logText = new StringBuilder();
    logText.append("Insert: " + clazz.getSimpleName() + " (id=" +
        dbObjectList.get(0).get("_id") + ")");
    if (dbObjectList.size() > 1) {
      logText.append(" and " + (dbObjectList.size() - 1) + " more items");
    }
    LOG.fine(logText.toString());

    return getDatabase().getCollection(this.getTableName())
        .insert(dbObjectList)
        .getN();
  }

  @Override
  public int update(Iterable<T> messages, WhereOption... whereOptions)
      throws DatabaseSchemaException, DatabaseRequestException {
    int rows = 0;
    BasicDBObject queryDbObject = getQueryObject(whereOptions);
    for (T t : messages) {
      String primaryKey = Database.getPrimaryKey(t);
      LOG.fine("Update: " + clazz.getSimpleName() + " (id=" + primaryKey + ")");
      queryDbObject.put("_id", new ObjectId(primaryKey));
      rows += getDatabase().getCollection(this.getTableName())
          .update(queryDbObject, Mongoizer.toDBObject(t))
          .getN();
    }
    return rows;
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
    validateType(field, value);

    BasicDBObject queryDbObject = getQueryObject(
        new WhereEquals("_id", Database.getPrimaryKey(message)));
    if (getDatabase().getCollection(this.getTableName())
        .update(queryDbObject, new BasicDBObject("$set",
            new BasicDBObject(fieldName, value instanceof Message ?
                Mongoizer.toDBObject(Validator.assertValid((Message) value)) : value)))
        .getN() == 0) {
      throw new DatabaseSchemaException("Object not found: " + classAndField
          + " (id=" + Database.getPrimaryKey(message) + ")");
    }

    Message.Builder messageBuilder = message.toBuilder();
    messageBuilder.setField(field, value);
    return (T) messageBuilder.build();
  }

  @SuppressWarnings("unchecked")
  private T setIterable(T message, String fieldName, Iterable<Object> values)
      throws DatabaseSchemaException, DatabaseRequestException {
    String classAndField = message.getClass().getSimpleName() + "." + fieldName;
    FieldDescriptor field = Database.getFieldDescriptor(message.getClass(), fieldName);
    validateType(field, values);

    BasicDBObject queryDbObject = getQueryObject(
        new WhereEquals("_id", Database.getPrimaryKey(message)));
    if (getDatabase().getCollection(this.getTableName())
        .update(queryDbObject, new BasicDBObject("$set", 
            new BasicDBObject(fieldName, Mongoizer.toDBList(values))))
        .getN() == 0) {
      throw new DatabaseSchemaException("Object not found: " + classAndField
          + " (id=" + Database.getPrimaryKey(message) + ")");
    }

    Message.Builder messageBuilder = message.toBuilder();
    messageBuilder.clearField(field);
    for (Object o : values) {
      messageBuilder.addRepeatedField(field, o);
    }
    return (T) messageBuilder.build();
  }

  @Override
  public <U extends Object> void push(T message, String fieldName, Iterable<U> values)
      throws DatabaseSchemaException, DatabaseRequestException {
    String classAndField = message.getClass().getSimpleName() + "." + fieldName;
    Collection.validateType(Database.getFieldDescriptor(message.getClass(), fieldName), values);

    BasicDBObject queryDbObject = getQueryObject(
        new WhereEquals("_id", Database.getPrimaryKey(message)));
    if (getDatabase().getCollection(this.getTableName())
        .update(queryDbObject, new BasicDBObject("$push", 
            new BasicDBObject(fieldName, new BasicDBObject("$each", Mongoizer.toDBList(values)))))
        .getN() == 0) {
      throw new DatabaseSchemaException("Object not found: " + classAndField
          + " (id=" + Database.getPrimaryKey(message) + ")");
    }
  }

  @Override
  public String toString() {
    try {
      return getTableName() + "=" + storageMethodMap.toString();
    } catch (DatabaseSchemaException e) {
      return super.toString();
    }
  }
}
