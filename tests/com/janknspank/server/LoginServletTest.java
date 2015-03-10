package com.janknspank.server;

import org.junit.Test;
import static org.junit.Assert.fail;

import com.janknspank.bizness.BiznessException;

public class LoginServletTest {
  @Test
  public void testLinkedInOAuthState() throws Exception {
    String oAuthState = LoginServlet.getLinkedInOAuthState();
    LoginServlet.verifyLinkedInOAuthState(oAuthState);

    try {
      LoginServlet.verifyLinkedInOAuthState("suck it trebek");
    } catch (BiznessException e) {
      return; // Success!
    }
    fail("verifyLinkedInOAuthState should have failed on invalid input");
  }
}
