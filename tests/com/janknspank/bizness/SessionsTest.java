package com.janknspank.bizness;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SessionsTest {
  /**
   * Makes sure we can decrypt a session key.
   */
  @Test
  public void testDecrypt() throws Exception {
    assertEquals("54cd3623e4b0161a5806d30c", Sessions.decrypt(
        "JX0_gDDyDHIH1NML0z0tksSgMZzqKLxCltrNwPe7aLlZi-RnmNOZAdNARop58LzV"));
  }
}
