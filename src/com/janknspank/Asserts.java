package com.janknspank;

import com.janknspank.data.ValidationException;

public class Asserts {
  public static void assertNotNull(Object o, String desc) throws ValidationException {
    if (o == null) {
      throw new ValidationException("Object is null: " + desc);
    }
  }

  public static void assertTrue(boolean b, String desc) throws ValidationException {
    if (!b) {
      throw new ValidationException("Condition is false: " + desc);
    }
  }

  public static void assertNonEmpty(String s, String desc) throws ValidationException {
    assertNotNull(s, desc);
    if (s.trim().length() == 0) {
      throw new ValidationException("String is empty: " + desc);
    }
  }
}
