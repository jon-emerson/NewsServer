package com.janknspank.proto;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.janknspank.common.Asserts;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Extensions.Required;
import com.janknspank.proto.Extensions.StorageMethod;

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
        int maxLength = fieldDescriptor.getOptions().getExtension(Extensions.stringLength);
        Asserts.assertTrue(maxLength <= 767 || storageMethod == StorageMethod.PULL_OUT,
            "Strings larger than 767 characters cannot be indexed");

        // This seems like a good idea for now, but if we ever re-use protos to
        // de-reference look-ups, we'll probably want to remove this (and that's
        // OK).
        Asserts.assertTrue(indexedFieldsAllowed, "Indexed fields are not allowed in nested protos");
      }

      // Verify we support all the specified types, and that strings are within
      // their length limits.
      switch (fieldDescriptor.getJavaType()) {
        case STRING:
          int maxLength = fieldDescriptor.getOptions().getExtension(Extensions.stringLength);
          if (maxLength <= 0) {
            throw new IllegalStateException(
                "String field " + fieldName + " must have non-zero string_length annotation");
          }
          if (fieldDescriptor.isRepeated()) {
            for (int i = 0; i < message.getRepeatedFieldCount(fieldDescriptor); i++) {
              int stringLength = ((String) message.getRepeatedField(fieldDescriptor, i)).length();
              if (stringLength > maxLength) {
                throw new ValidationException("Field " + fieldName + " can have a maximum "
                    + "length of " + maxLength + ". Actual length is " + stringLength + ".");
              }
            }
          } else {
            int stringLength = ((String) message.getField(fieldDescriptor)).length();
            if (stringLength > maxLength) {
              throw new ValidationException("Field " + fieldName + " can have a maximum "
                  + "length of " + maxLength + ". Actual length is " + stringLength + ".");
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
