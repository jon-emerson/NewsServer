package com.janknspank.database;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.janknspank.database.ExtensionsProto.StorageMethod;

/**
 * A collection is a table of data of the same data type.  In SQL instances,
 * a collection is stored as a table.  In MongoDB instances, a collection is
 * stored as a collection.
 * 
 * A collection object allows operations against objects of a specific type,
 * such as queries, inserts, updates, and deletes.
 * @see Collection#of(Class<Message>)
 * @see Collection#get(QueryOption...)
 */
public abstract class Collection<T extends Message> {
  protected final Class<T> clazz;
  protected final ImmutableMap<FieldDescriptor, StorageMethod> storageMethodMap;
  protected final String primaryKeyField;

  protected Collection(Class<T> clazz) {
    this.clazz = clazz;
    this.storageMethodMap = generateStorageMethodMap();
    this.primaryKeyField = generatePrimaryKeyField();
  }

  /**S
   * Returns a map of protocol buffer field descriptors to their StorageMethod
   * types, as defined in our protocol buffer extensions.
   */
  private ImmutableMap<FieldDescriptor, StorageMethod>
      generateStorageMethodMap() {
    ImmutableMap.Builder<FieldDescriptor, StorageMethod> storageMethodMapBuilder =
        ImmutableMap.builder();

    int primaryKeyCount = 0;
    Message message = Database.getDefaultInstance(clazz);
    for (FieldDescriptor field : message.getDescriptorForType().getFields()) {
      StorageMethod storageMethod = field.getOptions().getExtension(ExtensionsProto.storageMethod);
      switch (storageMethod) {
        case PRIMARY_KEY:
          primaryKeyCount++;
          storageMethodMapBuilder.put(field, storageMethod);
          break;

        case STANDARD:
        case INDEX:
        case UNIQUE_INDEX:
        case PULL_OUT:
          storageMethodMapBuilder.put(field, storageMethod);
          break;

        default:
          throw new IllegalStateException("Unsupported storage method: " + storageMethod.name());
      }
    }

    // Validate proto message definition.
    if (primaryKeyCount > 1) {
      throw new IllegalStateException("Tables with multiple primary keys are not yet supported");
    }

    return storageMethodMapBuilder.build();
  }

  /**
   * Returns the value of the passed {@code Message}'s primary key.
   */
  private String generatePrimaryKeyField() {
    for (FieldDescriptor field : storageMethodMap.keySet()) {
      if (storageMethodMap.get(field) == StorageMethod.PRIMARY_KEY) {
        return field.getName();
      }
    }
    return null;
  }

  /**
   * Removes the values of any Message fields annotated with StorageMethod:
   * DO_NOT_STORE.  This method should be called on Messages before writing them
   * to the database.
   */
  protected static Message cleanDoNotStoreFields(Message message) {
    Message.Builder messageBuilder = message.toBuilder();
    for (FieldDescriptor field : message.getAllFields().keySet()) {
      StorageMethod storageMethod = field.getOptions().getExtension(ExtensionsProto.storageMethod);
      if (storageMethod == StorageMethod.DO_NOT_STORE) {
        messageBuilder.clearField(field);
      } else if (field.getJavaType() == JavaType.MESSAGE) {
        if (field.isRepeated()) {
          for (int i = 0; i < message.getRepeatedFieldCount(field); i++) {
            messageBuilder.setRepeatedField(field, i, 
                cleanDoNotStoreFields((Message) message.getRepeatedField(field, i)));
          }
        } else {
          messageBuilder.setField(field,
              cleanDoNotStoreFields((Message) message.getField(field)));
        }
      }
    }
    return messageBuilder.build();
  }

  /**
   * Returns the MySQL table name that Messages of the passed type should be
   * stored in.
   */
  public String getTableName() throws DatabaseSchemaException {
    return Database.getTableName(clazz);
  }

  /**
   * Creates the table for storing objects of the given class {@code clazz}.
   */
  public abstract void createTable() throws DatabaseSchemaException;

