package com.janknspank;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import com.janknspank.ArticleHandler.ArticleCallback;
import com.janknspank.data.ArticleKeywords;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Urls;
import com.janknspank.data.Links;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.InterpretedData;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Core.Link;

public class TheMachine {
  public void start() throws DataInternalException {
    String originUrl = "http://www.nytimes.com/";
    Urls.put(originUrl, false);

    while (true) {
      final Url startUrl = Urls.markAsCrawled(Urls.getNextUrlToCrawl());
//      final Url startUrl = Url.newBuilder()
//          .setId("pYZDE7M36zxQNxbFTUVFCQ")
//          .setUrl("http://techcrunch.com/2015/01/03/the-sharing-economy-and-the-"
//              + "future-of-finance/")
//          .setTweetCount(0)
//          .setDiscoveryTime(System.currentTimeMillis())
//          .build();
      if (startUrl == null) {
        // Some other thread has likely claimed this URL - Go get another.
        continue;
      }

      if (!NewsSiteWhitelist.isOkay(startUrl.getUrl())) {
        System.err.println("Removing now-blacklisted page: " + startUrl.getUrl());
        Links.deleteIds(ImmutableList.of(startUrl.getId()));
        Database.delete(startUrl);
        continue;
      }

      System.err.println("Crawling: " + startUrl.getUrl());
      final String startUrlId = startUrl.getId();
      final Set<String> foundUrls = Sets.newHashSet();
      new Crawler(new ArticleCallback() {
        List<Message> linkList = Lists.newArrayList();

        @Override
        public void foundUrl(String url) {
          try {
            url = UrlCleaner.clean(url);
          } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
          }

          // Skip non-whitelisted URLs and URLs we've already seen on this page.
          if (!NewsSiteWhitelist.isOkay(url) || foundUrls.contains(url)) {
            return;
          }
          foundUrls.add(url);

          try {
            Url destination = Urls.put(url, /* isTweet */ false);
            linkList.add(Link.newBuilder()
                .setOriginUrlId(startUrlId)
                .setDestinationUrlId(destination.getId())
                .setDiscoveryTime(destination.getDiscoveryTime())
                .setLastFoundTime(destination.getDiscoveryTime())
                .build());
          } catch (DataInternalException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void foundArticle(Article article,
            InterpretedData interpretedData,
            Set<String> keywords) {
          try {
            // Flush any Links we've found lately.
            Database.insert(linkList);
            linkList.clear();

            // Save this article and its keywords.
            Database.insert(article);
            ArticleKeywords.add(article, interpretedData);
            ArticleKeywords.add(article, keywords);
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
