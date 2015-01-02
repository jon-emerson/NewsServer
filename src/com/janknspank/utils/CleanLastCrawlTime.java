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
 * Marks last_crawl_time as NULL and re-instates crawl_priority for articles
 * we've parsed but not actually stored any Article data for (because we
 * had an exception / error while trying to create it).
 */
public class CleanLastCrawlTime {
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
        "SELECT * FROM " + Database.getTableName(Url.class) + " " +
        "WHERE crawl_priority=0 AND url NOT LIKE \"%//twitter.com/%\"");
    result = stmt.executeQuery();
    List<Message> urlsToUpdate = Lists.newArrayList();
    while (!result.isAfterLast()) {
      Url url = Database.createFromResultSet(result, Url.class);
      if (url != null) {
        if (!crawledArticleIds.contains(url.getId())) {
          System.out.println("Fixin " + url.getUrl());
          urlsToUpdate.add(url.toBuilder()
              .clearLastCrawlTime()
              .setCrawlPriority(Urls.getCrawlPriority(url.getUrl(), null))
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
