package com.janknspank;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.data.Database;
import com.janknspank.data.Links;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;

public class NewsSiteWhitelist {
  private static final HashSet<String> WHITELIST = new HashSet<String>();
  static {
    WHITELIST.add("abc.net.au");
    WHITELIST.add("abcnews.go.com");
    WHITELIST.add("america.aljazeera.com");
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
    WHITELIST.add("chron.com");
    WHITELIST.add("cleveland.com");
    WHITELIST.add("cnbc.com");
    WHITELIST.add("cnn.com");
    // WHITELIST.add("csmonitor.com");
    WHITELIST.add("curbed.com");
    // WHITELIST.add("dailyrecord.co.uk");
    // WHITELIST.add("dallasnews.com");
    // WHITELIST.add("denverpost.com");
    // WHITELIST.add("drudgereport.com");
    // WHITELIST.add("dw.de");
    // WHITELIST.add("economist.com"); **
    // WHITELIST.add("engadget.com"); **
    // WHITELIST.add("eonline.com");
    // WHITELIST.add("ew.com");
    // WHITELIST.add("examiner.com");
    // WHITELIST.add("extremetech.com");
    // WHITELIST.add("forbes.com");
    // WHITELIST.add("foxnews.com");
    // WHITELIST.add("freep.com");
    // WHITELIST.add("gizmodo.com");
    // WHITELIST.add("globalpost.com");
    // WHITELIST.add("guardian.co.uk");
    // WHITELIST.add("haaretz.com");
    // WHITELIST.add("heraldscotland.com");
    // WHITELIST.add("hindustantimes.com");
    // WHITELIST.add("hollywoodreporter.com");
    // WHITELIST.add("huffingtonpost.com");
    // WHITELIST.add("indiatimes.com");
    // WHITELIST.add("iol.co.za");
    // WHITELIST.add("irishtimes.com");
    // WHITELIST.add("jpost.com");
    WHITELIST.add("latimes.com");
    // WHITELIST.add("manchestereveningnews.co.uk");
    // WHITELIST.add("manoramaonline.com");
    // WHITELIST.add("mashable.com"); **
    WHITELIST.add("mercurynews.com");
    // WHITELIST.add("mg.co.za");
    // WHITELIST.add("msnbc.com");
    // WHITELIST.add("nbcnews.com");
    // WHITELIST.add("nationalgeographic.com");
    // WHITELIST.add("ndtv.com");
    // WHITELIST.add("news.yahoo.com");
    // WHITELIST.add("nj.com");
    // WHITELIST.add("nypost.com");
    WHITELIST.add("nytimes.com");
    // WHITELIST.add("nzherald.co.nz");
    // WHITELIST.add("reuters.com");
    // WHITELIST.add("scmp.com");
    // WHITELIST.add("scotsman.com");
    // WHITELIST.add("seattletimes.com");
    WHITELIST.add("sfexaminer.com");
    WHITELIST.add("sfgate.com");
    WHITELIST.add("siliconbeat.com");
    // WHITELIST.add("smh.com.au");
    // WHITELIST.add("startribune.com");
    // WHITELIST.add("statesman.com");
    // WHITELIST.add("straitstimes.com");
    WHITELIST.add("techcrunch.com");
    // WHITELIST.add("techmeme.com");
    // WHITELIST.add("telegraph.co.uk");
    // WHITELIST.add("theatlantic.com"); **
    // WHITELIST.add("theglobeandmail.com");
    // WHITELIST.add("theguardian.com");
    // WHITELIST.add("thehindu.com");
    // WHITELIST.add("thenextweb.com");
    // WHITELIST.add("theregister.co.uk");
    // WHITELIST.add("theverge.com");
    // WHITELIST.add("tmz.com");
    // WHITELIST.add("usatoday.com");
    // WHITELIST.add("usnews.com");
    WHITELIST.add("washingtonpost.com");
    // WHITELIST.add("washingtontimes.com");
    // WHITELIST.add("westword.com");
    // WHITELIST.add("wired.com");
    // WHITELIST.add("zdnet.com");
  }

  /**
   * Makes sure that BBC News URLs start with a path in the joined whitelist and
   * end with an article number somewhere in the path after the final slash.
   * (Article numbers are usually 8 digits.)
   */
  private static final Pattern BBC_NEWS_ARTICLE_PATH = Pattern.compile(
      "^\\/(" + Joiner.on("|").join(new String[] {
          "news",
          "future\\/story",
          "culture\\/story",
          "earth\\/story",
          "capital\\/story",
          "autos\\/story"
      }) + ")\\/.*[0-9]{6,10}[^\\/]*$");

