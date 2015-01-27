package com.janknspank.rss;

import java.net.MalformedURLException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Iterables;
import com.janknspank.common.ArticleUrlDetector;
import com.janknspank.common.DateParser;
import com.janknspank.common.UrlCleaner;
import com.janknspank.common.UrlWhitelist;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.GuidFactory;
import com.janknspank.data.Urls;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.Core.Url;

/**
 * Iterates through all the RSS of various sites we like to crawl and pushes
 * any discovered articles to the database.
 */
public class RssCrawler {
  private final Fetcher fetcher = new Fetcher();
  private static final String[] RSS_URLS = new String[] {
    "http://america.aljazeera.com/content/ajam/articles.rss",
    "http://bdnews24.com/?widgetName=rssfeed&widgetId=1150&getXmlFeed=true",
    "http://blog.cleveland.com/business_impact/atom.xml",
    "http://blog.cleveland.com/business_impact/technology/atom.xml",
    "http://blog.cleveland.com/realtimenews/atom.xml",
    "http://curbed.com/atom.xml",
    "http://feeds.abcnews.com/abcnews/internationalheadlines",
    "http://feeds.abcnews.com/abcnews/moneyheadlines",
    "http://feeds.abcnews.com/abcnews/politicsheadlines",
    "http://feeds.abcnews.com/abcnews/technologyheadlines",
    "http://feeds.abcnews.com/abcnews/usheadlines",
    "http://feeds.bbci.co.uk/news/business/rss.xml",
    "http://feeds.bbci.co.uk/news/politics/rss.xml",
    "http://feeds.bbci.co.uk/news/rss.xml",
    "http://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
    "http://feeds.bbci.co.uk/news/technology/rss.xml",
    "http://feeds.bbci.co.uk/news/world/rss.xml",
    "http://feeds.mercurynews.com/mngi/rss/CustomRssServlet/568/200735.xml",
    "http://feeds.mercurynews.com/mngi/rss/CustomRssServlet/568/200737.xml",
    "http://feeds.washingtonpost.com/rss/business",
    "http://la.curbed.com/atom.xml",
    "http://ny.curbed.com/atom.xml",
    "http://recode.net/category/general/feed/",
    "http://recode.net/feed/",
    "http://rss.cnn.com/rss/cnn_tech.rss",
    "http://rss.cnn.com/rss/cnn_topstories.rss",
    "http://rss.cnn.com/rss/cnn_world.rss",
    "http://rss.cnn.com/rss/money_latest.rss",
    "http://rss.nytimes.com/services/xml/rss/nyt/Business.xml",
    "http://rss.nytimes.com/services/xml/rss/nyt/Economy.xml",
    "http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
    "http://rss.nytimes.com/services/xml/rss/nyt/InternationalHome.xml",
    "http://rss.nytimes.com/services/xml/rss/nyt/PersonalTech.xml",
    "http://rss.nytimes.com/services/xml/rss/nyt/Politics.xml",
    "http://rss.nytimes.com/services/xml/rss/nyt/Science.xml",
    "http://rss.nytimes.com/services/xml/rss/nyt/SmallBusiness.xml",
    "http://rss.nytimes.com/services/xml/rss/nyt/Technology.xml",
    "http://seattle.curbed.com/atom.xml",
    "http://sf.curbed.com/atom.xml",
    "http://www.abc.net.au/news/feed/45910/rss.xml", // "Top".
    "http://www.abc.net.au/news/feed/51120/rss.xml", // "Just in".
    "http://www.abc.net.au/news/feed/51892/rss.xml", // Business.
    "http://www.abc.net.au/news/feed/52278/rss.xml", // World.
    "http://www.buffalonews.com/section/rssGen?profileID=1109&profileName=Top%20Stories",
    "http://www.cbc.ca/cmlink/rss-business",
    "http://www.cbc.ca/cmlink/rss-technology",
    "http://www.cbc.ca/cmlink/rss-topstories",
    "http://www.cbsnews.com/latest/rss/main",
    "http://www.cbsnews.com/latest/rss/moneywatch",
    "http://www.cbsnews.com/latest/rss/tech",
    "http://www.cbsnews.com/latest/rss/us",
    "http://www.channelnewsasia.com/starterkit/servlet/cna/rss/business.xml",
    "http://www.channelnewsasia.com/starterkit/servlet/cna/rss/home.xml",
    "http://www.channelnewsasia.com/starterkit/servlet/cna/rss/world.xml",
    "http://www.chron.com/rss/feed/AP-Technology-and-Science-266.php",
    "http://www.chron.com/rss/feed/Business-287.php",
    "http://www.chron.com/rss/feed/News-270.php",
    "http://www.cnbc.com/id/100003114/device/rss/rss.html", // Top.
    "http://www.cnbc.com/id/10001147/device/rss/rss.html", // Business.
    "http://www.cnbc.com/id/19854910/device/rss/rss.html", // Tech.
    "http://www.forbes.com/business/feed/",
    "http://www.forbes.com/most-popular/feed/",
    "http://www.forbes.com/real-time/feed2/",
    "http://www.forbes.com/technology/feed/",
    "http://www.latimes.com/business/rss2.0.xml",
    "http://www.latimes.com/business/technology/rss2.0.xml",
    "http://www.latimes.com/nation/rss2.0.xml",
    "http://www.latimes.com/opinion/editorials/rss2.0.xml",
    "http://www.latimes.com/rss2.0.xml",
    "http://www.latimes.com/science/rss2.0.xml",
    "http://www.nytimes.com/services/xml/rss/nyt/JobMarket.xml",
    "http://www.sfgate.com/default/feed/City-Insider-Blog-573.php",
    "http://www.sfgate.com/rss/feed/Page-One-News-593.php",
    "http://www.sfgate.com/rss/feed/Tech-News-449.php",
    "http://www.theverge.com/mobile/rss/index.xml",
    "http://www.theverge.com/rss/index.xml"
  };

