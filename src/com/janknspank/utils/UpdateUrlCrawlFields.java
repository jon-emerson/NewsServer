package com.janknspank.utils;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.janknspank.data.Database;
import com.janknspank.data.QueryOption;
import com.janknspank.data.Urls;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;

/**
 * Updates crawl_priority, last_crawl_start_time, and last_crawl_finish_time for
 * articles we have URLs for but we haven't actually stored any Article data for.
 * This basically fixes up the DB after we've had exceptions parsing specific
 * articles, or we've decided to purge certain Articles and re-crawl them.
 * 
 * DO NOT RUN THIS WHILE A CRAWL IS GOING - IT WILL MARK ARTICLES AS UNCRAWLED
 * IF THEY WERE CRAWLED WHILE THIS PROCESS STARTED RUNNING.
 */
public class UpdateUrlCrawlFields {
  public static void main(String args[]) throws Exception {
    Database database = Database.getInstance();

    // Figure out what articles we've crawled already.
    Set<String> crawledArticleIds = Sets.newHashSet();
    for (Article article : database.get(Article.class)) {
      crawledArticleIds.add(article.getUrlId());
    }

    List<Url> urlsToUpdate = Lists.newArrayList();
    for (Url url : database.get(Url.class,
        new QueryOption.WhereEquals("crawl_priority", "0"),
        new QueryOption.WhereNotLike("url", "%//twitter.com/%"))) {
      if (!crawledArticleIds.contains(url.getId())) {
        System.out.println("Fixin " + url.getUrl());
        urlsToUpdate.add(url.toBuilder()
            .clearLastCrawlStartTime()
            .clearLastCrawlFinishTime()
            .setCrawlPriority(Urls.getCrawlPriority(url.getUrl(), null))
            .build());
      }
      if (urlsToUpdate.size() == 250 || url == null) {
        System.out.println(database.update(urlsToUpdate) + " rows updated");
        urlsToUpdate.clear();
      }
    }
  }
}
