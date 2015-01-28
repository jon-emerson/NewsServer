package com.janknspank.data;

import org.bson.types.ObjectId;

public class GuidFactory {
  public static final int GUID_SIZE = 24;

  /**
   * Returns a string suitable for object identifying.  In the Mongo DB
   * object ID implementation used here, the return value is guaranteed to be
   * 24 hex characters.
   */
  public static String generate() {
    return ObjectId.get().toHexString();
  }
}
