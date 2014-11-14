package com.janknspank;

import java.net.MalformedURLException;

import com.janknspank.Crawler.CrawlerCallback;

public class TheMachine {
  public void start() {
    String originUrl = "http://www.latimes.com/about/la-sitemap-htmlstory.html";
    DiscoveredUrl.put(originUrl, false);

    while (true) {
      final DiscoveredUrl startUrl = DiscoveredUrl.getNextUrlToCrawl();

      if (!NewsSiteWhitelist.isOkay(startUrl.getUrl())) {
        System.err.println("Removing now-blacklisted site: " + startUrl.getUrl());
        Link.deleteId(startUrl.getId());
        startUrl.delete();
        continue;
      }

      System.err.println("Crawling: " + startUrl.getUrl());
      if (!startUrl.markAsCrawled()) {
        // Some other thread has likely claimed this URL - Go get another.
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
            DiscoveredUrl destination = DiscoveredUrl.put(url, false);
            Link.put(startUrlId, destination.getId(), destination.getDiscoveryTime());
          }
        }

        @Override
        public void foundCrawlData(CrawlData data) {
          data.insert();
        }
      }).crawl(startUrl);
    }
  }

  public static void main(String args[]) {
    new TheMachine().start();
  }
}
