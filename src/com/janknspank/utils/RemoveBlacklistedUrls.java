package com.janknspank.utils;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.bizness.Links;
import com.janknspank.common.Logger;
import com.janknspank.crawler.UrlCleaner;
import com.janknspank.crawler.UrlWhitelist;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Url;

/**
 * Cleans up the database by deleting any URLs and related metadata (crawl
 * data, links, etc) for articles that are now blacklisted or whose query
 * parameters are now more restrictive.
 *
 * NOTE(jonemerson): We delete URLs with now-obsolete query parameters,
 * rather than update them, because it's generally difficult to canonicalize
 * two URLs and we can always find the cleaned URL again later, if necessary.
 */
public class RemoveBlacklistedUrls {
  private static final Logger LOG = new Logger(RemoveBlacklistedUrls.class);

  private static void deleteUrlMap(Map<String, String> urlsToDelete) throws DatabaseSchemaException {
    List<String> ids = Lists.newArrayList();
    for (Map.Entry<String, String> urlToDelete : urlsToDelete.entrySet()) {
      LOG.info("Deleting url: " + urlToDelete.getKey());
      ids.add(urlToDelete.getValue());
    }
    LOG.info("Deleted " + Database.with(Article.class).delete(ids) + " articles");
    LOG.info("Deleted " + Links.deleteIds(ids) + " links");
    LOG.info("Deleted " + Database.with(Url.class).delete(ids) + " urls");
  }

  public static void main(String args[]) throws Exception {
    Map<String, String> urlsToDelete = Maps.newHashMap();
    for (Url url : Database.with(Url.class).get(
        new QueryOption.WhereNotLike("url", "%//twitter.com/%"))) {
      String urlStr = url.getUrl();
      if ((!UrlWhitelist.isOkay(urlStr) || !urlStr.equals(UrlCleaner.clean(urlStr)))) {
        urlsToDelete.put(urlStr, url.getId());
      }
      if (urlsToDelete.size() == 100 || url == null) {
        deleteUrlMap(urlsToDelete);
        urlsToDelete.clear();
      }
    }
    deleteUrlMap(urlsToDelete);
  }
}
