package com.janknspank.proto;

import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.janknspank.Asserts;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Required;
import com.janknspank.proto.Core.Url;

/**
 * Validates a protocol buffer object by using the Required instructions
 * embedded in the .proto definition's extensions.
 */
public class Validator {
  public static Message assertValid(Message message) throws ValidationException {
    Asserts.assertNotNull(message, "Message cannot be null");

    // HACK(jonemerson): Let's figure out who's not cleaning their URLs.
    if (message instanceof Article) {
      if (((Article) message).getUrl().contains("?") &&
          (((Article) message).getUrl().contains("ns_campaign") ||
              ((Article) message).getUrl().contains("utm_"))) {
        throw new ValidationException("Article.url contains bad query parameter - " +
            "is this intentional? " + ((Article) message).getUrl());
      }
    }
    if (message instanceof Url) {
      if (((Url) message).getUrl().contains("?") &&
          (((Url) message).getUrl().contains("ns_campaign") ||
              ((Url) message).getUrl().contains("utm_"))) {
        throw new ValidationException("Url.url contains bad query parameter - " +
            "is this intentional? " + ((Url) message).getUrl());
      }
    }

    for (FieldDescriptor fieldDescriptor : message.getDescriptorForType().getFields()) {
      if (fieldDescriptor.getOptions().getExtension(Core.required) == Required.YES) {
        Asserts.assertTrue(message.hasField(fieldDescriptor),
            fieldDescriptor.getName() + " cannot be null");
        switch (fieldDescriptor.getJavaType()) {
          case STRING:
            int stringLength = ((String) message.getField(fieldDescriptor)).length();
            int maxLength = fieldDescriptor.getOptions().getExtension(Core.stringLength);
            if (stringLength > maxLength) {
              throw new ValidationException("Field " + fieldDescriptor.getName() + " can have a " +
                  "maximum length of " + maxLength + ". Actual length is " + stringLength + ".");
            }
          case LONG:
          case INT:
            break;
          default:
            throw new RuntimeException("Unsupported type: " + fieldDescriptor.getJavaType().name());
        }
      }
    }
    return message;
  }
}
