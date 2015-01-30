package com.janknspank.proto;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.janknspank.common.AssertionException;
import com.janknspank.common.Asserts;
import com.janknspank.database.ExtensionsProto;
import com.janknspank.database.ExtensionsProto.ClientSerialization;
import com.janknspank.database.ExtensionsProto.Required;
import com.janknspank.database.ExtensionsProto.StorageMethod;

/**
 * Prints a protocol buffer's values.
 */
public class Printer {
  public static void print(Message message) throws AssertionException {
    Asserts.assertNotNull(message, "Message cannot be null");
    for (FieldDescriptor fieldDescriptor : message.getDescriptorForType().getFields()) {
      System.out.print(fieldDescriptor.getName() + " = ");

      switch (fieldDescriptor.getJavaType()) {
        case STRING:
          System.out.print("\"" + message.getField(fieldDescriptor) + "\"");
          break;

        case LONG:
          System.out.print(message.getField(fieldDescriptor) + "L");
          break;

        case INT:
          System.out.print(message.getField(fieldDescriptor));
          break;

        default:
          throw new RuntimeException("Unsupported type: " + fieldDescriptor.getJavaType().name());
      }

      List<String> decorations = Lists.newArrayList();
      if (fieldDescriptor.getOptions().getExtension(ExtensionsProto.required) == Required.YES) {
        decorations.add("required");
      }
      StorageMethod storageMethod =
          fieldDescriptor.getOptions().getExtension(ExtensionsProto.storageMethod);
      if (storageMethod != StorageMethod.STANDARD) {
        decorations.add(storageMethod.name().toLowerCase());
      }
      if (fieldDescriptor.getOptions().getExtension(ExtensionsProto.clientSerialization)
          == ClientSerialization.EXCLUDE) {
        decorations.add("exclude_from_client");
      }
      if (!decorations.isEmpty()) {
        System.out.print(" (" + Joiner.on(", ").join(decorations) + ")");
      }
      System.out.println();
    }
  }
}
