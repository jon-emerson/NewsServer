package com.janknspank.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import com.janknspank.data.Database;
import com.janknspank.data.Urls;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;

/**
 * Goes through all the URLs in our database and sets their crawl priority
 * to the latest-and-greatest calculation involving the article's date and
 * overall importance.
 */
public class UpdateCrawlPriorities {

  public static void main(String args[]) throws Exception {
    // Figure out what articles we've crawled already.
    Set<String> crawledArticleIds = Sets.newHashSet();
    PreparedStatement stmt = Database.getConnection().prepareStatement(
        "SELECT * FROM " + Database.getTableName(Article.class));
    ResultSet result = stmt.executeQuery();
    while (!result.isAfterLast()) {
      Article article = Database.createFromResultSet(result, Article.class);
      if (article != null) {
        crawledArticleIds.add(article.getUrlId());
      }
    }

    stmt = Database.getConnection().prepareStatement(
        "SELECT * FROM " + Database.getTableName(Url.class));
    result = stmt.executeQuery();
    List<Message> urlsToUpdate = Lists.newArrayList();
    while (!result.isAfterLast()) {
      Url url = Database.createFromResultSet(result, Url.class);
      if (url != null) {
        int crawlPriority = crawledArticleIds.contains(url.getId())
            ? 0 : Urls.getCrawlPriority(url.getUrl(), null);
        if (Math.abs(url.getCrawlPriority() - crawlPriority) > 5) {
          System.out.println("pri=" + crawlPriority + " for " + url.getUrl());
          urlsToUpdate.add(url.toBuilder()
              .setCrawlPriority(crawlPriority)
              .build());
        }
      }
      if (urlsToUpdate.size() == 100 || url == null) {
        System.out.println(Database.update(urlsToUpdate) + " rows updated");
        urlsToUpdate.clear();
      }
    }
  }
}