  private String getArticleUrl(Node itemNode) {
    List<Node> linkNodes = itemNode.findAll("atom:link");
    linkNodes.addAll(itemNode.findAll("link"));
    String articleUrl = null;
    try {
      if (linkNodes.size() > 0) {
        String href = linkNodes.get(0).getAttributeValue("href");
        if (href != null) {
          articleUrl = href;
        } else {
          // Some sites (e.g. mercurynews.com) put the URL as character data inside
          // the <link>...</link> element.
          String maybeUrl = linkNodes.get(0).getFlattenedText();
          if (ArticleUrlDetector.isArticle(maybeUrl)) {
            articleUrl = maybeUrl;
          } else {
            // Then, some sites put the URL in the GUID, while using 'link' for
            // a trackable click-through URL that doesn't tell us anything and
            // is not canonicalized.  See if we can use the GUID URL.
            List<Node> guidNodes = itemNode.findAll("guid");
            if (guidNodes.size() > 0) {
              maybeUrl = guidNodes.get(0).getFlattenedText();
              if (ArticleUrlDetector.isArticle(maybeUrl)) {
                articleUrl = maybeUrl;
              }
            }
          }
        }
      }
      if (UrlWhitelist.isOkay(articleUrl)) {
        return UrlCleaner.clean(articleUrl);
      }
    } catch (MalformedURLException e) {
      // Ehh OK, ignore it.
    }
    return null;
  }

  private Long getArticleDate(Node itemNode) {
    Long millis = DateParser.parseDateFromUrl(getArticleUrl(itemNode), true /* allowMonth */);
    List<Node> dateNodes = itemNode.findAll("pubDate");
    dateNodes.addAll(itemNode.findAll("published"));
    if (dateNodes.size() > 0) {
      Long rssDate = DateParser.parseDateTime(dateNodes.get(0).getFlattenedText());
      millis = (rssDate == null) ? millis : rssDate;
    }
    return millis;
  }

  private void saveArticle(String url, Long date) {
    Url existing;
    try {
      existing = Urls.getByUrl(url);
      if (existing == null) {
        Database.insert(Url.newBuilder()
            .setUrl(url)
            .setId(GuidFactory.generate())
            .setTweetCount(0)
            .setDiscoveryTime(System.currentTimeMillis())
            .setCrawlPriority(Urls.getCrawlPriority(url, date))
            .build());
      }
    } catch (DataInternalException|ValidationException e) {
      // Oh well, it's just RSS.  Print it out at least, so we can debug it.
      e.printStackTrace();
    }
  }

  public void crawl(String rssUrl) {
    FetchResponse fetchResponse;
    try {
      fetchResponse = fetcher.fetch(rssUrl);
      if (fetchResponse.getStatusCode() == HttpServletResponse.SC_OK) {
        DocumentNode documentNode = DocumentBuilder.build(rssUrl, fetchResponse.getReader());
        for (Node itemNode : Iterables.concat(
            documentNode.findAll("item"),
            documentNode.findAll("entry"))) {
          String articleUrl = getArticleUrl(itemNode);
          if (ArticleUrlDetector.isArticle(articleUrl)) {
            Long millis = getArticleDate(itemNode);
            saveArticle(articleUrl, millis);
          }
        }
      }
    } catch (FetchException | ParserException e) {
      e.printStackTrace();
    }
  }

  public static void main(String args[]) {
    RssCrawler crawler = new RssCrawler();
    for (String rssUrl : RSS_URLS) {
      System.out.println("***** RSS FILE: " + rssUrl);
      crawler.crawl(rssUrl);
    }
  }
}
