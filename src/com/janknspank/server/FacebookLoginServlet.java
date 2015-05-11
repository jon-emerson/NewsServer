package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.FacebookLoginHandler;
import com.janknspank.bizness.Sessions;
import com.janknspank.bizness.UserInterests;
import com.janknspank.crawler.social.SocialException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.UserProto.User;

@ServletMapping(urlPattern = "/v1/facebook_login")
public class FacebookLoginServlet extends StandardServlet {
  /**
   * There's a weird client bug where if the user gets company interests but
   * not industry interests, they get both a pop-up and the industry chooser.
   * And the app crashes if they try to get out of it.  To fix this, don't
   * send company interests if there's no industry interests.
   * @param user
   * @return
   */
  private static User ftueClearInterestsHack(User user) {
    if (UserInterests.getUserIndustryFeatureIds(user).isEmpty()) {
      return user.toBuilder().clearInterest().build();
    }
    return user;
  }

  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws BiznessException, RequestException, DatabaseSchemaException, DatabaseRequestException {
    String fbAccessToken = getRequiredParameter(req, "fb_access_token");
    String fbUserId = getRequiredParameter(req, "fb_user_id");

    com.restfb.types.User fbUser;
    User user;
    try {
      fbUser = FacebookLoginHandler.getFacebookUser(fbAccessToken);
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
    response.put("user", new UserHelper(ftueClearInterestsHack(user)).getUserJson());
    response.put("session", Serializer.toJSON(session));

    // To help with client latency, return the articles for the user's home
    // screen in this response.
    Iterable<Article> articles = Articles.getMainStream(user);
    response.put("articles", ArticleSerializer.serialize(articles, user,
        false /* includeLinkedInContacts */, false /* includeAddressBookContacts */));

    return response;
  }
}
