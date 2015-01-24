package com.janknspank.data;

import java.util.List;

import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.janknspank.data.QueryOption.WhereOption;
import com.janknspank.proto.Extensions.StorageMethod;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
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
      if (storageMethod == StorageMethod.PRIMARY_KEY ||
          storageMethod == StorageMethod.INDEX ||
          storageMethod == StorageMethod.UNIQUE_INDEX) {
        collection.createIndex(new BasicDBObject(field.getName(), 1));
      }
    }
  }

  @Override
  public List<T> get(QueryOption... options) throws DataInternalException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int delete(QueryOption... options) throws DataInternalException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int insert(Iterable<T> messages) throws ValidationException,
      DataInternalException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int update(Iterable<T> messages, WhereOption... whereOptions)
      throws ValidationException, DataInternalException {
    // TODO Auto-generated method stub
    return 0;
  }

}
