package com.janknspank.data;

import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.janknspank.data.QueryOption.LimitWithOffset;
import com.janknspank.data.QueryOption.WhereEquals;
import com.janknspank.data.QueryOption.WhereEqualsIgnoreCase;
import com.janknspank.data.QueryOption.WhereLike;
import com.janknspank.data.QueryOption.WhereNotEquals;
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
    for (WhereEquals whereEquals :
        QueryOption.getList(options, WhereEquals.class)) {
      int size = Iterables.size(whereEquals.getValues());
      if (size == 0) {
        if (whereEquals instanceof WhereNotEquals) {
          // OK, don't write anything - Everything doesn't equal nothing.
          continue;
        }
        throw new IllegalStateException("Where clause contains no values - "
            + "This should have been caught earlier.");
      }

      String fieldName = getFieldName(whereEquals);
      if (size == 1) {
        String value = Iterables.getFirst(whereEquals.getValues(), null);
        if (whereEquals instanceof WhereEqualsIgnoreCase) {
          dbObject.put(fieldName, Pattern.compile(value, Pattern.CASE_INSENSITIVE));
        } else if (whereEquals instanceof WhereNotEquals) {
          dbObject.put(fieldName, new BasicDBObject("$ne", value));
        } else {
          dbObject.put(fieldName, value);
        }
      } else {
        BasicDBList or = new BasicDBList();
        for (String value : whereEquals.getValues()) {
          if (whereEquals instanceof WhereEqualsIgnoreCase) {
            or.put(fieldName, Pattern.compile(value, Pattern.CASE_INSENSITIVE));
          } else if (whereEquals instanceof WhereNotEquals) {
            or.put(fieldName, new BasicDBObject("$ne", value));
          } else {
            or.put(fieldName, value);
          }
        }
      }
    }
    for (WhereLike whereLike :
        QueryOption.getList(options, WhereLike.class)) {
      String fieldName = getFieldName(whereLike);
      Pattern pattern = Pattern.compile(whereLike.getValue().replaceAll("%", ".*"));
      if (whereLike instanceof WhereNotLike) {
        dbObject.put(fieldName, new BasicDBObject("$not", pattern));
      } else {
        dbObject.put(fieldName, pattern);
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
      dbObject.put(getFieldName(sort), sort instanceof QueryOption.AscendingSort);
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
    return getDatabase().getCollection(this.getTableName())
        .insert(Mongoizer.toDBObjectList(messages))
        .getN();
  }

  @Override
  public int update(Iterable<T> messages, WhereOption... whereOptions)
      throws ValidationException, DataInternalException {
    int rows = 0;
    for (T t : messages) {
      rows += getDatabase().getCollection(this.getTableName())
          .update(getQueryObject(whereOptions), Mongoizer.toDBObject(t))
          .getN();
    }
    return rows;
  }

}
