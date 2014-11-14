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
    WHITELIST.add("arstechnica.com");
    WHITELIST.add("bbc.co.uk");
    WHITELIST.add("bbc.com");
    WHITELIST.add("bdnews24.com");
    WHITELIST.add("bloomberg.com");
    WHITELIST.add("boston.com");
    WHITELIST.add("breitbart.com");
    WHITELIST.add("buffalonews.com");
    WHITELIST.add("businessweek.com");
    WHITELIST.add("cbc.ca");
    WHITELIST.add("cbsnews.com");
    WHITELIST.add("channelnewsasia.com");
    WHITELIST.add("chicagotribune.com");
    WHITELIST.add("chron.com");
    WHITELIST.add("cleveland.com");
    WHITELIST.add("cinemablend.com");
    WHITELIST.add("cnbc.ca");
    WHITELIST.add("cnbc.com");
    WHITELIST.add("cnn.com");
    WHITELIST.add("csmonitor.com");
    WHITELIST.add("dailyrecord.co.uk");
    WHITELIST.add("dallasnews.com");
    WHITELIST.add("denverpost.com");
    WHITELIST.add("drudgereport.com");
    WHITELIST.add("dw.de");
    WHITELIST.add("economist.com");
    WHITELIST.add("engadget.com");
    WHITELIST.add("eonline.com");
    WHITELIST.add("ew.com");
    WHITELIST.add("examiner.com");
    WHITELIST.add("extremetech.com");
    WHITELIST.add("forbes.com");
    WHITELIST.add("foxnews.com");
    WHITELIST.add("freep.com");
    WHITELIST.add("gizmodo.com");
    WHITELIST.add("globalpost.com");
    WHITELIST.add("guardian.co.uk");
    WHITELIST.add("haaretz.com");
    WHITELIST.add("heraldscotland.com");
    WHITELIST.add("hindustantimes.com");
    WHITELIST.add("hollywoodreporter.com");
    WHITELIST.add("huffingtonpost.com");
    WHITELIST.add("indiatimes.com");
    WHITELIST.add("iol.co.za");
    WHITELIST.add("irishtimes.com");
    WHITELIST.add("jpost.com");
    WHITELIST.add("latimes.com");
    WHITELIST.add("manchestereveningnews.co.uk");
    WHITELIST.add("manoramaonline.com");
    WHITELIST.add("mashable.com");
    WHITELIST.add("mg.co.za");
    WHITELIST.add("msnbc.com");
    WHITELIST.add("nbcnews.com");
    WHITELIST.add("nationalgeographic.com");
    WHITELIST.add("ndtv.com");
    WHITELIST.add("news.bbc.co.uk");
    WHITELIST.add("news.yahoo.com");
    WHITELIST.add("nj.com");
    WHITELIST.add("nypost.com");
    WHITELIST.add("nytimes.com");
    WHITELIST.add("nzherald.co.nz");
    WHITELIST.add("reuters.com");
    WHITELIST.add("scmp.com");
    WHITELIST.add("scotsman.com");
    WHITELIST.add("seattletimes.com");
    WHITELIST.add("sfgate.com");
    WHITELIST.add("smh.com.au");
    WHITELIST.add("startribune.com");
    WHITELIST.add("statesman.com");
    WHITELIST.add("straitstimes.com");
    WHITELIST.add("techcrunch.com");
    WHITELIST.add("techmeme.com");
    WHITELIST.add("telegraph.co.uk");
    WHITELIST.add("theatlantic.com");
    WHITELIST.add("theglobeandmail.com");
    WHITELIST.add("theguardian.com");
    WHITELIST.add("thehindu.com");
    WHITELIST.add("thenextweb.com");
    WHITELIST.add("theregister.co.uk");
    WHITELIST.add("theverge.com");
    WHITELIST.add("tmz.com");
    WHITELIST.add("usatoday.com");
    WHITELIST.add("usnews.com");
    WHITELIST.add("washingtonpost.com");
    WHITELIST.add("washingtontimes.com");
    WHITELIST.add("westword.com");
    WHITELIST.add("wired.com");
    WHITELIST.add("wsj.com");
    WHITELIST.add("zdnet.com");
  }

  private static final HashSet<String> BLACKLIST = new HashSet<String>();
  static {
    BLACKLIST.add("account.washingtonpost.com");
    BLACKLIST.add("advertising.chicagotribune.com");
    BLACKLIST.add("alerts.uk.reuters.com");
    BLACKLIST.add("apps.chicagotribune.com");
    BLACKLIST.add("autos.nj.com");
    BLACKLIST.add("binaryapi.ap.org");
    BLACKLIST.add("cars.irishtimes.com");
    BLACKLIST.add("classifieds.latimes.com");
    BLACKLIST.add("classifieds.nj.com");
    BLACKLIST.add("customers.reuters.com");
    BLACKLIST.add("dating.telegraph.co.uk");
    BLACKLIST.add("digitalservices.latimes.com");
    BLACKLIST.add("discussion.theguardian.com");
    BLACKLIST.add("dj.wsj.com");
    BLACKLIST.add("faq.external.bbc.co.uk");
    BLACKLIST.add("feeds.washingtonpost.com");
    BLACKLIST.add("findnsave.dallasnews.com");
    BLACKLIST.add("forums.abcnews.go.com");
    BLACKLIST.add("framework.latimes.com");
    BLACKLIST.add("games.cnn.com");
    BLACKLIST.add("games.latimes.com");
    BLACKLIST.add("gardenshop.telegraph.co.uk");
    BLACKLIST.add("tamil.thehindu.com");
    BLACKLIST.add("id.theguardian.com");
    BLACKLIST.add("iplayerhelp.external.bbc.co.uk");
    BLACKLIST.add("jobsearch.bloomberg.com");
    BLACKLIST.add("jp.wsj.com");
    BLACKLIST.add("khabar.ndtv.com");
    BLACKLIST.add("media.bloomberg.com");
    BLACKLIST.add("members.chicagotribune.com");
    BLACKLIST.add("mexico.cnn.com");
    BLACKLIST.add("mobile-phones.smh.com.au");
    BLACKLIST.add("myaccount.dallasnews.com");
    BLACKLIST.add("myaccount.nytimes.com");
    BLACKLIST.add("placeanad.chicagotribune.com");
    BLACKLIST.add("profile.theguardian.com");
    BLACKLIST.add("query.nytimes.com");
    BLACKLIST.add("radio.foxnews.com");
    BLACKLIST.add("rssfeeds.usatoday.com");
    BLACKLIST.add("search.bloomberg.com");
    BLACKLIST.add("shop.telegraph.co.uk");
    BLACKLIST.add("shopping.nj.com");
    BLACKLIST.add("ssl.bbc.co.uk");
    BLACKLIST.add("static.reuters.com");
    BLACKLIST.add("www2b.abc.net.au");
  }

  public static boolean isOkay(String url) {
    try {
      URL bigUrl = new URL(url);

      // Length exclusion (this is the longest a URL can be in our database).
      if (url.length() > 767) {
        return false;
      }

      // Path exclusions.
      String domain = bigUrl.getHost();
      String path = bigUrl.getPath();
      if (path.startsWith("/cgi-bin/")) {
        return false;
      }
      if (domain.endsWith("abcnews.go.com") && path.startsWith("/meta/")) {
        return false;
      }
      if (domain.endsWith("bbc.co.uk") && path.startsWith("/mpapps/pagetools/")) {
        return false;
      }
      if (domain.endsWith(".bloomberg.com") && path.endsWith("/_/slideshow/")) {
        return false;
      }
      if (domain.endsWith("boston.com") && path.startsWith("/boston/action/rssfeed")) {
        return false;
      }
      if (domain.endsWith(".chicagotribune.com") && path.endsWith("/comments/atom.xml")) {
        return false;
      }
      if (domain.endsWith("forbes.com") && path.startsWith("/account/")) {
        return false;
      }
      if (domain.equals("investing.businessweek.com") &&
          path.startsWith("/research/stocks/snapshot/")) {
        return false;
      }
      if (domain.endsWith("latimes.com")) {
        // Don't geek out on the historical articles.  If they're linked from other
        // places, great, but we don't need to dig through all of them.
        if (path.matches("^\\/[12][0-9]{3}\\/.*")) {
          return false;
        }
      }
      if (domain.endsWith("money.usnews.com") && path.startsWith("/529s/")) {
        return false;
      }
      if (domain.endsWith("money.cnn.com") && path.startsWith("/quote/")) {
        return false;
      }
      if (domain.endsWith(".nj.com") && path.endsWith("/print.html")) {
        return false;
      }
      if (domain.endsWith("usatoday.com") && path.startsWith("/marketing/rss/")) {
        return false;
      }
      if (domain.endsWith("westword.com") &&
          (path.startsWith("/classifieds/") || path.startsWith("/promotions/"))) {
        return false;
      }
      if (domain.endsWith(".wsj.com") && path.endsWith("/tab/print")) {
        return false;
      }

      // Extension exclusions.
      if (path.endsWith("/atom.xml") ||
          path.endsWith("/rss.xml") ||
          path.endsWith(".gif") ||
          path.endsWith(".jpeg") ||
          path.endsWith(".jpg") ||
          path.endsWith(".mp3") ||
          path.endsWith(".mp4") ||
          path.endsWith(".mpeg") ||
          path.endsWith(".pdf")) {
        return false;
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

