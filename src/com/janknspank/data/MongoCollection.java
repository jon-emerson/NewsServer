package com.janknspank.data;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.janknspank.data.QueryOption.LimitWithOffset;
import com.janknspank.data.QueryOption.WhereEquals;
import com.janknspank.data.QueryOption.WhereEqualsIgnoreCase;
import com.janknspank.data.QueryOption.WhereEqualsNumber;
import com.janknspank.data.QueryOption.WhereLike;
import com.janknspank.data.QueryOption.WhereNotEquals;
import com.janknspank.data.QueryOption.WhereNotEqualsNumber;
import com.janknspank.data.QueryOption.WhereNotLike;
import com.janknspank.data.QueryOption.WhereNotNull;
import com.janknspank.data.QueryOption.WhereNull;
import com.janknspank.data.QueryOption.WhereOption;
import com.janknspank.proto.Extensions.StorageMethod;
import com.janknspank.proto.Mongoizer;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

public class MongoCollection<T extends Message> extends Collection<T> {
  private MongoClient __clientInternal = null; // DO NOT USE DIRECTLY!!
  private DB __database = null; // DO NOT USE DIRECTLY!!

  protected MongoCollection(Class<T> clazz) {
    super(clazz);
  }

  protected MongoClient getClient() throws DataInternalException {
    if (__clientInternal == null) {
      __clientInternal = MongoConnection.getClient();
      __database = MongoConnection.getDatabase();
    }
    return __clientInternal;
  }

  protected DB getDatabase() throws DataInternalException {
    getClient();
    return __database;
  }

  @Override
  public void createTable() throws DataInternalException {
    DBCollection collection =
        getDatabase().createCollection(getTableName(), new BasicDBObject());
    for (FieldDescriptor field : storageMethodMap.keySet()) {
      StorageMethod storageMethod = storageMethodMap.get(field);
      if (storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX) {
        collection.createIndex(new BasicDBObject(field.getName(), 1));
      }
    }
  }

  /**
   * Returns a primary key-adjusted field name for the specified query
   * option.
   */
  private String getFieldName(QueryOption option) {
    String fieldName;
    if (option instanceof WhereOption){
      fieldName = ((WhereOption) option).getFieldName();
    } else if (option instanceof QueryOption.Sort) {
      fieldName = ((QueryOption.Sort) option).getFieldName();
    } else {
      throw new IllegalStateException("QueryOption has no field name");
    }
    return (fieldName.equals(this.primaryKeyField)) ? "_id" : fieldName;
  }

  private BasicDBObject getQueryObject(QueryOption[] options) {
    BasicDBObject dbObject = new BasicDBObject();
    for (WhereOption whereEquals :
        Iterables.concat(
            QueryOption.getList(options, WhereEquals.class),
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
        Object value = Iterables.getFirst(whereEquals instanceof WhereEquals ?
            ((WhereEquals) whereEquals).getValues() : ((WhereEqualsNumber) whereEquals).getValues(), null);
        if ("_id".equals(fieldName)) {
          value = new ObjectId((String) value);
        }
        if (whereEquals instanceof WhereEqualsIgnoreCase) {
          dbObject.put(fieldName, Pattern.compile((String) value, Pattern.CASE_INSENSITIVE));
        } else if (whereEquals instanceof WhereNotEquals || whereEquals instanceof WhereNotEqualsNumber) {
          dbObject.put(fieldName, new BasicDBObject("$ne",
              fieldName.equals("_id") ? value : value));
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
  public Iterable<T> get(QueryOption... options) throws DataInternalException {
    if (QueryOption.isWhereClauseEmpty(options)) {
      return ImmutableList.of();
    }

    DBCursor cursor = null;
    try {
      cursor = getDatabase().getCollection(this.getTableName())
          .find(getQueryObject(options))
          .sort(getSortObject(options));

      List<QueryOption.Limit> queryOptionList = QueryOption.getList(options, QueryOption.Limit.class);
      if (queryOptionList.size() > 1) {
        throw new IllegalStateException("Duplicate definitions of QueryOption.Limit not allowed");
      }
      if (!queryOptionList.isEmpty()) {
        cursor.limit(queryOptionList.get(0).getLimit());
        if (queryOptionList.get(0) instanceof LimitWithOffset) {
          cursor.skip(((LimitWithOffset) queryOptionList.get(0)).getOffset());
        }
      }

      return Mongoizer.fromCursor(cursor, this.clazz);

    } catch (ValidationException e) {
      throw new DataInternalException("Could not understand database object: " + e.getMessage(), e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  @Override
  public int delete(QueryOption... options) throws DataInternalException {
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
        } catch (ValidationException e) {
          System.err.println("Warning: " + e.getMessage());
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
  public int insert(Iterable<T> messages) throws ValidationException,
      DataInternalException {
    if (Iterables.isEmpty(messages)) {
      return 0;
    }
    System.out.println("Insert: " + Iterables.getFirst(messages, null) + " ... and "
        + (Iterables.size(messages) - 1) + " more " + clazz.getSimpleName() + " items");
    return getDatabase().getCollection(this.getTableName())
        .insert(Mongoizer.toDBObjectList(messages))
        .getN();
  }

  @Override
  public int update(Iterable<T> messages, WhereOption... whereOptions)
      throws ValidationException, DataInternalException {
    int rows = 0;
    BasicDBObject queryDbObject = getQueryObject(whereOptions);
    for (T t : messages) {
      System.out.println("Update: " + t);
      queryDbObject.put("_id", new ObjectId(Database.getPrimaryKey(t)));
      rows += getDatabase().getCollection(this.getTableName())
          .update(queryDbObject, Mongoizer.toDBObject(t))
          .getN();
    }
    return rows;
  }

}
