package com.janknspank.proto;

import org.json.JSONObject;

import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.janknspank.proto.Core.ClientSerialization;

/**
 * Converts a protocol buffer object to JSON by using the ClientSerialization
 * instructions embedded in the .proto definition's extensions.
 */
public class Serializer {
  public static JSONObject toJSON(Message message) {
    JSONObject o = new JSONObject();
    for (FieldDescriptor fieldDescriptor : message.getDescriptorForType().getFields()) {
      ClientSerialization serialization =
          fieldDescriptor.getOptions().getExtension(Core.clientSerialization);
      if (serialization == ClientSerialization.INCLUDE ||
          serialization == ClientSerialization.INCLUDE_AS_NUMBER) {
        String fieldName = fieldDescriptor.getName();
        switch (fieldDescriptor.getJavaType()) {
          case STRING:
            if (serialization == ClientSerialization.INCLUDE_AS_NUMBER) {
              throw new RuntimeException("Cannot include a <string> type as number");
            }
            o.put(fieldName, (String) message.getField(fieldDescriptor));
            break;

          case LONG:
            if (serialization == ClientSerialization.INCLUDE_AS_NUMBER) {
              o.put(fieldName, (long) message.getField(fieldDescriptor));
            } else {
              o.put(fieldName, Long.toString((long) message.getField(fieldDescriptor)));
            }
            break;

          case INT:
            o.put(fieldName, (int) message.getField(fieldDescriptor));
            break;

          default:
            throw new RuntimeException("Unsupported type: " + fieldDescriptor.getJavaType().name());
        }
      }
    }
    return o;
  }
}
