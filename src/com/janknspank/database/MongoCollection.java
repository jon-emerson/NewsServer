package com.janknspank.database;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import com.janknspank.common.Asserts;
import com.janknspank.common.Logger;
import com.janknspank.database.ExtensionsProto.StorageMethod;
import com.janknspank.database.QueryOption.LimitWithOffset;
import com.janknspank.database.QueryOption.WhereEquals;
import com.janknspank.database.QueryOption.WhereEqualsEnum;
import com.janknspank.database.QueryOption.WhereEqualsIgnoreCase;
import com.janknspank.database.QueryOption.WhereEqualsNumber;
import com.janknspank.database.QueryOption.WhereFalse;
import com.janknspank.database.QueryOption.WhereInequality;
import com.janknspank.database.QueryOption.WhereLike;
import com.janknspank.database.QueryOption.WhereNotEquals;
import com.janknspank.database.QueryOption.WhereNotEqualsEnum;
import com.janknspank.database.QueryOption.WhereNotEqualsNumber;
import com.janknspank.database.QueryOption.WhereNotFalse;
import com.janknspank.database.QueryOption.WhereNotLike;
import com.janknspank.database.QueryOption.WhereNotNull;
import com.janknspank.database.QueryOption.WhereNotTrue;
import com.janknspank.database.QueryOption.WhereNull;
import com.janknspank.database.QueryOption.WhereOption;
import com.janknspank.database.QueryOption.WhereTrue;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;

/**
 * An implementation of the Collection abstract class that enables gets,
 * deletes, updates, and inserts against a Mongo DB database.
 */
public class MongoCollection<T extends Message> extends Collection<T> {
  private static final Logger LOG = new Logger(MongoCollection.class);
  private final Set<String> primaryKeyFields;
  private MongoClient __clientInternal = null; // DO NOT USE DIRECTLY!!
  private DB __database = null; // DO NOT USE DIRECTLY!!

