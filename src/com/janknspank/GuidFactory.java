package com.janknspank;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

public class GuidFactory {
  /**
   * Returns a random base-64 string.  The returned string can be up to 22
   * characters long.
   */
  public static String generate() {
    UUID uuid = UUID.randomUUID();
    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.putLong(uuid.getLeastSignificantBits());
    bb.putLong(uuid.getMostSignificantBits());
    return Base64.encodeBase64URLSafeString(bb.array()).replaceAll("=", "");
  }
}
