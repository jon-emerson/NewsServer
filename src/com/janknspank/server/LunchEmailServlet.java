package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.notifications.SendLunchEmails;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired
@ServletMapping(urlPattern = "/lunch_email")
public class LunchEmailServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException, BiznessException {
    User user = getUser(req);
    Iterable<Article> articles = SendLunchEmails.getArticles(user);
    return new SoyMapData(
        "title", SendLunchEmails.getTitle(articles, user),
        "articles", SendLunchEmails.getArticleSoyList(user, articles),
        "date", SendLunchEmails.getDate(),
        "spotterAtLunchImgSrc", "/resources/img/spotterAtLunch2@2x.png",
        "spotterEmailRedTagLeftImgSrc", "/resources/img/spotterEmailRedTagLeft@2x.png",
        "spotterEmailRedTagRightImgSrc", "/resources/img/spotterEmailRedTagRight@2x.png",
        "spotterEmailWhiteTagLeftImgSrc", "/resources/img/spotterEmailWhiteTagLeft@2x.png",
        "spotterEmailWhiteTagRightImgSrc", "/resources/img/spotterEmailWhiteTagRight@2x.png",
        "unsubscribeLink", WelcomeEmailServlet.getUnsubscribeLink(user, false /* relativeUrl */));
  }
}