  protected MongoCollection(Class<T> clazz) throws DatabaseSchemaException {
    super(clazz);
    primaryKeyFields = getPrimaryKeyFields(storageMethodMap.keySet());
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

  @VisibleForTesting
  static Set<String> getPrimaryKeyFields(Iterable<FieldDescriptor> fields)
      throws DatabaseSchemaException {
    Set<String> primaryKeyFields = Sets.newHashSet();
    for (FieldDescriptor field : fields) {
      StorageMethod storageMethod = field.getOptions().getExtension(ExtensionsProto.storageMethod);
      if (storageMethod == StorageMethod.PRIMARY_KEY) {
        Asserts.assertTrue(field.getJavaType() == JavaType.STRING,
            "Primary keys must be of type string", DatabaseSchemaException.class);
        primaryKeyFields.add(field.getName());
      }
      if (field.getJavaType() == JavaType.MESSAGE) {
        for (String nestedPrimaryKeyField :
            getPrimaryKeyFields(field.getMessageType().getFields())) {
          primaryKeyFields.add(field.getName() + "." + nestedPrimaryKeyField);
        }
      }
    }
    return primaryKeyFields;
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
  @VisibleForTesting
  String getFieldName(QueryOption option) {
    String fieldName;
    if (option instanceof WhereOption) {
      fieldName = ((WhereOption) option).getFieldName();
    } else if (option instanceof QueryOption.Sort) {
      fieldName = ((QueryOption.Sort) option).getFieldName();
    } else {
      throw new IllegalStateException("QueryOption has no field name");
    }

    if (primaryKeyFields.contains(fieldName)) {
      if (fieldName.contains(".")) {
        return fieldName.substring(0, fieldName.lastIndexOf(".") + 1) + "_id";
      }
      return "_id";
    }
    return fieldName;
  }

  /**
   * Returns true if the passed field name represents an ObjectId-typed field.
   */
  private boolean isObjectIdFieldName(String fieldName) {
    // This may be a gross simplification!!  But it seems to work for us in all
    // cases right now...
    return fieldName.equals("_id") || fieldName.endsWith("._id");
  }

  /**
   * TODO(jonemerson): This should throw DatabaseRequestExceptions if any
   * options don't match up to the schema.
   */
  BasicDBObject getQueryObject(QueryOption... options) {
    BasicDBObject dbObject = new BasicDBObject();

    // And together any $or's or other complex queries, so that they don't
    // overwrite each other.
    List<DBObject> orQueriesToAndTogether = Lists.newArrayList();

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
          ProtocolMessageEnum e = Iterables.getFirst(((WhereEqualsEnum) whereEquals).getValues(), null);
          value = e.getValueDescriptor().getName();
        } else if (whereEquals instanceof WhereEqualsNumber) {
          value = Iterables.getFirst(((WhereEqualsNumber) whereEquals).getValues(), null);
        } else {
          throw new IllegalStateException();
        }
        if (isObjectIdFieldName(fieldName)) {
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
            orQueriesToAndTogether.add(or);
          }
        } else if (whereEquals instanceof WhereEqualsEnum) {
          if (whereEquals instanceof WhereNotEqualsEnum) {
            for (ProtocolMessageEnum value : ((WhereNotEqualsEnum) whereEquals).getValues()) {
              dbObject.put(fieldName, new BasicDBObject("$ne", value.getValueDescriptor().getName()));
            }
          } else {
            BasicDBList or = new BasicDBList();
            for (ProtocolMessageEnum value : ((WhereEqualsEnum) whereEquals).getValues()) {
              or.add(new BasicDBObject(fieldName, value.getValueDescriptor().getName()));
            }
            orQueriesToAndTogether.add(or);
          }
        } else if (whereEquals instanceof WhereNotEquals) {
          for (String value : ((WhereNotEquals) whereEquals).getValues()) {
            dbObject.put(fieldName, new BasicDBObject("$ne",
                isObjectIdFieldName(fieldName) ? new ObjectId((String) value) : value));
          }
        } else {
          BasicDBList or = new BasicDBList();
          for (String value : ((WhereEquals) whereEquals).getValues()) {
            if (whereEquals instanceof WhereEqualsIgnoreCase) {
              or.add(new BasicDBObject(fieldName, Pattern.compile(value, Pattern.CASE_INSENSITIVE)));
            } else if (isObjectIdFieldName(fieldName)) {
              or.add(new BasicDBObject(fieldName, new ObjectId((String) value)));
            } else {
              or.add(new BasicDBObject(fieldName, value));
            }
          }
          orQueriesToAndTogether.add(or);
        }
      }
    }

    // Merge all the $or's into an $and with multiple $or children, or just put
    // a single $or onto the query, if that's all we got.
    if (orQueriesToAndTogether.size() == 0) {
      // Do nothing.
    } else if (orQueriesToAndTogether.size() == 1) {
      dbObject.put("$or", orQueriesToAndTogether.get(0));
    } else {
      List<DBObject> and = Lists.newArrayList();
      for (DBObject or : orQueriesToAndTogether) {
        BasicDBObject innerDbObject = new BasicDBObject();
        innerDbObject.put("$or", or);
        and.add(innerDbObject);
      }
      dbObject.put("$and", and);
    }

    for (WhereLike whereLike :
        QueryOption.getList(options, WhereLike.class)) {
      int flags =
          (whereLike instanceof QueryOption.WhereLikeIgnoreCase
              || whereLike instanceof QueryOption.WhereNotLikeIgnoreCase) ?
          Pattern.CASE_INSENSITIVE : 0;
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
            Pattern.compile("(?!" + escapedValue.substring(0, escapedValue.length() - 1) + ").*",
                flags));
      } else {
        dbObject.put(fieldName, Pattern.compile(escapedValue.replaceAll("%", ".*"), flags));
      }
    }
    for (WhereTrue whereTrue : QueryOption.getList(options, WhereTrue.class)) {
      dbObject.put(getFieldName(whereTrue), new BasicDBObject("$eq", true));
    }
    for (WhereNotTrue whereNotTrue : QueryOption.getList(options, WhereNotTrue.class)) {
      dbObject.put(getFieldName(whereNotTrue), new BasicDBObject("$ne", true));
    }
    for (WhereFalse whereFalse : QueryOption.getList(options, WhereFalse.class)) {
      dbObject.put(getFieldName(whereFalse), new BasicDBObject("$eq", false));
    }
    for (WhereNotFalse whereNotFalse : QueryOption.getList(options, WhereNotFalse.class)) {
      dbObject.put(getFieldName(whereNotFalse), new BasicDBObject("$ne", false));
    }
    for (WhereNull whereNull : QueryOption.getList(options, WhereNull.class)) {
      dbObject.put(getFieldName(whereNull),
          new BasicDBObject("$exists", whereNull instanceof WhereNotNull));
    }
    for (WhereInequality whereInequality :
        QueryOption.getList(options, WhereInequality.class)) {
      String fieldName = getFieldName(whereInequality);
      String comparatorStr;
      if (whereInequality instanceof QueryOption.WhereGreaterThan) {
        comparatorStr = "$gt";
      } else if (whereInequality instanceof QueryOption.WhereGreaterThanOrEquals) {
        comparatorStr = "$gte";
      } else if (whereInequality instanceof QueryOption.WhereLessThan) {
        comparatorStr = "$lt";
      } else if (whereInequality instanceof QueryOption.WhereLessThanOrEquals) {
        comparatorStr = "$lte";
      } else {
        throw new IllegalStateException("Unexpected inequality: " + whereInequality.getClass());
      }
      dbObject.put(fieldName, new BasicDBObject(comparatorStr, whereInequality.getValue()));
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

    List<QueryOption.Limit> limitOptions = QueryOption.getList(options, QueryOption.Limit.class);
    if (limitOptions.size() > 1) {
      throw new IllegalStateException("Duplicate definitions of QueryOption.Limit not allowed");
    }
    if (!limitOptions.isEmpty()) {
      // This is fugly but it (hopefully) does the job.  A more MongoDB-
      // native implementation would probably be better long-term, but
      // do note that Mongo doesn't have the best Java API for removes.
      int rows = 0;
      for (T t : get(options)) {
        try {
          rows += getDatabase().getCollection(this.getTableName())
              .remove(Mongoizer.toDBObject(t))
              .getN();
        } catch (DatabaseSchemaException e) {
          LOG.warning(e.getMessage());
        }
      }
      return rows;
    } else {
      BasicDBObject queryObject = getQueryObject(options);
      WriteResult result = getDatabase().getCollection(this.getTableName()).remove(queryObject);
      return result.getN();
    }
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

  /**
   * Returns the number of documents in this collection that match the passed
   * QueryOptions.
   */
  public long getSize(QueryOption.WhereOption... whereOptions) throws DatabaseSchemaException {
    return getDatabase().getCollection(this.getTableName()).count(getQueryObject(whereOptions));
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
