package com.janknspank.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.data.ArticleKeywords;
import com.janknspank.data.Database;
import com.janknspank.data.Links;
import com.janknspank.data.QueryOption;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;

public class UrlWhitelist {
  public static final Predicate<String> PREDICATE = new Predicate<String>() {
    @Override
    public boolean apply(String url) {
      return UrlWhitelist.isOkay(url);
    }
  };

  private static final HashSet<String> WHITELIST = new HashSet<String>();
  static {
    // Add: http://hbswk.hbs.edu/industries/
    WHITELIST.add("abc.net.au");
    WHITELIST.add("abcnews.go.com");
    WHITELIST.add("advice.careerbuilder.com");
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
    // WHITELIST.add("economist.com"); // DO NOT TURN ON - PAYWALL IS HORRIBLE.
    WHITELIST.add("engadget.com");
    // WHITELIST.add("eonline.com");
    // WHITELIST.add("ew.com");
    // WHITELIST.add("examiner.com");
    // WHITELIST.add("extremetech.com");
    WHITELIST.add("forbes.com");
    // WHITELIST.add("foxnews.com");
    // WHITELIST.add("freep.com");
    WHITELIST.add("gizmodo.com");
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
    WHITELIST.add("mashable.com");
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
    WHITELIST.add("recode.net");
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
    WHITELIST.add("techmeme.com");
    // WHITELIST.add("telegraph.co.uk");
    // WHITELIST.add("theatlantic.com"); **
    // WHITELIST.add("theglobeandmail.com");
    // WHITELIST.add("theguardian.com");
    // WHITELIST.add("thehindu.com");
    WHITELIST.add("thenextweb.com");
    // WHITELIST.add("theregister.co.uk");
    WHITELIST.add("theverge.com");
    // WHITELIST.add("tmz.com");
    // WHITELIST.add("usatoday.com");
    // WHITELIST.add("usnews.com");
    WHITELIST.add("washingtonpost.com");
    // WHITELIST.add("washingtontimes.com");
    // WHITELIST.add("westword.com");
    WHITELIST.add("wired.com");
    // WHITELIST.add("zdnet.com");
  }

  /**
   * Makes sure that BBC News URLs start with a path in the joined whitelist and
   * end with an article number somewhere in the path after the final slash.
   * (Article numbers are usually 8 digits.)
   * TODO(jonemerson): Make this a lot more lenient.  This was made tight
   * initially to address the "we're crawling too much crap" issue - Now that
   * issue's addressed by ArticleUrlDetector and URL crawl priority.
   */
  private static final Pattern BBC_NEWS_ARTICLE_PATH = Pattern.compile(
      "^\\/(" + Joiner.on("|").join(new String[] {
          "autos\\/story",
          "capital\\/story",
          "culture\\/story",
          "earth\\/story",
          "future\\/story",
          "news"
          // TODO: add democracylive (which has a different url pattern)
      }) + ")\\/.*[0-9]{6,10}[^\\/]*$");

  /**
   * URL validation regex's for NYTimes articles.
   * TODO(jonemerson): Make this a lot more lenient.  This was made tight
   * initially to address the "we're crawling too much crap" issue - Now that
   * issue's addressed by ArticleUrlDetector and URL crawl priority.
   */
  private static final Pattern NYTIMES_ARTICLE_DOMAIN = Pattern.compile(
      "^(" + Joiner.on("|").join(new String[] {
          "dealbook\\.nytimes\\.com",
          "nytimes\\.com",
          "www\\.nytimes\\.com"
      }) + ")$");
  private static final Pattern NYTIMES_ARTICLE_PATH = Pattern.compile(
      "^\\/(1|2)[0-9]{3}\\/[0-9]{2}\\/[0-9]{2}\\/.*(\\/|\\.html)$");

