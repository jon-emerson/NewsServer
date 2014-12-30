package com.janknspank;

import java.net.MalformedURLException;

import com.janknspank.ArticleHandler.ArticleCallback;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Urls;
import com.janknspank.data.Links;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Core.Link;

public class TheMachine {
  public void start() throws DataInternalException {
    String originUrl = "http://www.nytimes.com/";
    Urls.put(originUrl, false);

    while (true) {
      final Url startUrl = Urls.markAsCrawled(Urls.getNextUrlToCrawl());
      if (startUrl == null) {
        // Some other thread has likely claimed this URL - Go get another.
        continue;
      }

      if (!NewsSiteWhitelist.isOkay(startUrl.getUrl())) {
        System.err.println("Removing now-blacklisted site: " + startUrl.getUrl());
        Links.deleteId(startUrl.getId());
        Database.delete(startUrl);
        continue;
      }

      System.err.println("Crawling: " + startUrl.getUrl());
      final String startUrlId = startUrl.getId();
      new Crawler(new ArticleCallback() {
        @Override
        public void foundUrl(String url) {
          try {
            url = UrlCleaner.clean(url);
          } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
          }

          if (NewsSiteWhitelist.isOkay(url)) {
            try {
              Url destination = Urls.put(url, /* isTweet */ false);
              Database.insert(Link.newBuilder()
                  .setOriginId(startUrlId)
                  .setDestinationId(destination.getId())
                  .setDiscoveryTime(destination.getDiscoveryTime())
                  .setLastFoundTime(destination.getDiscoveryTime())
                  .build());
            } catch (ValidationException|DataInternalException e) {
              e.printStackTrace();
            }
          }
        }

        @Override
        public void foundArticle(Article article) {
          try {
            Database.insert(article);
          } catch (ValidationException|DataInternalException e) {
            e.printStackTrace();
          }
        }
      }).crawl(startUrl);
    }
  }

  public static void main(String args[]) throws Exception {
    new TheMachine().start();
  }
}
