package com.janknspank.server;

import org.junit.Test;
import static org.junit.Assert.fail;

import com.janknspank.bizness.BiznessException;

public class LinkedInLoginServletTest {
  @Test
  public void testLinkedInOAuthState() throws Exception {
    String oAuthState = LinkedInLoginServlet.getLinkedInOAuthState();
    LinkedInLoginServlet.verifyLinkedInOAuthState(oAuthState);

    try {
      LinkedInLoginServlet.verifyLinkedInOAuthState("suck it trebek");
    } catch (BiznessException e) {
      return; // Success!
    }
    fail("verifyLinkedInOAuthState should have failed on invalid input");
  }
}
