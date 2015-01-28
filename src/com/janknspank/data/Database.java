package com.janknspank.data;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.janknspank.proto.Extensions;
import com.janknspank.proto.Extensions.StorageMethod;
import com.janknspank.proto.Local.LongAbstract;
import com.janknspank.proto.Local.TokenToEntity;

public class Database {
  @SuppressWarnings("rawtypes")
  private static final LoadingCache<Class, Collection> CACHED_COLLECTIONS =
      CacheBuilder.newBuilder()
          .maximumSize(100)
          .expireAfterWrite(1000, TimeUnit.DAYS)
          .build(
              new CacheLoader<Class, Collection>() {
                @SuppressWarnings("unchecked")
                public Collection load(Class clazz) {
                  if (clazz.equals(TokenToEntity.class) ||
                      clazz.equals(LongAbstract.class)) {
                    return new LocalSqlCollection(clazz);
                  } else {
                    // return new MongoCollection(clazz);
                    return new SqlCollection(clazz);
                  }
                }
              });

  @SuppressWarnings("unchecked")
  public static <T extends Message> Collection<T> with(Class<T> clazz) {
    try {
      return (Collection<T>) CACHED_COLLECTIONS.get(clazz);
    } catch (ExecutionException e) {
      Throwables.propagateIfInstanceOf(e.getCause(), RuntimeException.class);
      throw new RuntimeException(e.getCause());
    }
  }

  /**
   * Returns the value of the passed {@code Message}'s primary key.
   */
  static String getPrimaryKey(Message message) {
    Map<FieldDescriptor, StorageMethod> fieldMap = with(message.getClass()).storageMethodMap;
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
  static <T extends Message> Iterable<String> getPrimaryKeys(Iterable<T> messages) {
    List<String> primaryKeys = Lists.newArrayList();
    T firstMessage = Iterables.getFirst(messages, null);
    for (T message : messages) {
      if (!firstMessage.getClass().equals(message.getClass())) {
        throw new IllegalStateException("Types do not match");
      }
      primaryKeys.add(getPrimaryKey(message));
    }
    return primaryKeys;
  }

  /**
   * Returns the maximum allowed string length for the specified field in the
   * passed Message subclass.
   */
  public static <T extends Message> int getStringLength(Class<T> clazz, String fieldName) {
    for (FieldDescriptor field : getDefaultInstance(clazz).getDescriptorForType().getFields()) {
      if (JavaType.STRING == field.getJavaType()) {
        if (fieldName.equals(field.getName())) {
          return field.getOptions().getExtension(Extensions.stringLength);
        }
      }
    }
    throw new IllegalStateException("Could not find length of " + clazz.getSimpleName()
        + "." + fieldName + " field");
  }

  // TODO(jonemerson): Make this package-private someday.
  @SuppressWarnings("unchecked")
  public static <T extends Message> T getDefaultInstance(Class<T> clazz) {
    try {
      return (T) clazz.getMethod("getDefaultInstance").invoke(null);
    } catch (IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException("Could not reflect on Message type: " + e.getMessage(), e);
    }
  }

  /**
   * Updates the instance of the passed message using the values in {@code
   * message}, overwriting whatever was stored in the database before.  Returns
   * false if no object was updated (since it doesn't yet exist).
   */
  public static boolean update(Message message) throws ValidationException, DataInternalException {
    return update(ImmutableList.of(message)) == 1;
  }

  public static boolean update(Message message, QueryOption.WhereOption... whereOptions)
      throws ValidationException, DataInternalException {
    return update(ImmutableList.of(message), whereOptions) == 1;
  }

  /**
   * Updates the passed messages, overwriting whatever was stored in the
   * database before.  Returns the number of modified objects.
   */
  public static <T extends Message> int update(
      Iterable<T> messages, QueryOption.WhereOption... whereOptions)
      throws ValidationException, DataInternalException {
    if (Iterables.isEmpty(messages)) {
      return 0;
    }
    @SuppressWarnings("unchecked")
    Collection<T> collection =
        (Collection<T>) with(Iterables.getFirst(messages, null).getClass());
    return collection.update(messages);
  }

  /**
   * Inserts the passed message into the database.
   */
  public static void insert(Message message) throws ValidationException, DataInternalException {
    insert(ImmutableList.of(message));
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
    @SuppressWarnings("unchecked")
    Collection<T> collection =
        (Collection<T>) with(Iterables.getFirst(messages, null).getClass());
    return collection.insert(messages);
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
   * Deletes the passed object from the database.
   * @throws DataInternalException if the object could not be deleted, including
   *     if it could not be found
   */
  public static <T extends Message> void delete(Iterable<T> messages)
      throws DataInternalException {
    if (!Iterables.isEmpty(messages)) {
      with(Iterables.getFirst(messages, null).getClass())
          .delete(getPrimaryKeys(messages));
    }
  }

  /**
   * Deletes the passed object from the database.
   * @throws DataInternalException if the object could not be deleted, including
   *     if it could not be found
   */
  public static void delete(Message message) throws DataInternalException {
    delete(ImmutableList.of(message));
  }
}
