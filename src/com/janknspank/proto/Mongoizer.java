package com.janknspank.proto;

import java.util.List;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;

import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.janknspank.common.Asserts;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Extensions.Required;
import com.janknspank.proto.Extensions.StorageMethod;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * Converts protocol buffer objects to MongoDB documents, and vice versa.
 */
public class Mongoizer {
  public static <T extends Message> List<T> fromDBList(BasicBSONList list, Class<T> clazz)
      throws ValidationException {
    List<T> messageList = Lists.newArrayList();
    for (int i = 0; i < list.size(); i++) {
      messageList.add(fromDBObject((BasicBSONObject) list.get(i), clazz));
    }
    return messageList;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Message> T fromDBObject(BasicBSONObject object, Class<T> clazz)
      throws ValidationException {
    T defaultInstance = (T) Database.getDefaultInstance(clazz);
    T.Builder messageBuilder = defaultInstance.newBuilderForType();
    for (FieldDescriptor fieldDescriptor : defaultInstance.getDescriptorForType().getFields()) {
      String fieldName = fieldDescriptor.getName();
      JavaType javaType = fieldDescriptor.getJavaType();
      
      // Handle primary keys a little differently - they're stored in "_id".
      StorageMethod storageMethod =
          fieldDescriptor.getOptions().getExtension(Extensions.storageMethod);
      if (storageMethod == StorageMethod.PRIMARY_KEY) {
        Asserts.assertTrue(javaType == JavaType.STRING, "Primary key must be a string");
        messageBuilder.setField(fieldDescriptor, object.getObjectId("_id").toHexString());
        continue;        
      }
      
      // Enforce required fields.
      if (!object.containsField(fieldName)) {
        Required required = fieldDescriptor.getOptions().getExtension(Extensions.required);
        if (required == Required.YES) {
          throw new ValidationException("Required field missing: " + fieldName);
        }
        continue;
      }

      if (fieldDescriptor.isRepeated()) {
        BasicDBList list = (BasicDBList) object.get(fieldName);

        if (javaType == JavaType.MESSAGE) {
          Message.Builder embeddedMessageBuilder =
              messageBuilder.newBuilderForField(fieldDescriptor);
          Class<T> type = (Class<T>) embeddedMessageBuilder.getDefaultInstanceForType().getClass();
          for (T t : fromDBList(list, type)) {
            messageBuilder.addRepeatedField(fieldDescriptor, t);
          }
          continue;
        }

        for (int i = 0; i < list.size(); i++) {
          switch (javaType) {
            case STRING:
              messageBuilder.addRepeatedField(fieldDescriptor, (String) list.get(i));
              break;

            case LONG:
              messageBuilder.addRepeatedField(fieldDescriptor, (Long) list.get(i));
              break;

            case INT:
              messageBuilder.addRepeatedField(fieldDescriptor, (Integer) list.get(i));
              break;

            case ENUM:
              // TODO(jonemerson): Add error handling, in case no value is found.
              messageBuilder.addRepeatedField(fieldDescriptor,
                  fieldDescriptor.getEnumType().findValueByName(object.getString(fieldName)));
              break;

            default:
              throw new ValidationException("Unsupported type: "
                  + fieldDescriptor.getJavaType().name());
          }
        }
        object.put(fieldName, list);
      } else {
        switch (javaType) {
          case STRING:
            messageBuilder.setField(fieldDescriptor, object.getString(fieldName));
            break;

          case LONG:
            messageBuilder.setField(fieldDescriptor, object.getLong(fieldName));
            break;

          case INT:
            messageBuilder.setField(fieldDescriptor, object.getInt(fieldName));
            break;

          case ENUM:
            // TODO(jonemerson): Add error handling, in case no value is found.
            messageBuilder.setField(fieldDescriptor,
                fieldDescriptor.getEnumType().findValueByName(object.getString(fieldName)));
            break;

          case MESSAGE:
            Message.Builder embeddedMessageBuilder =
                messageBuilder.newBuilderForField(fieldDescriptor);
            Class<T> type = (Class<T>) embeddedMessageBuilder.getDefaultInstanceForType().getClass();
            messageBuilder.setField(fieldDescriptor,
                fromDBObject((BasicBSONObject) object.get(fieldName), type));
            break;

          default:
            throw new ValidationException("Unsupported type: "
                + fieldDescriptor.getJavaType().name());
        }
      }
    }
    return (T) messageBuilder.build();
  }
  
  public static <T extends Message> BasicDBList toDBList(Iterable<T> messages)
      throws ValidationException {
    BasicDBList list = new BasicDBList();
    for (Message message : messages) {
      list.add(toDBObject(message));
    }
    return list;
  }

  public static BasicDBObject toDBObject(Message message) throws ValidationException {
    BasicDBObject object = new BasicDBObject();
    for (FieldDescriptor fieldDescriptor : message.getDescriptorForType().getFields()) {
      String fieldName = fieldDescriptor.getName();
      JavaType javaType = fieldDescriptor.getJavaType();
      if (fieldDescriptor.isRepeated() &&
          message.getRepeatedFieldCount(fieldDescriptor) > 0) {
        BasicDBList list = new BasicDBList();
        for (int i = 0; i < message.getRepeatedFieldCount(fieldDescriptor); i++) {
          switch (javaType) {
            case STRING:
            case LONG:
            case INT:
              list.add(message.getRepeatedField(fieldDescriptor, i));
              break;

            case ENUM:
              EnumValueDescriptor v =
                  (EnumValueDescriptor) message.getRepeatedField(fieldDescriptor, i);
              list.add(v.getName());
              break;

            case MESSAGE:
              list.add(toDBObject((Message) message.getRepeatedField(fieldDescriptor, i)));
              break;

            default:
              throw new ValidationException("Unsupported type: "
                  + fieldDescriptor.getJavaType().name());
          }
        }
        object.put(fieldName, list);

      } else if (message.hasField(fieldDescriptor)) {
        StorageMethod storageMethod =
            fieldDescriptor.getOptions().getExtension(Extensions.storageMethod);
        if (storageMethod == StorageMethod.PRIMARY_KEY) {
          Asserts.assertTrue(javaType == JavaType.STRING, "Primary key must be a string");
          object.put("_id", new ObjectId(((String) message.getField(fieldDescriptor))));
          continue;
        }

        switch (javaType) {
          case STRING:
          case LONG:
          case INT:
            object.put(fieldName, message.getField(fieldDescriptor));
            break;

          case ENUM:
            EnumValueDescriptor v = (EnumValueDescriptor) message.getField(fieldDescriptor);
            object.put(fieldName, v.getName());
            break;

          case MESSAGE:
            object.put(fieldName, toDBObject((Message) message.getField(fieldDescriptor)));
            break;

          default:
            throw new ValidationException("Unsupported type: "
                + fieldDescriptor.getJavaType().name());
        }
      }
    }
    return object;
  }
}
