package com.janknspank.server;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.UrlFavorite;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/set_user_url_favorite")
public class SetUserUrlFavoriteServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException, BiznessException {
    // Read parameters.
    String urlId = getRequiredParameter(req, "url_id");
    ListenableFuture<Iterable<Article>> articleFuture = Database.with(Article.class).getFuture(
        new QueryOption.WhereEquals("url_id", urlId),
        new QueryOption.Limit(1));
    User user = getUser(req);

    // Parameter validation.
    Article article = null;
    try {
      article = Iterables.getFirst(articleFuture.get(), null);
    } catch (InterruptedException | ExecutionException e) {
      throw new BiznessException("Error fetching article: " + e.getMessage(), e);
    }
    if (article == null) {
      throw new RequestException("Article does not exist");
    }

    // Business logic.
    Set<String> existingFavoriteUrlIds = Sets.newHashSet();
    for (UrlFavorite favorite : user.getUrlFavoriteList()) {
      existingFavoriteUrlIds.add(favorite.getUrlId());
    }
    if (!existingFavoriteUrlIds.contains(urlId)) {
      Database.with(User.class).push(user, "url_favorite", ImmutableList.of(
          UrlFavorite.newBuilder()
              .setUrlId(urlId)
              .setCreateTime(System.currentTimeMillis())
              .build()));

      // If we're not already retaining this article, start doing it now.
      // Otherwise the Pruner will prune it and the user won't have his favorite
      // anymore.
      if (!article.getRetain()) {
        Database.update(article.toBuilder().setRetain(true).build());
      }
    }

    // Write response.
    return createSuccessResponse();
  }
}
