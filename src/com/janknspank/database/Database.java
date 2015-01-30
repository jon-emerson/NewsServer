package com.janknspank.database;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.janknspank.common.Asserts;
import com.janknspank.database.ExtensionsProto.StorageMethod;
import com.janknspank.proto.LocalProto.LongAbstract;
import com.janknspank.proto.LocalProto.TokenToEntity;

public class Database {
  @SuppressWarnings("rawtypes")
  private static final LoadingCache<Class, Collection> CACHED_COLLECTIONS =
      CacheBuilder.newBuilder()
          .maximumSize(100)
          .expireAfterWrite(1000, TimeUnit.DAYS)
          .build(
              new CacheLoader<Class, Collection>() {
                @SuppressWarnings("unchecked")
                public Collection load(Class clazz) throws ExecutionException {
                  try {
                    return getCollectionForClass(clazz);
                  } catch (DatabaseSchemaException e) {
                    throw new ExecutionException(e);
                  }
                }
              });

  @SuppressWarnings("unchecked")
  public static <T extends Message> Collection<T> with(Class<T> clazz)
      throws DatabaseSchemaException {
    try {
      return (Collection<T>) CACHED_COLLECTIONS.get(clazz);
    } catch (ExecutionException e) {
      Throwables.propagateIfInstanceOf(e.getCause(), RuntimeException.class);
      Throwables.propagateIfInstanceOf(e.getCause(), DatabaseSchemaException.class);
      throw new RuntimeException(e.getCause());
    }
  }

