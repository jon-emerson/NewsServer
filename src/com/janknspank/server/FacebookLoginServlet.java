package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.FacebookLoginHandler;
import com.janknspank.bizness.Sessions;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.UserProto.User;

@ServletMapping(urlPattern = "/v1/facebook_login")
public class FacebookLoginServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws BiznessException, RequestException, DatabaseSchemaException, DatabaseRequestException {
    String fbAccessToken = getRequiredParameter(req, "fb_access_token");
    String fbUserId = getRequiredParameter(req, "fb_user_id");

    com.restfb.types.User fbUser = FacebookLoginHandler.getFacebookUser(fbAccessToken);
    if (!fbUser.getId().equals(fbUserId)) {
      throw new RequestException("fb_access_token is not for fb_user_id");
    }
    User user = FacebookLoginHandler.login(fbUser, fbAccessToken);

    long startTime = System.currentTimeMillis();
    Session session = Sessions.createForUser(user);
    System.out.println("Sessions.createForUser(User, User), time = "
        + (System.currentTimeMillis() - startTime) + "ms");

    // Create the response.
    JSONObject response = this.createSuccessResponse();
    response.put("user", new UserHelper(user).getUserJson());
    response.put("session", Serializer.toJSON(session));

    // To help with client latency, return the articles for the user's home
    // screen in this response.
    startTime = System.currentTimeMillis();
    Iterable<Article> articles = Articles.getMainStream(user);
    response.put("articles", ArticleSerializer.serialize(articles, user,
        false /* includeLinkedInContacts */, false /* includeAddressBookContacts */));
    System.out.println("FacebookLoginServlet.doPostInternal, main stream calculation, time = "
        + (System.currentTimeMillis() - startTime) + "ms");

    return response;
  }
}
