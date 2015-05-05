package com.janknspank.server;

import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.User;

/**
 * Servlet for handling subscribes.
 */
@ServletMapping(urlPattern = "/subscribe")
public class SubscribeServlet extends StandardServlet {
  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, RedirectException {
    if (!"yes".equals(getParameter(req, "confirm"))) {
      User user = WelcomeEmailServlet.getValidatedUser(
          getRequiredParameter(req, "email"), getRequiredParameter(req, "schmutz"));
      if (!user.getOptOutEmail()) {
        throw new RedirectException(WelcomeEmailServlet.getUnsubscribeLink(
            user, true /* relativeUrl */));
      }
    }
    return null;
  }

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws BiznessException, RequestException, DatabaseSchemaException {
    User user = WelcomeEmailServlet.getValidatedUser(
        getRequiredParameter(req, "email"), getRequiredParameter(req, "schmutz"));
    try {
      return new SoyMapData(
          "confirmed", "yes".equals(getParameter(req, "confirm")),
          "subscribeLink",
              new URIBuilder(WelcomeEmailServlet.getSubscribeLink(user, true /* relativeUrl */))
                  .setParameter("confirm", "yes")
                  .toString());
    } catch (URISyntaxException e) {
      throw new BiznessException("Error forming URI", e);
    }
  }

  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RedirectException, RequestException, DatabaseSchemaException,
          DatabaseRequestException, BiznessException {
    User user = WelcomeEmailServlet.getValidatedUser(
        getRequiredParameter(req, "email"), getRequiredParameter(req, "schmutz"));

    boolean confirmed = "yes".equals(getParameter(req, "confirm"));
    if (confirmed) {
      user = user.toBuilder()
          .setOptOutEmail(false)
          .build();
      Database.update(user);
    }

    try {
      throw new RedirectException(
          new URIBuilder(WelcomeEmailServlet.getSubscribeLink(user, true /* relativeUrl */))
              .setParameter("confirm", "yes")
              .toString());
    } catch (URISyntaxException e) {
      throw new BiznessException("Error forming URI", e);
    }
  }
}