  /**
   * Returns the value of the passed {@code Message}'s primary key.
   */
  static String getPrimaryKey(Message message) throws DatabaseSchemaException {
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
  static <T extends Message> Iterable<String> getPrimaryKeys(Iterable<T> messages)
      throws DatabaseSchemaException {
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
          return field.getOptions().getExtension(ExtensionsProto.stringLength);
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
  public static boolean update(Message message)
      throws DatabaseRequestException, DatabaseSchemaException {
    return update(ImmutableList.of(message)) == 1;
  }

  public static boolean update(Message message, QueryOption.WhereOption... whereOptions)
      throws DatabaseRequestException, DatabaseSchemaException {
    return update(ImmutableList.of(message), whereOptions) == 1;
  }

  /**
   * Updates the passed messages, overwriting whatever was stored in the
   * database before.  Returns the number of modified objects.
   */
  public static <T extends Message> int update(
      Iterable<T> messages, QueryOption.WhereOption... whereOptions)
      throws DatabaseRequestException, DatabaseSchemaException {
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
  public static void insert(Message message)
      throws DatabaseRequestException, DatabaseSchemaException {
    insert(ImmutableList.of(message));
  }

  /**
   * Inserts the passed messages into the database.  All messages passed must be
   * of the same type.
   */
  public static <T extends Message> int insert(Iterable<T> messages)
      throws DatabaseRequestException, DatabaseSchemaException {
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
  public static void upsert(Message message)
      throws DatabaseRequestException, DatabaseSchemaException {
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
      throws DatabaseSchemaException {
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
  public static void delete(Message message) throws DatabaseSchemaException {
    delete(ImmutableList.of(message));
  }

  private static <T extends Message> String getDatabaseCollectionSpec(Class<T> clazz)
      throws DatabaseSchemaException {
    String databaseCollectionSpecification =
        getDefaultInstance(clazz).getDescriptorForType().getOptions().getExtension(
            ExtensionsProto.databaseCollection);
    Asserts.assertNonEmpty(databaseCollectionSpecification,
        "No table defined for object: " + clazz.getSimpleName()
        + ". It probably is a child of a larger object - "
        + "you should use it that way instead.",
        DatabaseSchemaException.class);
    Asserts.assertTrue(
        databaseCollectionSpecification.startsWith("MongoDB.") ||
        databaseCollectionSpecification.startsWith("MySQL."),
        "Database collection type must be either MongoDB or MySQL",
        DatabaseSchemaException.class);
    return databaseCollectionSpecification;
  }

  public static <T extends Message> Collection<T> getCollectionForClass(Class<T> clazz)
      throws DatabaseSchemaException {
    if (clazz.equals(TokenToEntity.class) ||
        clazz.equals(LongAbstract.class)) {
      return new LocalSqlCollection<T>(clazz);
    } else if (getDatabaseCollectionSpec(clazz).startsWith(("MySQL."))) {
      return new SqlCollection<T>(clazz);
    } else {
      return new MongoCollection<T>(clazz);
    }
  }

  static <T extends Message> String getTableName(Class<T> clazz)
      throws DatabaseSchemaException {
    String databaseCollectionSpecification = getDatabaseCollectionSpec(clazz);
    return databaseCollectionSpecification.substring(
        databaseCollectionSpecification.indexOf(".") + 1);
  }

  /**
   * Sets a specific repeated subfield in this collection to a single value.
   * The value can be a primitive, an Iterable, or a Message.  Returns the
   * modified message.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Message> T set(T message, String fieldName, Object value)
      throws DatabaseSchemaException, DatabaseRequestException {
    return ((Collection<T>) with(message.getClass())).set(message, fieldName, value);
  }

  /**
   * Pushes values into an embedded array within the passed message, with the 
   * array field specifed by {@code fieldName}.
   * TODO(jonemerson): Return the modified message.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Message> void push(T message, String fieldName, List<Object> values)
      throws DatabaseSchemaException, DatabaseRequestException {
    ((Collection<T>) with(message.getClass())).push(message, fieldName, values);
  }

  /**
   * Gets the FieldDescriptor for the specified field, if one exists.  Supports
   * dot-notation to retrieve nested fields.
   * TODO(jonemerson): It probably makes more sense to cache a Map of field
   * names to FielDescriptors on the Collection objects.  Doing this for every
   * set seems like a bad idea, long-term.
   */
  static <T extends Message> FieldDescriptor getFieldDescriptor(Class<T> clazz, String fieldName) {
    Iterable<String> tokens = Splitter.on(".").limit(2).split(fieldName);
    String firstToken = Iterables.getFirst(tokens, null);
    T defaultInstance = getDefaultInstance(clazz);
    for (FieldDescriptor field : defaultInstance.getDescriptorForType().getFields()) {
      if (field.getName().equals(firstToken)) {
        if (Iterables.size(tokens) == 1) {
          return field;
        } else {
          return getFieldDescriptor(
              defaultInstance.toBuilder().newBuilderForField(field).build().getClass(),
              Iterables.get(tokens, 1));
        }
      }
    }
    return null;
  }

  /**
   * Performs type checking to validate that the passed value is actually
   * correct for placement in the passed {@code fieldName}.
   */
  static <T extends Message, U extends Object> void assertObjectsValidForField(
      Class<T> clazz, String fieldName, List<U> values) throws DatabaseRequestException {
    for (Object value : values) {
      assertObjectValidForField(clazz, fieldName, value);
    }
  }

  /**
   * Performs type checking to validate that the passed value is actually
   * correct for placement in the passed {@code fieldName}.
   */
  static <T extends Message> void assertObjectValidForField(
      Class<T> clazz, String fieldName, Object value) throws DatabaseRequestException {
    String classPlusField = clazz.getSimpleName() + "." + fieldName;
    FieldDescriptor field = getFieldDescriptor(clazz, fieldName);
    Asserts.assertNotNull(field, "Field not found: " + classPlusField,
        DatabaseRequestException.class);
    switch (field.getJavaType()) {
      case STRING:
        Asserts.assertTrue(value instanceof String, classPlusField + " must be a String",
            DatabaseRequestException.class);
        int stringLength = field.getOptions().getExtension(ExtensionsProto.stringLength);
        Asserts.assertTrue(((String) value).length() <= stringLength,
            "Max string length for " + classPlusField + " is " + stringLength + " characters",
            DatabaseRequestException.class);
        StorageMethod storageMethod = field.getOptions().getExtension(ExtensionsProto.storageMethod);
        if (storageMethod == StorageMethod.PRIMARY_KEY) {
          Asserts.assertTrue(((String) value).length() == stringLength,
              "Primary key " + classPlusField + " must be " + stringLength + " characters",
              DatabaseRequestException.class);
        }
        break;
      case INT:
        Asserts.assertTrue(value instanceof Integer, classPlusField + " must be an Integer",
            DatabaseRequestException.class);
        break;
      case LONG:
        Asserts.assertTrue(value instanceof Long, classPlusField + " must be a Long",
            DatabaseRequestException.class);
        break;
      case ENUM:
        Asserts.assertTrue(value instanceof Enum<?>, classPlusField + " must be an enum",
            DatabaseRequestException.class);
        break;
      case BOOLEAN:
        Asserts.assertTrue(value instanceof Boolean, classPlusField + " must be a Boolean",
            DatabaseRequestException.class);
        break;
      case DOUBLE:
        Asserts.assertTrue(value instanceof Double, classPlusField + " must be a Double",
            DatabaseRequestException.class);
        break;
      case MESSAGE:
        Asserts.assertTrue(value instanceof Message, classPlusField + " must be a Message",
            DatabaseRequestException.class);
        Descriptor expectedDescriptor = ((Message) value).getDescriptorForType();
        Asserts.assertTrue(field.getMessageType().equals(expectedDescriptor),
            "Message field types must match: Found " + field.getFullName() + ", but found "
                + expectedDescriptor.getFullName(),
            DatabaseRequestException.class);
        break;
      default:
        throw new IllegalStateException("Unsupported type: " + field.getJavaType().name());
    }
  }
}
