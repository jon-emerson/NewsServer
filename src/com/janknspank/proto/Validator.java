package com.janknspank.proto;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.janknspank.common.Asserts;
import com.janknspank.data.GuidFactory;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Extensions.Required;
import com.janknspank.proto.Extensions.StorageMethod;
import com.janknspank.proto.Extensions.StringCharset;

/**
 * Validates a protocol buffer object by using the Required instructions
 * embedded in the .proto definition's extensions.
 */
public class Validator {
  public static Message assertValid(Message message) throws ValidationException {
    return assertValid(message, true /* indexedFieldsAllowed */);
  }

  private static Message assertValid(Message message, boolean indexedFieldsAllowed)
      throws ValidationException {
    Asserts.assertNotNull(message, "Message cannot be null");

    boolean foundPrimaryKey = false;
    for (FieldDescriptor fieldDescriptor : message.getDescriptorForType().getFields()) {
      String fieldName = message.getClass().getSimpleName() + "." + fieldDescriptor.getName();

      // Verify required fields are all set, and required repeateds are non-empty.
      if (fieldDescriptor.getOptions().getExtension(Extensions.required) == Required.YES) {
        if (fieldDescriptor.isRepeated()) {
          Asserts.assertTrue(message.getRepeatedFieldCount(fieldDescriptor) > 0,
              "Required repeated field " + fieldName + " must be non-empty");
        } else {
          Asserts.assertTrue(message.hasField(fieldDescriptor),
              fieldName + " cannot be null");
        }
      }

      // Verify only valid fields are indexed.
      StorageMethod storageMethod =
          fieldDescriptor.getOptions().getExtension(Extensions.storageMethod);
      if (storageMethod != StorageMethod.STANDARD && storageMethod != StorageMethod.DO_NOT_STORE) {
        int stringLength = fieldDescriptor.getOptions().getExtension(Extensions.stringLength);
        int stringBytes = fieldDescriptor.getOptions().getExtension(Extensions.stringCharset)
            == StringCharset.UTF8 ? stringLength * 2 : stringLength;
        Asserts.assertTrue(stringBytes <= 767 || storageMethod == StorageMethod.PULL_OUT,
            "Error on " + fieldName + ": Strings larger than 767 bytes cannot be indexed");

        // This seems like a good idea for now, but if we ever re-use protos to
        // de-reference look-ups, we'll probably want to remove this (and that's
        // OK).
        Asserts.assertTrue(indexedFieldsAllowed, "Error in " + message.getClass().getSimpleName()
            + ": Indexed fields are not allowed in nested protos");
      }
      
      // Verify string length is valid.
      int stringLength = fieldDescriptor.getOptions().getExtension(Extensions.stringLength);
      if (fieldDescriptor.getJavaType() == JavaType.STRING) {
        Asserts.assertTrue(stringLength> 0,
            "String field " + fieldName + " must have non-zero string_length annotation");
      } else {
        Asserts.assertTrue(
            !fieldDescriptor.getOptions().hasExtension(Extensions.stringLength),
            "Error in " + fieldName + " definition: Non-strings can't have string_length "
                + "declarations");
      }

      // Verify that there's only one primary key, and that it's a 24-character
      // string.  (This requirement allows us to use MongoDB's _id field for our
      // primary keys, which is indexed by hash.)
      if (storageMethod == StorageMethod.PRIMARY_KEY) {
        Asserts.assertTrue(!foundPrimaryKey, "Error in " + message.getClass().getSimpleName()
            + ": Only one primary key per table allowed");
        Asserts.assertTrue(stringLength == GuidFactory.GUID_SIZE,
            "Primary keys must be strings of size " + GuidFactory.GUID_SIZE);
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
                throw new ValidationException("Field " + fieldName + " can have a maximum "
                    + "length of " + stringLength + ". Actual length is " + fieldLength + ".");
              }
            }
          } else {
            int fieldLength = ((String) message.getField(fieldDescriptor)).length();
            if (fieldLength > stringLength) {
              throw new ValidationException("Field " + fieldName + " can have a maximum "
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
          throw new RuntimeException("Unsupported type: " + fieldDescriptor.getJavaType().name());
      }
    }
    return message;
  }
}
