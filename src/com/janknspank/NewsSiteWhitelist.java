package com.janknspank;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

public class NewsSiteWhitelist {
  private static final HashSet<String> WHITELIST = new HashSet<String>();
  static {
    WHITELIST.add("abc.net.au");
    WHITELIST.add("abcnews.go.com");
    WHITELIST.add("aljazeera.net");
    WHITELIST.add("ap.org");
    WHITELIST.add("bdnews24.com");
    WHITELIST.add("bloomberg.com");
    WHITELIST.add("boston.com");
    WHITELIST.add("breitbart.com");
    WHITELIST.add("businessweek.com");
    WHITELIST.add("cbsnews.com");
    WHITELIST.add("chicagotribune.com");
    WHITELIST.add("chron.com");
    WHITELIST.add("cnbc.ca");
    WHITELIST.add("cnbc.com");
    WHITELIST.add("cnn.com");
    WHITELIST.add("csmonitor.com");
    WHITELIST.add("drudgereport.com");
    WHITELIST.add("dw.de");
    WHITELIST.add("economist.com");
    WHITELIST.add("examiner.com");
    WHITELIST.add("forbes.com");
    WHITELIST.add("foxnews.com");
    WHITELIST.add("guardian.co.uk");
    WHITELIST.add("haaretz.com");
    WHITELIST.add("hindustantimes.com");
    WHITELIST.add("hollywoodreporter.com");
    WHITELIST.add("huffingtonpost.com");
    WHITELIST.add("indiatimes.com");
    WHITELIST.add("jpost.com");
    WHITELIST.add("latimes.com");
    WHITELIST.add("manoramaonline.com");
    WHITELIST.add("msnbc.com");
    WHITELIST.add("nationalgeographic.com");
    WHITELIST.add("news.bbc.co.uk");
    WHITELIST.add("news.yahoo.com");
    WHITELIST.add("nj.com");
    WHITELIST.add("nypost.com");
    WHITELIST.add("nytimes.com");
    WHITELIST.add("reuters.com");
    WHITELIST.add("sfgate.com");
    WHITELIST.add("smh.com.au");
    WHITELIST.add("telegraph.co.uk");
    WHITELIST.add("theatlantic.com");
    WHITELIST.add("theglobeandmail.com");
    WHITELIST.add("theguardian.com");
    WHITELIST.add("thehindu.com");
    WHITELIST.add("usatoday.com");
    WHITELIST.add("usnews.com");
    WHITELIST.add("washingtonpost.com");
    WHITELIST.add("wsj.com");
  }

  private static final HashSet<String> BLACKLIST = new HashSet<String>();
  static {
    BLACKLIST.add("classifieds.latimes.com");
    BLACKLIST.add("dating.telegraph.co.uk");
    BLACKLIST.add("digitalservices.latimes.com");
    BLACKLIST.add("dj.wsj.com");
    BLACKLIST.add("forums.abcnews.go.com");
    BLACKLIST.add("framework.latimes.com");
    BLACKLIST.add("games.latimes.com");
    BLACKLIST.add("gardenshop.telegraph.co.uk");
    BLACKLIST.add("jp.wsj.com");
    BLACKLIST.add("mexico.cnn.com");
    BLACKLIST.add("shop.telegraph.co.uk");
  }

  public static boolean isOkay(String url) {
    try {
      URL bigUrl = new URL(url);
      String domain = bigUrl.getHost();

      // Path exclusions.
      if (domain.endsWith("abcnews.go.com") && bigUrl.getPath().startsWith("/meta/")) {
        return false;
      }
      if (domain.endsWith("latimes.com")) {
        // Don't geek out on the historical articles.  If they're linked from other
        // places, great, but we don't need to dig through all of them.
        if (bigUrl.getPath().matches("^\\/[12][0-9]{3}\\/.*")) {
          return false;
        }
      }

      // Domain exclusions.
      while (domain.contains(".")) {
        if (BLACKLIST.contains(domain)) {
          return false;
        }
        if (WHITELIST.contains(domain)) {
          return true;
        }
        domain = domain.substring(domain.indexOf(".") + 1);
      }
      return false;
    } catch (MalformedURLException e) {
      return false;
    }
  }
}