  private static final HashSet<String> BLACKLIST = new HashSet<String>();
  static {
    for (String blacklistDomain : new String[] {
        "about.abc.net.au",
        "account.washingtonpost.com",
        "ads.bdnews24.com",
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
        "bdns.bloomberg.com",
        "binaryapi.ap.org",
        "blog.cleveland.com",
        "blogcabin.boston.com",
        "blogs.cnn.com",
        "blogs.forbes.com", // This is their account management site.
        "businessfinder.cleveland.com",
        "bwso.businessweek.com",
        "calendar.boston.com",
        "cars.chron.com",
        "cars.irishtimes.com",
        "cars.sfgate.com",
        "cbsn.cbsnews.com",
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
        "events.mashable.com",
        "events.sfgate.com",
        "extras.sfgate.com",
        "fangear.chron.com",
        "fanshop.sfgate.com",
        "faq.external.bbc.co.uk",
        "feeds.arstechnica.com",
        "feeds.washingtonpost.com",
        "findjobs.mashable.com",
        "findnsave.cleveland.com",
        "findnsave.dallasnews.com",
        "findnsave.washingtonpost.com",
        "finds.boston.com",
        "foreclosures.cleveland.com",
        "forums.abcnews.go.com",
        "forums.businessweek.com",
        "framework.latimes.com",
        "futuresnow.cnbc.com",
        "games.cnn.com",
        "games.latimes.com",
        "games.washingtonpost.com",
        "gardenshop.telegraph.co.uk",
        "graphics.latimes.com",
        "guides.latimes.com",
        "healthguide.boston.com",
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
        "markets.sfgate.com",
        "media.bloomberg.com",
        "mediakit.latimes.com",
        "members.boston.com",
        "members.chicagotribune.com",
        "membership.latimes.com",
        "mexico.cnn.com",
        "mobile.abc.net.au",
        "mobile.bloomberg.com",
        "mobile.businessweek.com",
        "mobilejobs.cleveland.com",
        "mobileobits.cleveland.com",
        "mobile-phones.smh.com.au",
        "music.cbc.ca",
        "myaccount.dallasnews.com",
        "myaccount2.latimes.com",
        "nie.washingtonpost.com",
        "nucwed.aus.aunty.abc.net.au",
        "on.recode.net",
        "open.bloomberg.com",
        "partners.cnn.com",
        "photos.cleveland.com",
        "placeanad.chicagotribune.com",
        "placeanad.latimes.com",
        "portfolio.cnbc.com",
        "portfolio.money.cnn.com",
        "pro.cnbc.com",
        "profile.theguardian.com",
        "projects.latimes.com",
        "r.prdedit.boston.com",
        "radio.foxnews.com",
        "realestate.boston.com",
        "realestate.cleveland.com",
        "realestate.money.cnn.com",
        "realestate.washingtonpost.com",
        "recipes.latimes.com",
        "related.forbes.com",
        "revive.bdnews24.com",
        "rss.cnn.com",
        "rssfeeds.usatoday.com",
        "scene.boston.com",
        "schools.latimes.com",
        "search.abc.net.au",
        "search.bloomberg.com",
        "search.boston.com",
        "search.cleveland.com",
        "search.cnn.com",
        "search1.bloomberg.com",
        "secure.businessweek.com",
        "service.bloomberg.com",
        "services.buffalonews.com",
        "shop.abc.net.au",
        "shop.telegraph.co.uk",
        "shopping.buffalonews.com",
        "shopping.nj.com",
        "signup.cleveland.com",
        "spiderbites.boston.com",
        "ssl.bbc.co.uk",
        "ssl.washingtonpost.com",
        "static.reuters.com",
        "stats.boston.com",
        "stats.cleveland.com",
        "stats.washingtonpost.com",
        "store.latimes.com",
        "subscribe.businessweek.com",
        "subscribe.washingtonpost.com",
        "syndication.boston.com",
        "syndication.washingtonpost.com",
        "tamil.thehindu.com",
        "tickets.boston.com",
        "transcripts.cnn.com",
        "ugv.abcnews.go.com",
        "update.cleveland.com",
        "videoreprints.cnbc.com",
        "washpost.bloomberg.com",
        "watchlist.cnbc.com",
        "weather.cnn.com",
        "webcast.cnbc.com",
        "www2b.abc.net.au",
        "yellowpages.washingtonpost.com"}) {
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

      TreeMap<String, String> parameters = new TreeMap<>();
      for (NameValuePair nameValue : URLEncodedUtils.parse(bigUrl.getQuery(), Charsets.UTF_8)) {
        parameters.put(nameValue.getName(), nameValue.getValue());
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
      if (domain.endsWith("abc.net.au") &&
          path.startsWith("/radio/") ||
          (path.contains("/sport/") && path.contains("/scoreboard/")) ||
          (path.contains("/sport/") && path.contains("/results/")) ||
          (path.contains("/image/"))) {
        return false;
      }
      if (domain.endsWith("abcnews.go.com") &&
          (path.startsWith("/Site/") ||
           path.startsWith("/meta/") ||
           path.startsWith("/xmldata/") ||
           path.contains("/photos/") ||
           path.contains("/videos/") ||
           path.endsWith("/video"))) {
        return false;
      }
      if (domain.endsWith("arstechnica.com") &&
          (path.startsWith("/archive/") ||
           path.startsWith("/articles/paedia/") ||
           path.startsWith("/civis/") ||
           path.startsWith("/cpu/") ||
           path.startsWith("/etc/") ||
           path.startsWith("/features/") ||
           path.startsWith("/forum/") ||
           path.startsWith("/guide/") ||
           path.startsWith("/guides/") ||
           path.startsWith("/mt-static/") ||
           path.startsWith("/old/") ||
           path.startsWith("/paedia/") ||
           path.startsWith("/reviews/") ||
           path.startsWith("/site/") ||
           path.startsWith("/sponsored/") ||
           path.startsWith("/subscriptions/") ||
           path.startsWith("/tweak/") ||
           path.startsWith("/wankerdesk/")) ||
           path.endsWith("/sendnews.php")) {
        return false;
      }
      if (domain.endsWith("bbc.co.uk") || domain.endsWith("bbc.com")) {
        if (!path.startsWith("/newsbeat/") &&
            !BBC_NEWS_ARTICLE_PATH.matcher(path).matches()) {
          return false;
        }
      }
      if (domain.endsWith("bdnews24.com")) {
        if (!parameters.containsKey("getXmlFeed") ||
            "rssfeed".equals(parameters.get("widgetName"))) {
          return false;
        }
      }
      if (domain.endsWith(".bloomberg.com") &&
          (path.startsWith("/ad-section/") ||
           path.startsWith("/apps/") ||
           path.startsWith("/billionaires/") ||
           path.startsWith("/graphics/") ||
           path.startsWith("/infographics/") ||
           path.startsWith("/news/print/") ||
           path.startsWith("/podcasts/") ||
           path.startsWith("/quote/") ||
           path.startsWith("/slideshow/") ||
           path.startsWith("/visual-data/") ||
           path.endsWith("/_/slideshow/"))) {
        return false;
      }
      if (domain.endsWith("boston.com") &&
          (path.startsWith("/boston/action/rssfeed") ||
           path.startsWith("/cars/") ||
           path.startsWith("/eom/") ||
           path.startsWith("/help/") ||
           // /jobs/ is OK!!!
           path.startsWith("/news/traffic/") ||
           path.startsWith("/radio") ||
           path.startsWith("/sports/blogs/"))) {
        return false;
      }
      if (domain.endsWith("businessweek.com") &&
          (path.startsWith("/adsections/") ||
           path.startsWith("/blogs/getting-in/") ||
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
           path.startsWith("/slideshows/") || path.equals("/slideshows") ||
           path.startsWith("/small-business/") ||
           path.startsWith("/technology/") ||
           path.startsWith("/videos/") || path.equals("/videos"))) {
        return false;
      }
      if (domain.endsWith("cbc.ca") &&
          (path.startsWith("/connects/") ||
           path.startsWith("/mediacentre/") ||
           path.startsWith("/player/") ||
           path.startsWith("/shop/"))) {
        return false;
      }
      if (domain.endsWith("cbsnews.com") &&
          (path.startsWith("/cbsnews./quote")) ||
           path.startsWith("/media/")) {
        return false;
      }
      if (domain.endsWith("channelnewsasia.com") &&
          path.contains("/wp-admin/")) {
        return false;
      }
      if (domain.endsWith("cleveland.com") &&
          (path.startsWith("/events/") || path.equals("/events") ||
           path.startsWith("/forums/") || path.equals("/forums") ||
           path.startsWith("/jobs/") || path.equals("/jobs") ||
           path.endsWith("/print.html"))) {
        return false;
      }
      if (domain.endsWith("cnbc.com") &&
          path.startsWith("/live-tv/")) {
        return false;
      }
      if (domain.endsWith("cnn.com") &&
          (path.startsWith("/CNN/") ||
           path.startsWith("/CNNI/") ||
           path.startsWith("/linkto/") ||
           path.startsWith("/services/") ||
           path.contains("/gallery/"))) {
        return false;
      }
      if (domain.endsWith(".chicagotribune.com") && path.endsWith("/comments/atom.xml")) {
        return false;
      }
      if (domain.endsWith(".chron.com") &&
          (path.endsWith("/feed") ||
              parameters.containsKey("share"))) {
        return false;
      }
      if (domain.endsWith(".cnn.com") &&
          (path.startsWith("/calculator/") ||
           path.startsWith("/data/") ||
           path.startsWith("/infographic/") ||
           path.startsWith("/interactive/") ||
           path.startsWith("/quizzes/") ||
           path.contains("/comment-page-"))) { // E.g. /comment-page-9/
        return false;
      }
      if (domain.endsWith("curbed.com") && path.equals("/search.php")) {
        return false;
      }
      if (domain.endsWith("finance.boston.com") && path.endsWith("/quote")) {
        return false;
      }
      if (domain.endsWith("forbes.com") &&
          (path.startsWith("/account/") ||
           path.startsWith("/pictures/") ||
           path.startsWith("/video/") ||
           path.matches(".*\\/[0-9]{1,3}\\/$") ||  // Page 2, 3, etc.
           path.endsWith("/print/"))) {
        return false;
      }
      if (domain.equals("investing.businessweek.com") &&
          path.startsWith("/research/stocks/snapshot/")) {
        return false;
      }
      if (domain.endsWith("latimes.com")) {
        if (path.startsWith("/classified/") ||
            path.startsWith("/shopping/")) {
          return false;
        }
        // Don't geek out on the historical articles.  If they're linked from other
        // places, great, but we don't need to dig through all of them.
        if (path.matches("^\\/[12][0-9]{3}\\/.*")) {
          return false;
        }
      }
      if (domain.endsWith("mashable.com") &&
          (path.startsWith("/login/") ||
           path.startsWith("/search/") ||
           path.startsWith("/sgs/") ||
           path.startsWith("/media-summit/"))) {
        return false;
      }
      if (domain.endsWith("money.usnews.com") && path.startsWith("/529s/")) {
        return false;
      }
      if (domain.endsWith("money.cnn.com") &&
          (path.startsWith("/data/") ||
           path.startsWith("/galleries/") ||
           path.startsWith("/gallery/") ||
           path.startsWith("/quote/") ||
           path.startsWith("/magazines/") ||
           path.startsWith("/tag/") ||
           path.startsWith("/tools/"))) {
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
      if (domain.endsWith("recode.net") &&
          (path.startsWith("/events/") ||
           path.startsWith("/follow-us/") ||
           path.startsWith("/next/") ||
           path.startsWith("/sponsored-content/") ||
           path.startsWith("/video/") ||
           path.startsWith("/wp-admin/") ||
           parameters.containsKey("share"))) {
        return false;
      }
      if (domain.endsWith("sfgate.com") &&
          (path.startsWith("/merge/") ||
           parameters.containsKey("share"))) {
        return false;
      }
      if (domain.endsWith("siliconbeat.com") &&
          parameters.containsKey("share")) {
        return false;
      }
      if (domain.endsWith("sports.chron.com") &&
          path.startsWith("/merge/")) {
        return false;
      }
      if (domain.endsWith("techcrunch.com") &&
          (path.startsWith("/event-type/") ||
           path.startsWith("/events/") ||
           path.startsWith("/gallery/") ||
           path.startsWith("/rss/"))) {
        return false;
      }
      if (domain.endsWith("telegraph.co.uk") && path.startsWith("/sponsored/")) {
        return false;
      }
      if (domain.endsWith("theverge.com") &&
          (path.startsWith("/forums/") ||
           path.startsWith("/jobs") ||
           path.startsWith("/search") ||
           path.startsWith("/video/"))) {
        return false;
      }
      if (domain.endsWith("usatoday.com") && path.startsWith("/marketing/rss/")) {
        return false;
      }
      if (domain.endsWith("washingtonpost.com") &&
          (path.startsWith("/capitalweathergang/") ||
           path.startsWith("/newssearch/") ||
           path.contains("/wp-dyn/") ||
           path.contains("/wp-srv/") ||
           path.endsWith("/wp-dyn") ||
           path.endsWith("_category.html") ||
           path.endsWith("/post.php"))) {
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
    Map<String, String> urlsToDelete = Maps.newHashMap();
    for (Url url : Database.with(Url.class).get(
        new QueryOption.WhereNotLike("url", "%//twitter.com/%"))) {
      String urlStr = url.getUrl();
      if ((!isOkay(urlStr) || !urlStr.equals(UrlCleaner.clean(urlStr)))) {
        urlsToDelete.put(urlStr, url.getId());
      }
      if (urlsToDelete.size() == 100 || url == null) {
        List<String> urls = Lists.newArrayList();
        List<String> ids = Lists.newArrayList();
        for (Map.Entry<String, String> urlToDelete : urlsToDelete.entrySet()) {
          System.out.println("Deleting url: " + urlToDelete.getKey());
          urls.add(urlToDelete.getKey());
          ids.add(urlToDelete.getValue());
        }
        System.out.println("Deleted " + Database.with(Article.class).delete(ids) + " articles");
        System.out.println("Deleted " + ArticleKeywords.deleteForUrlIds(ids) + " article keywords");
        System.out.println("Deleted " + Links.deleteIds(ids) + " links");
        System.out.println("Deleted " + Database.with(Url.class).delete(urls) + " urls");
        urlsToDelete.clear();
      }
    }
  }
}

