package com.janknspank.server;

import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.Users;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.User;

/**
 * Servlet for handling unsubscribes.
 */
@ServletMapping(urlPattern = "/unsubscribe")
public class UnsubscribeServlet extends StandardServlet {
  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, RedirectException {
    if (!"yes".equals(getParameter(req, "confirm"))) {
      User user = WelcomeEmailServlet.getValidatedUser(
          getRequiredParameter(req, "email"), getRequiredParameter(req, "schmutz"));
      if (user.getOptOutEmail()) {
        throw new RedirectException(WelcomeEmailServlet.getSubscribeLink(
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
          "unsubscribeLink",
              new URIBuilder(WelcomeEmailServlet.getUnsubscribeLink(user, true /* relativeUrl */))
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
          .setOptOutEmail(true)
          .build();
      Database.update(user);
    }

    try {
      throw new RedirectException(
          new URIBuilder(WelcomeEmailServlet.getUnsubscribeLink(user, true /* relativeUrl */))
              .setParameter("confirm", "yes")
              .toString());
    } catch (URISyntaxException e) {
      throw new BiznessException("Error forming URI", e);
    }
  }

  public static void main(String args[]) throws DatabaseRequestException, DatabaseSchemaException {
    if (args.length == 0) {
      System.out.println("Specify email address please!");
      return;
    }
    User user = Users.getByEmail(args[0]);
    if (user == null) {
      System.out.println("User not found: \"" + args[0] + "\"");
      return;
    }
    if (user.getOptOutEmail()) {
      System.out.println("User has already opted out.");
      return;
    }
    if (Database.update(user.toBuilder().setOptOutEmail(true).build())) {
      System.out.println("Success");
    } else {
      System.out.println("Failure");
    }
  }
}
