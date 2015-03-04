package com.janknspank.database;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.janknspank.common.Asserts;
import com.janknspank.database.ExtensionsProto.Required;
import com.janknspank.database.ExtensionsProto.StorageMethod;
import com.janknspank.database.ExtensionsProto.StringCharset;

/**
 * Validates a protocol buffer object by using the Required instructions
 * embedded in the .proto definition's extensions.
 */
public class Validator {
  public static Message assertValid(Message message)
      throws DatabaseRequestException, DatabaseSchemaException {
    return assertValid(message, true /* indexedFieldsAllowed */);
  }

  private static Message assertValid(Message message, boolean indexedFieldsAllowed)
      throws DatabaseRequestException, DatabaseSchemaException {
    Asserts.assertNotNull(message, "Message cannot be null", DatabaseRequestException.class);

    boolean foundPrimaryKey = false;
    for (FieldDescriptor fieldDescriptor : message.getDescriptorForType().getFields()) {
      String fieldName = message.getClass().getSimpleName() + "." + fieldDescriptor.getName();

      // Verify required fields are all set, and required repeateds are non-empty.
      if (fieldDescriptor.getOptions().getExtension(ExtensionsProto.required) == Required.YES) {
        if (fieldDescriptor.isRepeated()) {
          Asserts.assertTrue(message.getRepeatedFieldCount(fieldDescriptor) > 0,
              "Required repeated field " + fieldName + " must be non-empty",
              DatabaseRequestException.class);
        } else {
          Asserts.assertTrue(message.hasField(fieldDescriptor),
              fieldName + " cannot be null", DatabaseRequestException.class);
        }
      } else if (!fieldDescriptor.isRepeated() && !message.hasField(fieldDescriptor)) {
        // Optional field that's unset - totally cromulant!  Let's keep going!
        continue;
      }

      // Verify only valid fields are indexed.
      StorageMethod storageMethod =
          fieldDescriptor.getOptions().getExtension(ExtensionsProto.storageMethod);
      if (storageMethod != StorageMethod.STANDARD && storageMethod != StorageMethod.DO_NOT_STORE) {
        int stringLength = fieldDescriptor.getOptions().getExtension(ExtensionsProto.stringLength);
        int stringBytes = fieldDescriptor.getOptions().getExtension(ExtensionsProto.stringCharset)
            == StringCharset.UTF8 ? stringLength * 2 : stringLength;
        Asserts.assertTrue(stringBytes <= 767 || storageMethod == StorageMethod.PULL_OUT,
            "Error on " + fieldName + ": Strings larger than 767 bytes cannot be indexed",
            DatabaseSchemaException.class);
      }

      // Verify string length is valid.
      int stringLength = fieldDescriptor.getOptions().getExtension(ExtensionsProto.stringLength);
      if (fieldDescriptor.getJavaType() == JavaType.STRING) {
        Asserts.assertTrue(stringLength> 0, "String field " + fieldName
            + " must have non-zero string_length annotation", DatabaseSchemaException.class);
      } else {
        Asserts.assertTrue(
            !fieldDescriptor.getOptions().hasExtension(ExtensionsProto.stringLength),
            "Error in " + fieldName + " definition: Non-strings can't have string_length "
                + "declarations", DatabaseSchemaException.class);
      }

      // Verify that there's only one primary key, and that it's a 24-character
      // string.  (This requirement allows us to use MongoDB's _id field for our
      // primary keys, which is indexed by hash.)
      if (storageMethod == StorageMethod.PRIMARY_KEY) {
        Asserts.assertTrue(!foundPrimaryKey, "Error in " + message.getClass().getSimpleName()
            + ": Only one primary key per table allowed", DatabaseSchemaException.class);
//        Asserts.assertTrue(stringLength == GuidFactory.GUID_SIZE,
//            "Primary keys must be strings of size " + GuidFactory.GUID_SIZE);
        foundPrimaryKey = true;
      }

      // Verify we support all the specified types, and that strings are within
      // their length limits.
      switch (fieldDescriptor.getJavaType()) {
        case STRING:
          if (fieldDescriptor.isRepeated()) {
            for (int i = 0; i < message.getRepeatedFieldCount(fieldDescriptor); i++) {
              int fieldLength = ((String) message.getRepeatedField(fieldDescriptor, i)).length();
              if (fieldLength > stringLength) {
                throw new DatabaseRequestException("Field " + fieldName + " can have a maximum "
                    + "length of " + stringLength + ". Actual length is " + fieldLength + ".");
              }
            }
          } else {
            int fieldLength = ((String) message.getField(fieldDescriptor)).length();
            if (fieldLength > stringLength) {
              throw new DatabaseRequestException("Field " + fieldName + " can have a maximum "
                  + "length of " + stringLength + ". Actual length is " + fieldLength + ".");
            }
          }
          break;
        case MESSAGE:
          if (fieldDescriptor.isRepeated()) {
            for (int i = 0; i < message.getRepeatedFieldCount(fieldDescriptor); i++) {
              assertValid((Message) message.getRepeatedField(fieldDescriptor, i),
                  false /* indexedFieldsAllowed */);
            }
          } else {
            assertValid((Message) message.getField(fieldDescriptor),
                false /* indexedFieldsAllowed */);
          }
          break;
        case LONG:
        case INT:
        case ENUM:
        case BOOLEAN:
        case DOUBLE:
          break;
        default:
          throw new DatabaseSchemaException(
              "Unsupported type: " + fieldDescriptor.getJavaType().name());
      }
    }
    return message;
  }
}