  /**
   * URL validation regex's for NYTimes articles.
   */
  private static final Pattern NYTIMES_ARTICLE_DOMAIN = Pattern.compile(
      "^(" + Joiner.on("|").join(new String[] {
          "nytimes\\.com",
          "www\\.nytimes\\.com",
          "dealbook\\.nytimes\\.com"
      }) + ")$");
  private static final Pattern NYTIMES_ARTICLE_PATH = Pattern.compile(
      "^\\/(1|2)[0-9]{3}\\/[0-9]{2}\\/[0-9]{2}\\/.*(\\/|\\.html)$");

  private static final HashSet<String> BLACKLIST = new HashSet<String>();
  static {
    for (String blacklistDomain : new String[] {
        "account.washingtonpost.com",
        "advertise.latimes.com",
        "advertising.chicagotribune.com",
        "advertising.washingtonpost.com",
        "alerts.uk.reuters.com",
        "apps.chicagotribune.com",
        "apps.washingtonpost.com",
        "arabic.cnn.com",
        "audience.cnn.com",
        "autos.cleveland.com",
        "autos.nj.com",
        "bdn-ak.bloomberg.com",
        "binaryapi.ap.org",
        "blogs.cnn.com",
        "businessfinder.cleveland.com",
        "calendar.boston.com",
        "cars.irishtimes.com",
        "cars.sfgate.com",
        "cgi.money.cnn.com",
        "cinesport.cleveland.com",
        "circulars.boston.com",
        "classifieds.cleveland.com",
        "classifieds.latimes.com",
        "classifieds.nj.com",
        "cnnespanol.cnn.com",
        "connect.bloomberg.com",
        "connect.cleveland.com",
        "customers.reuters.com",
        "data.cnbc.com",
        "datadesk.latimes.com",
        "dating.telegraph.co.uk",
        "dailydeals.latimes.com",
        "demiurge.arstechnica.com",
        "digitalservices.latimes.com",
        "discussion.theguardian.com",
        "discussions.latimes.com",
        "disrupt.techcrunch.com",
        "dj.wsj.com",
        "documents.latimes.com",
        "ee.latimes.com",
        "episteme.arstechnica.com",
        "events.sfgate.com",
        "faq.external.bbc.co.uk",
        "fanshop.sfgate.com",
        "feeds.arstechnica.com",
        "feeds.washingtonpost.com",
        "findnsave.cleveland.com",
        "findnsave.dallasnews.com",
        "findnsave.washingtonpost.com",
        "finds.boston.com",
        "foreclosures.cleveland.com",
        "forums.abcnews.go.com",
        "framework.latimes.com",
        "futuresnow.cnbc.com",
        "games.cnn.com",
        "games.latimes.com",
        "games.washingtonpost.com",
        "gardenshop.telegraph.co.uk",
        "graphics.latimes.com",
        "guides.latimes.com",
        "highschoolsports.cleveland.com",
        "homeguides.sfgate.com",
        "id.theguardian.com",
        "ilivehere.latimes.com",
        "images.businessweek.com",
        "inhealth.cnn.com",
        "iplayerhelp.external.bbc.co.uk",
        "ireport.cnn.com",
        "iview.abc.net.au",
        "jobs.bloomberg.com",
        "jobs.businessweek.com",
        "jobs.cleveland.com",
        "jobsearch.bloomberg.com",
        "jobsearch.money.cnn.com",
        "jp.techcrunch.com",
        "jp.wsj.com",
        "js.washingtonpost.com",
        "khabar.ndtv.com",
        "listings.boston.com",
        "localdeals.latimes.com",
        "login.bloomberg.com",
        "m.bbc.com",
        "m.bloomberg.com",
        "m.cleveland.com",
        "m.washingtonpost.com",
        "marketplace.latimes.com",
        "marketplaceads.latimes.com",
        "media.bloomberg.com",
        "mediakit.latimes.com",
        "members.chicagotribune.com",
        "membership.latimes.com",
        "mexico.cnn.com",
        "mobile.abc.net.au",
        "mobile.bloomberg.com",
        "mobile.businessweek.com",
        "mobilejobs.cleveland.com",
        "mobile-phones.smh.com.au",
        "myaccount.dallasnews.com",
        "nie.washingtonpost.com",
        "partners.cnn.com",
        "placeanad.chicagotribune.com",
        "placeanad.latimes.com",
        "portfolio.cnbc.com",
        "pro.cnbc.com",
        "profile.theguardian.com",
        "projects.latimes.com",
        "radio.foxnews.com",
        "realestate.boston.com",
        "realestate.cleveland.com",
        "realestate.money.cnn.com",
        "realestate.washingtonpost.com",
        "recipes.latimes.com",
        "rss.cnn.com",
        "rssfeeds.usatoday.com",
        "scene.boston.com",
        "schools.latimes.com",
        "search.abc.net.au",
        "search.bloomberg.com",
        "search.boston.com",
        "search.cleveland.com",
        "secure.businessweek.com",
        "service.bloomberg.com",
        "shop.abc.net.au",
        "shop.telegraph.co.uk",
        "shopping.nj.com",
        "signup.cleveland.com",
        "spiderbites.boston.com",
        "ssl.bbc.co.uk",
        "ssl.washingtonpost.com",
        "static.reuters.com",
        "stats.boston.com",
        "stats.cleveland.com",
        "store.latimes.com",
        "subscribe.businessweek.com",
        "subscribe.washingtonpost.com",
        "syndication.boston.com",
        "tamil.thehindu.com",
        "tickets.boston.com",
        "transcripts.cnn.com",
        "update.cleveland.com",
        "watchlist.cnbc.com",
        "webcast.cnbc.com",
        "www2b.abc.net.au"}) {
      BLACKLIST.add(blacklistDomain);
    }

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
      if (domain.contains("video.") ||
          domain.contains("videos.") ||
          path.startsWith("/cgi-bin/") ||
          path.contains("/video/") ||
          path.contains("/videos/")) {
        return false;
      }
      if (domain.endsWith("abcnews.go.com") &&
          (path.startsWith("/blogs/") ||
           path.startsWith("/meta/") ||
           path.startsWith("/xmldata/") ||
           path.contains("/photos/") ||
           path.contains("/videos/") ||
           path.endsWith("/video"))) {
        return false;
      }
      if (domain.endsWith("arstechnica.com") &&
          (path.startsWith("/civis/") ||
           path.startsWith("/forum/") ||
           path.startsWith("/guide/") ||
           path.startsWith("/guides/") ||
           path.startsWith("/sponsored/") ||
           path.startsWith("/subscriptions/"))) {
        return false;
      }
      if (domain.endsWith("bbc.co.uk") || domain.endsWith("bbc.com")) {
        if (!BBC_NEWS_ARTICLE_PATH.matcher(path).matches()) {
          return false;
        }
      }
      if (domain.endsWith(".bloomberg.com") &&
          (path.startsWith("/podcasts/") ||
           path.startsWith("/quote/") ||
           path.startsWith("/slideshow/") ||
           path.startsWith("/visual-data/") ||
           path.endsWith("/_/slideshow/"))) {
        return false;
      }
      if (domain.endsWith("boston.com") &&
          (path.startsWith("/boston/action/rssfeed") ||
           path.startsWith("/cars/") ||
           path.startsWith("/help/") ||
           path.startsWith("/radio") ||
           path.startsWith("/sports/blogs/"))) {
        return false;
      }
      if (domain.endsWith("businessweek.com") &&
          (path.startsWith("/adsections/") ||
           path.startsWith("/bschools/") ||
           path.startsWith("/business-schools/") ||
           path.startsWith("/companies-and-industries/") ||
           path.startsWith("/global-economics/") ||
           path.startsWith("/innovation-and-design/") ||
           path.startsWith("/innovation/") ||
           path.startsWith("/interactive/") ||
           path.startsWith("/lifestyle/") ||
           path.startsWith("/markets-and-finance/") ||
           path.startsWith("/photos/") ||
           path.startsWith("/politics-and-policy/") ||
           path.startsWith("/printer/") ||
           path.startsWith("/quiz/") ||
           path.startsWith("/reports/") ||
           path.startsWith("/slideshows/") ||
           path.startsWith("/small-business/") ||
           path.startsWith("/technology/"))) {
        return false;
      }
      if (domain.endsWith("cbc.ca") &&
          (path.startsWith("/mediacentre/") ||
           path.startsWith("/player/") ||
           path.startsWith("/shop/"))) {
        return false;
      }
      if (domain.endsWith("cleveland.com") &&
          (path.endsWith("/print.html"))) {
        return false;
      }
      if (domain.endsWith("cnbc.com") &&
          path.startsWith("/live-tv/")) {
        return false;
      }
      if (domain.endsWith("cnn.com") &&
          (path.startsWith("/CNN/") ||
           path.startsWith("/services/"))) {
        return false;
      }
      if (domain.endsWith(".chicagotribune.com") && path.endsWith("/comments/atom.xml")) {
        return false;
      }
      if (domain.endsWith(".cnn.com") &&
          (path.startsWith("/calculator/") ||
           path.startsWith("/data/") ||
           path.startsWith("/infographic/") ||
           path.startsWith("/interactive/") ||
           path.startsWith("/quizzes/"))) {
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
        if (path.startsWith("/shopping/")) {
          return false;
        }
        // Don't geek out on the historical articles.  If they're linked from other
        // places, great, but we don't need to dig through all of them.
        if (path.matches("^\\/[12][0-9]{3}\\/.*")) {
          return false;
        }
      }
      if (domain.endsWith("money.usnews.com") && path.startsWith("/529s/")) {
        return false;
      }
      if (domain.endsWith("money.cnn.com") &&
          (path.startsWith("/data/") ||
           path.startsWith("/quote/") ||
           path.startsWith("/tag/"))) {
        return false;
      }
      if (domain.endsWith(".nj.com") && path.endsWith("/print.html")) {
        return false;
      }
      if (domain.endsWith("nytimes.com")) {
        if (!NYTIMES_ARTICLE_DOMAIN.matcher(domain).matches() ||
            !NYTIMES_ARTICLE_PATH.matcher(path).matches()) {
          return false;
        }
      }
      if (domain.endsWith("sfgate.com") &&
          path.startsWith("/merge/")) {
        return false;
      }
      if (domain.endsWith("techcrunch.com") &&
          (path.startsWith("/events/") ||
           path.startsWith("/rss/"))) {
        return false;
      }
      if (domain.endsWith("telegraph.co.uk") && path.startsWith("/sponsored/")) {
        return false;
      }
      if (domain.endsWith("usatoday.com") && path.startsWith("/marketing/rss/")) {
        return false;
      }
      if (domain.endsWith("washingtonpost.com") &&
          (path.contains("/wp-dyn/") ||
           path.endsWith("_category.html"))) {
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
          path.endsWith(".pdf") ||
          path.endsWith(".png")) {
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

  /**
   * Cleans up the database by deleting any URLs and related metadata (crawl
   * data, links, etc) for articles that are now blacklisted or whose query
   * parameters are now more restrictive.
   * NOTE(jonemerson): We delete URLs with now-obsolete query parameters,
   * rather than update them, because it's generally difficult to canonicalize
   * two URLs and we can always find the cleaned URL again later, if necessary.
   */
  public static void main(String args[]) throws Exception {
    PreparedStatement stmt = Database.getConnection().prepareStatement(
        "SELECT * FROM " + Database.getTableName(Url.class) + " WHERE url NOT LIKE '%//twitter.com/%'");
    ResultSet result = stmt.executeQuery();
    Map<String, String> urlsToDelete = Maps.newHashMap();
    while (!result.isAfterLast()) {
      Url url = Database.createFromResultSet(result, Url.class);
      if (url != null) {
        String urlStr = url.getUrl();
        if ((!isOkay(urlStr) || !urlStr.equals(UrlCleaner.clean(urlStr)))) {
          urlsToDelete.put(urlStr, url.getId());
        }
      }
      if (urlsToDelete.size() == 100 || url == null) {
        List<String> urls = Lists.newArrayList();
        List<String> ids = Lists.newArrayList();
        for (String urlStr : urlsToDelete.keySet()) {
          System.out.println("Deleting url: " + urlStr);
          urls.add(urlStr);
          ids.add(urlsToDelete.get(urlStr));
        }
        System.out.println("Deleted " + Database.deletePrimaryKeys(urls, Url.class) + " urls");
        System.out.println("Deleted " + Database.deletePrimaryKeys(ids, Article.class) + " articles");
        System.out.println("Deleted " + Links.deleteIds(ids) + " links");
        urlsToDelete.clear();
      }
    }
  }
}

