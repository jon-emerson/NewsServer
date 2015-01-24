package com.janknspank.proto;

import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;

/**
 * Converts a protocol buffer object to a MongoDB document.
 */
public class Mongoizer {
//  public static <T extends Message> BasicDBList toDBList(Iterable<T> messages) {
//    JSONArray a = new JSONArray();
//    for (Message message : messages) {
//      a.put(toJSON(message));
//    }
//    return a;
//  }
//
//  public static JSONObject toJSON(Message message) {
//    JSONObject o = new JSONObject();
//    for (FieldDescriptor fieldDescriptor : message.getDescriptorForType().getFields()) {
//      ClientSerialization serialization =
//          fieldDescriptor.getOptions().getExtension(Extensions.clientSerialization);
//
//      // Omit unset fields and fields we've been requested to not serialize.
//      if (serialization == ClientSerialization.EXCLUDE ||
//          (fieldDescriptor.isRepeated() && message.getRepeatedFieldCount(fieldDescriptor) == 0) ||
//          (!fieldDescriptor.isRepeated() && !message.hasField(fieldDescriptor))) {
//        continue;
//      }
//
//      // Validation.
//      if (serialization == ClientSerialization.INCLUDE_AS_NUMBER &&
//          fieldDescriptor.getJavaType() != JavaType.LONG &&
//          fieldDescriptor.getJavaType() != JavaType.INT) {
//        throw new RuntimeException("Cannot include a <" + fieldDescriptor.getJavaType().name()
//            + "> type as number");
//      }
//
//      String fieldName = fieldDescriptor.getName();
//      if (fieldDescriptor.isRepeated()) {
//        JSONArray jsonArray = new JSONArray();
//        JavaType javaType = fieldDescriptor.getJavaType();
//        for (int i = 0; i < message.getRepeatedFieldCount(fieldDescriptor); i++) {
//          switch (javaType) {
//            case STRING:
//              jsonArray.put((String) message.getRepeatedField(fieldDescriptor, i));
//              break;
//
//            case LONG:
//              if (serialization == ClientSerialization.INCLUDE_AS_NUMBER) {
//                jsonArray.put((long) message.getRepeatedField(fieldDescriptor, i));
//              } else {
//                jsonArray.put(Long.toString((long) message.getRepeatedField(fieldDescriptor, i)));
//              }
//              break;
//
//            case INT:
//              jsonArray.put((int) message.getRepeatedField(fieldDescriptor, i));
//              break;
//
//            case ENUM:
//              EnumValueDescriptor v =
//                  (EnumValueDescriptor) message.getRepeatedField(fieldDescriptor, i);
//              jsonArray.put(v.getName());
//              break;
//
//            case MESSAGE:
//              jsonArray.put(toJSON((Message) message.getRepeatedField(fieldDescriptor, i)));
//              break;
//
//            default:
//              throw new RuntimeException("Unsupported type: " + fieldDescriptor.getJavaType().name());
//          }
//        }
//        o.put(fieldName, jsonArray);
//      } else {
//        switch (fieldDescriptor.getJavaType()) {
//          case STRING:
//            o.put(fieldName, (String) message.getField(fieldDescriptor));
//            break;
//
//          case LONG:
//            if (serialization == ClientSerialization.INCLUDE_AS_NUMBER) {
//              o.put(fieldName, (long) message.getField(fieldDescriptor));
//            } else {
//              o.put(fieldName, Long.toString((long) message.getField(fieldDescriptor)));
//            }
//            break;
//
//          case INT:
//            o.put(fieldName, (int) message.getField(fieldDescriptor));
//            break;
//
//          case ENUM:
//            EnumValueDescriptor v = (EnumValueDescriptor) message.getField(fieldDescriptor);
//            o.put(fieldName, v.getName());
//            break;
//
//          case MESSAGE:
//            o.put(fieldName, toJSON((Message) message.getField(fieldDescriptor)));
//            break;
//
//          default:
//            throw new RuntimeException("Unsupported type: " + fieldDescriptor.getJavaType().name());
//        }
//      }
//    }
//    return o;
//  }
}
