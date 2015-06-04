package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

/**
 * This is the old path for getting the beta build, back before we launched.
 * Now we use the same build for beta versions, so it's been renamed to
 * "beta".
 */
@ServletMapping(urlPattern = "/demo")
public class DemoServlet extends StandardServlet {
  @Override
  public JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RedirectException {
    throw new RedirectException("/beta");
  }
}
