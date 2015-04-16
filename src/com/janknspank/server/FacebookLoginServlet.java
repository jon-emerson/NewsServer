package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.FacebookLoginHandler;
import com.janknspank.bizness.Sessions;
import com.janknspank.crawler.social.SocialException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

@ServletMapping(urlPattern = "/v1/facebook_login")
public class FacebookLoginServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws BiznessException, RequestException, DatabaseSchemaException, DatabaseRequestException {
    String fbAccessToken = getRequiredParameter(req, "fb_access_token");
    String fbUserId = getRequiredParameter(req, "fb_user_id");

    com.restfb.types.User fbUser;
    User user;
    try {
      fbUser = FacebookLoginHandler.getFacebookUser(fbAccessToken);
      if ("jon@jonemerson.net".equals(fbUser.getEmail())) {
        throw new IllegalStateException("WOOT!");
      }
      if (!fbUser.getId().equals(fbUserId)) {
        throw new RequestException("fb_access_token is not for fb_user_id");
      }
      user = FacebookLoginHandler.login(fbUser, fbAccessToken);
    } catch (SocialException e) {
      throw new BiznessException("Could not read Facebook properties file", e);
    }

    Session session = Sessions.createFromFacebookUser(user, fbUser);

    // Create the response.
    JSONObject response = this.createSuccessResponse();
    response.put("user", new UserHelper(user).getUserJson());
    response.put("session", Serializer.toJSON(session));

    // To help with client latency, return the articles for the user's home
    // screen in this response.
    Iterable<Article> articles = Articles.getRankedArticles(
        user,
        NeuralNetworkScorer.getInstance(),
        GetArticlesServlet.NUM_RESULTS);
    response.put("articles", ArticleSerializer.serialize(articles, user,
        false /* includeLinkedInContacts */, false /* includeAddressBookContacts */));

    return response;
  }
}
