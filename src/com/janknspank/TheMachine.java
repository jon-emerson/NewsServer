package com.janknspank;

import java.net.MalformedURLException;

import com.janknspank.Crawler.CrawlerCallback;

public class TheMachine {
  public void start() {
    String originUrl = "http://www.latimes.com/about/la-sitemap-htmlstory.html";
    DiscoveredUrl.put(originUrl);

    DiscoveredUrl startUrl = DiscoveredUrl.getNextUrlToCrawl();
    while (startUrl != null) {
      System.err.println("Crawling: " + startUrl.getUrl());
      if (!startUrl.markAsCrawled()) {
        // Some other thread has likely claimed this URL - Go get another.
        startUrl = DiscoveredUrl.getNextUrlToCrawl();
        continue;
      }

      final String startUrlId = startUrl.getId();
      new Crawler(new CrawlerCallback() {
        @Override
        public void foundUrl(String url) {
          try {
            url = UrlCleaner.clean(url);
          } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
          }

          if (NewsSiteWhitelist.isOkay(url)) {
            DiscoveredUrl destination = DiscoveredUrl.put(url);
            Link.put(startUrlId, destination.getId());
          }
        }

        @Override
        public void foundCrawlData(CrawlData data) {
          data.insert();
        }
      }).crawl(startUrl);

      // Get the next URL.
      startUrl = DiscoveredUrl.getNextUrlToCrawl();
    }
  }

  public static void main(String args[]) {
    new TheMachine().start();
  }
}
