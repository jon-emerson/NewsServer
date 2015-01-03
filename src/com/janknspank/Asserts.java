package com.janknspank;

import com.janknspank.data.ValidationException;

public class Asserts {
  public static <T extends Object> T assertNotNull(T o, String desc) throws ValidationException {
    if (o == null) {
      throw new ValidationException("Object is null: " + desc);
    }
    return o;
  }

  public static void assertTrue(boolean b, String desc) throws ValidationException {
    if (!b) {
      throw new ValidationException("Condition is false: " + desc);
    }
  }

  public static String assertNonEmpty(String s, String desc) throws ValidationException {
    assertNotNull(s, desc);
    if (s.trim().length() == 0) {
      throw new ValidationException("String is empty: " + desc);
    }
    return s;
  }
}
