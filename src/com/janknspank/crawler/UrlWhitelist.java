package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.bizness.Links;
import com.janknspank.common.Logger;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.CrawlProto.ContentSite;
import com.janknspank.proto.CrawlProto.ContentSite.PathBlacklist;
import com.janknspank.proto.CrawlProto.ContentSite.PathBlacklist.Location;
import com.janknspank.proto.CrawlProto.CrawlInstructions;

public class UrlWhitelist {
  public static final Predicate<String> PREDICATE = new Predicate<String>() {
    @Override
    public boolean apply(String url) {
      return UrlWhitelist.isOkay(url);
    }
  };

  public static final CrawlInstructions CRAWL_INSTRUCTIONS = CrawlInstructions.newBuilder()
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("abc.net.au")
          .addStartUrl("http://www.abc.net.au/")
          .addSubdomainBlacklist("about.abc.net.au")
          .addSubdomainBlacklist("iview.abc.net.au")
          .addSubdomainBlacklist("mobile.abc.net.au")
          .addSubdomainBlacklist("mobile-phones.smh.com.au")
          .addSubdomainBlacklist("nucwed.aus.aunty.abc.net.au")
          .addSubdomainBlacklist("search.abc.net.au")
          .addSubdomainBlacklist("shop.abc.net.au")
          .addSubdomainBlacklist("www2b.abc.net.au")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/radio/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("\\/sport\\/.*\\/scoreboard\\/")
              .setLocation(Location.REGEX_FIND))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("\\/sport\\/.*\\/results\\/")
              .setLocation(Location.REGEX_FIND))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/image/")
              .setLocation(Location.CONTAINS))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("abcnews.go.com")
          .addStartUrl("http://www.abcnews.go.com")
          .addSubdomainBlacklist("forums.abcnews.go.com")
          .addSubdomainBlacklist("ugv.abcnews.go.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/Site/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/meta/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/xmldata/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/photos/")
              .setLocation(Location.CONTAINS))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("advice.careerbuilder.com")
          .addStartUrl("http://www.advice.careerbuilder.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("allthingsd.com")
          .addStartUrl("http://www.allthingsd.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("america.aljazeera.com")
          .addStartUrl("http://america.aljazeera.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("arstechnica.com")
          .addStartUrl("http://www.arstechnica.com")
          .addSubdomainBlacklist("demiurge.arstechnica.com")
          .addSubdomainBlacklist("episteme.arstechnica.com")
          .addSubdomainBlacklist("feeds.arstechnica.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/archive/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/articles/paedia/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/civis/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/cpu/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/etc/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/features/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/forum/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/guide/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/guides/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/mt-static/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/old/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/paedia/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/reviews/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/site/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/sponsored/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/subscriptions/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/tweak/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/wankerdesk/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/sendnews.php")
              .setLocation(Location.ENDS_WITH))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("bbc.co.uk")
          .addAkaRootDomain("bbc.com")
          .addStartUrl("http://www.bbc.co.uk")
          .addStartUrl("http://www.bbc.com")
          .addSubdomainBlacklist("iplayerhelp.external.bbc.co.uk")
          .addSubdomainBlacklist("faq.external.bbc.co.uk")
          .addSubdomainBlacklist("newsvote.bbc.co.uk")
          .addSubdomainBlacklist("ssl.bbc.co.uk")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/guides/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/iplayer/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/mundo/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/programmes/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/portuguese/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/sport/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/travel/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/webwise/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("bbc.com")
          .addStartUrl("http://www.bbc.com")
          .addSubdomainBlacklist("m.bbc.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("bdnews24.com")
          .addStartUrl("http://www.bdnews24.com")
          .addSubdomainBlacklist("ads.bdnews24.com")
          .addSubdomainBlacklist("revive.bdnews24.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("bloomberg.com")
          .addStartUrl("http://www.bloomberg.com")
          .addSubdomainBlacklist("bdn-ak.bloomberg.com")
          .addSubdomainBlacklist("bdns.bloomberg.com")
          .addSubdomainBlacklist("connect.bloomberg.com")
          .addSubdomainBlacklist("m.bloomberg.com")
          .addSubdomainBlacklist("jobs.bloomberg.com")
          .addSubdomainBlacklist("jobsearch.bloomberg.com")
          .addSubdomainBlacklist("login.bloomberg.com")
          .addSubdomainBlacklist("media.bloomberg.com")
          .addSubdomainBlacklist("mobile.bloomberg.com")
          .addSubdomainBlacklist("open.bloomberg.com")
          .addSubdomainBlacklist("search.bloomberg.com")
          .addSubdomainBlacklist("search1.bloomberg.com")
          .addSubdomainBlacklist("service.bloomberg.com")
          .addSubdomainBlacklist("washpost.bloomberg.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/ad-section/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/apps/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/billionaires/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/graphics/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/infographics/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/news/print/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/podcasts/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/quote/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/slideshow/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/visual-data/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/_/slideshow/")
              .setLocation(Location.ENDS_WITH))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("boston.com")
          .addStartUrl("http://www.boston.com")
          .addSubdomainBlacklist("blogcabin.boston.com")
          .addSubdomainBlacklist("calendar.boston.com")
          .addSubdomainBlacklist("circulars.boston.com")
          .addSubdomainBlacklist("finds.boston.com")
          .addSubdomainBlacklist("healthguide.boston.com")
          .addSubdomainBlacklist("listings.boston.com")
          .addSubdomainBlacklist("members.boston.com")
          .addSubdomainBlacklist("r.prdedit.boston.com")
          .addSubdomainBlacklist("realestate.boston.com")
          .addSubdomainBlacklist("search.boston.com")
          .addSubdomainBlacklist("scene.boston.com")
          .addSubdomainBlacklist("spiderbites.boston.com")
          .addSubdomainBlacklist("stats.boston.com")
          .addSubdomainBlacklist("syndication.boston.com")
          .addSubdomainBlacklist("tickets.boston.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/boston/action/rssfeed"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/cars/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/eom/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/help/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/news/traffic/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/radio"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/sports/blogs/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/quote")
              .setLocation(Location.ENDS_WITH))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("breitbart.com")
          .addStartUrl("http://www.breitbart.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("buffalonews.com")
          .addStartUrl("http://www.buffalonews.com")
          .addSubdomainBlacklist("services.buffalonews.com")
          .addSubdomainBlacklist("shopping.buffalonews.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("businessinsider.com")
          .addStartUrl("http://www.businessinsider.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("businessweek.com")
          .addStartUrl("http://www.businessweek.com")
          .addSubdomainBlacklist("bwso.businessweek.com")
          .addSubdomainBlacklist("forums.businessweek.com")
          .addSubdomainBlacklist("images.businessweek.com")
          .addSubdomainBlacklist("jobs.businessweek.com")
          .addSubdomainBlacklist("mobile.businessweek.com")
          .addSubdomainBlacklist("secure.businessweek.com")
          .addSubdomainBlacklist("subscribe.businessweek.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/adsections/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/blogs/getting-in/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/bschools/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/business-schools/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/companies-and-industries/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/global-economics/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/innovation-and-design/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/innovation/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/interactive/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/lifestyle/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/markets-and-finance/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/photos/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/politics-and-policy/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/printer/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/quiz/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/reports/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/research/stocks/snapshot/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/slideshows/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/slideshows")
              .setLocation(Location.EQUALS))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/small-business/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/technology/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/videos/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/videos")
              .setLocation(Location.EQUALS))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("cbc.ca")
          .addStartUrl("http://www.cbc.ca")
          .addSubdomainBlacklist("music.cbc.ca")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/connects/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/mediacentre/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/player/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/shop/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("cbsnews.com")
          .addStartUrl("http://www.cbsnews.com")
          .addSubdomainBlacklist("cbsn.cbsnews.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/cbsnews./quote"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/media/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("channelnewsasia.com")
          .addStartUrl("http://www.channelnewsasia.com")
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/wp-admin/")
              .setLocation(Location.CONTAINS))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("chicagotribune.com")
          .addStartUrl("http://www.chicagotribune.com")
          .addSubdomainBlacklist("advertising.chicagotribune.com")
          .addSubdomainBlacklist("apps.chicagotribune.com")
          .addSubdomainBlacklist("members.chicagotribune.com")
          .addSubdomainBlacklist("placeanad.chicagotribune.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("chron.com")
          .addStartUrl("http://www.chron.com")
          .addSubdomainBlacklist("cars.chron.com")
          .addSubdomainBlacklist("fangear.chron.com")
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/feed")
              .setLocation(Location.ENDS_WITH))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/merge/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("cleveland.com")
          .addStartUrl("http://www.cleveland.com")
          .addSubdomainBlacklist("autos.cleveland.com")
          .addSubdomainBlacklist("blog.cleveland.com")
          .addSubdomainBlacklist("businessfinder.cleveland.com")
          .addSubdomainBlacklist("connect.cleveland.com")
          .addSubdomainBlacklist("findnsave.cleveland.com")
          .addSubdomainBlacklist("mobilejobs.cleveland.com")
          .addSubdomainBlacklist("mobileobits.cleveland.com")
          .addSubdomainBlacklist("photos.cleveland.com")
          .addSubdomainBlacklist("realestate.cleveland.com")
          .addSubdomainBlacklist("cinesport.cleveland.com")
          .addSubdomainBlacklist("classifieds.cleveland.com")
          .addSubdomainBlacklist("foreclosures.cleveland.com")
          .addSubdomainBlacklist("highschoolsports.cleveland.com")
          .addSubdomainBlacklist("jobs.cleveland.com")
          .addSubdomainBlacklist("m.cleveland.com")
          .addSubdomainBlacklist("search.cleveland.com")
          .addSubdomainBlacklist("signup.cleveland.com")
          .addSubdomainBlacklist("stats.cleveland.com")
          .addSubdomainBlacklist("update.cleveland.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/events/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/events")
              .setLocation(Location.EQUALS))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/forums/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/forums")
              .setLocation(Location.EQUALS))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/jobs/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/jobs")
              .setLocation(Location.EQUALS))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/print.html")
              .setLocation(Location.ENDS_WITH))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("cnbc.com")
          .addStartUrl("http://www.cnbc.com")
          .addSubdomainBlacklist("data.cnbc.com")
          .addSubdomainBlacklist("futuresnow.cnbc.com")
          .addSubdomainBlacklist("portfolio.cnbc.com")
          .addSubdomainBlacklist("pro.cnbc.com")
          .addSubdomainBlacklist("watchlist.cnbc.com")
          .addSubdomainBlacklist("videoreprints.cnbc.com")
          .addSubdomainBlacklist("webcast.cnbc.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/live-tv/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("cnn.com")
          .addStartUrl("http://www.cnn.com")
          .addSubdomainBlacklist("arabic.cnn.com")
          .addSubdomainBlacklist("audience.cnn.com")
          .addSubdomainBlacklist("blogs.cnn.com")
          .addSubdomainBlacklist("cnnespanol.cnn.com")
          .addSubdomainBlacklist("games.cnn.com")
          .addSubdomainBlacklist("inhealth.cnn.com")
          .addSubdomainBlacklist("ireport.cnn.com")
          .addSubdomainBlacklist("mexico.cnn.com")
          .addSubdomainBlacklist("partners.cnn.com")
          .addSubdomainBlacklist("rss.cnn.com")
          .addSubdomainBlacklist("search.cnn.com")
          .addSubdomainBlacklist("transcripts.cnn.com")
          .addSubdomainBlacklist("weather.cnn.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/CNN/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/CNNI/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/calculator/"))
          // E.g. /comment-page-9/
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/comment-page-")
              .setLocation(Location.CONTAINS))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/data/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/gallery/")
              .setLocation(Location.CONTAINS))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/infographic/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/interactive/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/linkto/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/quizzes/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/services/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("curbed.com")
          .addStartUrl("http://www.curbed.com")
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/search.php")
              .setLocation(Location.ENDS_WITH))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("engadget.com")
          .addStartUrl("http://www.engadget.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("fastcompany.com")
          .addStartUrl("http://www.fastcompany.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("forbes.com")
          .addStartUrl("http://www.forbes.com")
          .addSubdomainBlacklist("blogs.forbes.com") // This is their account management site.
          .addSubdomainBlacklist("related.forbes.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/account/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/pictures/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/video/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle(".*\\/[0-9]{1,3}\\/$") // Page 2, 3, etc.
              .setLocation(Location.REGEX_MATCH))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/print/")
              .setLocation(Location.ENDS_WITH))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("gizmodo.com")
          .addStartUrl("http://www.gizmodo.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("latimes.com")
          .addStartUrl("http://www.latimes.com")
          .addSubdomainBlacklist("advertise.latimes.com")
          .addSubdomainBlacklist("classifieds.latimes.com")
          .addSubdomainBlacklist("digitalservices.latimes.com")
          .addSubdomainBlacklist("datadesk.latimes.com")
          .addSubdomainBlacklist("dailydeals.latimes.com")
          .addSubdomainBlacklist("discussions.latimes.com")
          .addSubdomainBlacklist("documents.latimes.com")
          .addSubdomainBlacklist("ee.latimes.com")
          .addSubdomainBlacklist("framework.latimes.com")
          .addSubdomainBlacklist("games.latimes.com")
          .addSubdomainBlacklist("graphics.latimes.com")
          .addSubdomainBlacklist("guides.latimes.com")
          .addSubdomainBlacklist("ilivehere.latimes.com")
          .addSubdomainBlacklist("localdeals.latimes.com")
          .addSubdomainBlacklist("marketplace.latimes.com")
          .addSubdomainBlacklist("marketplaceads.latimes.com")
          .addSubdomainBlacklist("mediakit.latimes.com")
          .addSubdomainBlacklist("membership.latimes.com")
          .addSubdomainBlacklist("myaccount2.latimes.com")
          .addSubdomainBlacklist("placeanad.latimes.com")
          .addSubdomainBlacklist("projects.latimes.com")
          .addSubdomainBlacklist("recipes.latimes.com")
          .addSubdomainBlacklist("schools.latimes.com")
          .addSubdomainBlacklist("store.latimes.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/classified/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/shopping/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("mashable.com")
          .addStartUrl("http://www.mashable.com")
          .addSubdomainBlacklist("events.mashable.com")
          .addSubdomainBlacklist("findjobs.mashable.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/login/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/search/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/sgs/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/media-summit/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("medium.com")
          .addStartUrl("http://www.medium.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("mercurynews.com")
          .addStartUrl("http://www.mercurynews.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("money.cnn.com")
          .addStartUrl("http://money.cnn.com")
          .addSubdomainBlacklist("cgi.money.cnn.com")
          .addSubdomainBlacklist("jobsearch.money.cnn.com")
          .addSubdomainBlacklist("portfolio.money.cnn.com")
          .addSubdomainBlacklist("realestate.money.cnn.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/data/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/galleries/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/gallery/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/quote/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/magazines/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/tag/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/tools/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("nytimes.com")
          .addStartUrl("http://www.nytimes.com")
          .addSubdomainBlacklist("jobmarket.nytimes.com")
          .addSubdomainBlacklist("homedelivery.nytimes.com")
          .addSubdomainBlacklist("mobile.nytimes.com")
          .addSubdomainBlacklist("autos.nytimes.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/content/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/rss")
              .setLocation(Location.EQUALS))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/rss/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/ref/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/adx/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/content/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/register")
              .setLocation(Location.EQUALS))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/services/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/slideshow/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/subscriptions/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/times-journeys/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("pcmag.com")
          .addStartUrl("http://www.pcmag.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("recode.net")
          .addStartUrl("http://recode.net")
          .addSubdomainBlacklist("on.recode.net")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/events/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/follow-us/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/next/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/sponsored-content/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/video/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/wp-admin/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("redherring.com")
          .addStartUrl("http://www.redherring.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("sfexaminer.com")
          .addStartUrl("http://www.sfexaminer.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("sfgate.com")
          .addStartUrl("http://www.sfgate.com")
          .addSubdomainBlacklist("cars.sfgate.com")
          .addSubdomainBlacklist("events.sfgate.com")
          .addSubdomainBlacklist("extras.sfgate.com")
          .addSubdomainBlacklist("fanshop.sfgate.com")
          .addSubdomainBlacklist("homeguides.sfgate.com")
          .addSubdomainBlacklist("markets.sfgate.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/merge/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("siliconbeat.com")
          .addStartUrl("http://www.siliconbeat.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("slate.com")
          .addStartUrl("http://www.slate.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("startupworkout.com")
          .addStartUrl("http://www.startupworkout.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("techcrunch.com")
          .addStartUrl("http://www.techcrunch.com")
          .addSubdomainBlacklist("disrupt.techcrunch.com")
          .addSubdomainBlacklist("jp.techcrunch.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/event-type/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/events/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/gallery/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/rss/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("techmeme.com")
          .addStartUrl("http://www.techmeme.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("technologyreview.com")
          .addStartUrl("http://www.technologyreview.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("telegraph.co.uk")
          .addStartUrl("http://www.telegraph.co.uk")
          .addSubdomainBlacklist("dating.telegraph.co.uk")
          .addSubdomainBlacklist("gardenshop.telegraph.co.uk")
          .addSubdomainBlacklist("shop.telegraph.co.uk")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/sponsored/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("theguardian.com")
          .addStartUrl("http://www.theguardian.com")
          .addSubdomainBlacklist("discussion.theguardian.com")
          .addSubdomainBlacklist("id.theguardian.com")
          .addSubdomainBlacklist("profile.theguardian.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("thenextweb.com")
          .addStartUrl("http://www.thenextweb.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("theverge.com")
          .addStartUrl("http://www.theverge.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/forums/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/jobs"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/search"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/video/"))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("venturebeat.com")
          .addStartUrl("http://www.venturebeat.com")
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("washingtonpost.com")
          .addStartUrl("http://www.washingtonpost.com")
          .addSubdomainBlacklist("account.washingtonpost.com")
          .addSubdomainBlacklist("advertising.washingtonpost.com")
          .addSubdomainBlacklist("apps.washingtonpost.com")
          .addSubdomainBlacklist("feeds.washingtonpost.com")
          .addSubdomainBlacklist("findnsave.washingtonpost.com")
          .addSubdomainBlacklist("games.washingtonpost.com")
          .addSubdomainBlacklist("js.washingtonpost.com")
          .addSubdomainBlacklist("m.washingtonpost.com")
          .addSubdomainBlacklist("nie.washingtonpost.com")
          .addSubdomainBlacklist("realestate.washingtonpost.com")
          .addSubdomainBlacklist("stats.washingtonpost.com")
          .addSubdomainBlacklist("syndication.washingtonpost.com")
          .addSubdomainBlacklist("ssl.washingtonpost.com")
          .addSubdomainBlacklist("subscribe.washingtonpost.com")
          .addSubdomainBlacklist("yellowpages.washingtonpost.com")
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/capitalweathergang/"))
          .addPathBlacklist(PathBlacklist.newBuilder().setNeedle("/newssearch/"))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/wp-dyn/")
              .setLocation(Location.CONTAINS))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/wp-srv/")
              .setLocation(Location.CONTAINS))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/wp-dyn")
              .setLocation(Location.ENDS_WITH))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("_category.html")
              .setLocation(Location.ENDS_WITH))
          .addPathBlacklist(PathBlacklist.newBuilder()
              .setNeedle("/post.php")
              .setLocation(Location.ENDS_WITH))
          .build())
      .addContentSite(ContentSite.newBuilder()
          .setRootDomain("wired.com")
          .addStartUrl("http://www.wired.com")
          .build())
      .build();

  private static final Logger LOG = new Logger(UrlWhitelist.class);

  public static ContentSite getContentSiteForUrl(URL url) {
    String domain = url.getHost();
    while (domain.contains(".")) {
      for (ContentSite contentSite : CRAWL_INSTRUCTIONS.getContentSiteList()) {
        if (contentSite.getRootDomain().equals(domain)
            || Iterables.contains(contentSite.getAkaRootDomainList(), domain)) {
          return contentSite;
        }
      }
      domain = domain.substring(domain.indexOf(".") + 1);
    }
    return null;
  }

  public static boolean isOkay(String urlString) {
    try {
      URL url = new URL(urlString);

      // Length exclusion (this is the longest a URL can be in our database).
      if (urlString.length() > 767) {
        return false;
      }

      ContentSite contentSite = getContentSiteForUrl(url);
      if (contentSite == null) {
        return false;
      }
      String domain = url.getHost();
      for (String blacklistedDomain : contentSite.getSubdomainBlacklistList()) {
        if (domain.equals(blacklistedDomain)) {
          return false;
        }
      }
      String path = url.getPath();
      for (PathBlacklist blacklistedPath : contentSite.getPathBlacklistList()) {
        switch (blacklistedPath.getLocation()) {
          case EQUALS:
            if (path.equals(blacklistedPath.getNeedle())) {
              return false;
            }
            break;

          case STARTS_WITH:
            if (path.startsWith(blacklistedPath.getNeedle())) {
              return false;
            }
            break;

          case ENDS_WITH:
            if (path.endsWith(blacklistedPath.getNeedle())) {
              return false;
            }
            break;

          case CONTAINS:
            if (path.contains(blacklistedPath.getNeedle())) {
              return false;
            }
            break;

          case REGEX_FIND:
            if (Pattern.compile(blacklistedPath.getNeedle()).matcher(path).find()) {
              return false;
            }
            break;

          case REGEX_MATCH:
            if (Pattern.compile(blacklistedPath.getNeedle()).matcher(path).matches()) {
              return false;
            }
            break;
        }
      }

      // Global path exclusions.
      if (domain.contains("video.") ||
          domain.contains("videos.") ||
          path.startsWith("/cgi-bin/") ||
          path.contains("/video/") ||
          path.contains("/videos/")) {
        return false;
      }

      // Global extension exclusions.
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

      // Everything passed!
      return true;

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
        deleteUrlMap(urlsToDelete);
        urlsToDelete.clear();
      }
    }
    deleteUrlMap(urlsToDelete);
  }

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
}
