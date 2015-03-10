package com.janknspank.bizness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.janknspank.proto.UserProto.User;
import com.janknspank.server.RequestException;

public class SessionsTest {
  /**
   * Makes sure we can decrypt a session key.
   */
  @Test
  public void testLinkedInOAuthState() throws Exception {
    User user = User.newBuilder()
        .setId(GuidFactory.generate())
        .setCreateTime(System.currentTimeMillis())
        .setFirstName("Jorge")
        .setLastName("Pasilda")
        .setEmail("jpasilda@spotternews.com")
        .build();
    String sessionKey = Sessions.createSessionKey(user);
    assertEquals(user.getId(), Sessions.getUserId(sessionKey));

    try {
      Sessions.getUserId("suck it trebek");
    } catch (RequestException e) {
      return; // Success!
    }
    fail("getUserId should have failed on invalid input");
  }
}