  /**
   * Gets the message of the specified class {@code clazz} with the primary
   * key equaling {@code primaryKey}.
   */
  public T get(String primaryKey) throws DatabaseSchemaException {
    return Iterables.getFirst(get(ImmutableList.of(primaryKey)), null);
  }

  /**
   * Gets Messages with the specified class {@code clazz} and the given
   * primary keys {@code primaryKeys}, if they exist.
   */
  public Iterable<T> get(Iterable<String> primaryKeys)
      throws DatabaseSchemaException {
    if (primaryKeyField == null) {
      throw new IllegalStateException(
          "Invalid query: " + clazz.getSimpleName() + " has no primary key");
    }
    return get(new QueryOption.WhereEquals(primaryKeyField, primaryKeys));
  }

  /**
   * Returns the first message with the specified class {@code clazz} and the
   * specified field options, if such a message exists.
   */
  public T getFirst(QueryOption... options) throws DatabaseSchemaException {
    // Make sure we only have one limit statement.  If we find another, capture
    // its offset, so we don't lose it.
    List<QueryOption> goodOptions = Lists.newArrayList();
    Integer offset = null;
    for (QueryOption option : options) {
      if (option instanceof QueryOption.LimitWithOffset) {
        offset = ((QueryOption.LimitWithOffset) option).getOffset();
      } else if (!(option instanceof QueryOption.Limit)) {
        goodOptions.add(option);
      }
    }
    return Iterables.getFirst(
        get(Iterables.toArray(
            Iterables.concat(goodOptions, offset == null ?
                ImmutableList.of(new QueryOption.Limit(1)) :
                ImmutableList.of(new QueryOption.LimitWithOffset(1, offset))),
            QueryOption.class)),
        null);
  }

  /**
   * Gets Messages with the specified class {@code clazz} and the field values,
   * if they exist.
   */
  public abstract Iterable<T> get(QueryOption... options) throws DatabaseSchemaException;

  /**
   * Deletes the object with the specified primary key from the table specified
   * by the passed-in class.
   * @throws DataInternalException if the object could not be deleted, including
   *     if it could not be found
   */
  public boolean delete(String primaryKey) throws DatabaseSchemaException {
    return delete(ImmutableList.of(primaryKey)) == 1;
  }

  /**
   * Deletes objects with the specified primary keys from the table specified
   * by the passed-in class.
   */
  public int delete(Iterable<String> primaryKeys) throws DatabaseSchemaException {
    if (primaryKeyField == null) {
      throw new IllegalStateException(
          "Invalid query: " + clazz.getSimpleName() + " has no primary key");
    }
    return delete(new QueryOption.WhereEquals(primaryKeyField, primaryKeys));
  }

  /**
   * Deletes objects from the specified object {@code clazz} that match the
   * given query options.
   */
  public abstract int delete(QueryOption... options) throws DatabaseSchemaException;

  /**
   * Inserts the passed messages into the database.  All messages passed must be
   * of the same type.
   */
  public abstract int insert(Iterable<T> messages)
      throws DatabaseRequestException, DatabaseSchemaException;

  /**
   * Updates the passed messages, overwriting whatever was stored in the
   * database before.  Returns the number of modified objects.
   */
  public abstract int update(Iterable<T> messages, QueryOption.WhereOption... whereOptions)
      throws DatabaseRequestException, DatabaseSchemaException;

  /**
   * Sets a specific subfield in the specified message to a value.  The value
   * can be a primitive, an Iterable, or a Message.  Returns the modified
   * message.
   */
  public abstract T set(T message, String fieldName, Object value)
      throws DatabaseSchemaException, DatabaseRequestException;

  /**
   * Pushes values into an embedded array within the passed message, with the 
   * array field specifed by {@code fieldName}.
   * TODO(jonerson): Return the modified message.
   */
  public abstract <U extends Object> void push(T message, String fieldName, Iterable<U> values)
      throws DatabaseSchemaException, DatabaseRequestException;
}
