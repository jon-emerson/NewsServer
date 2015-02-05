package com.janknspank.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import twitter4j.JSONObject;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.database.Database;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;

/**
 * Analyzes the social data for all articles in the database and generates
 * average share counts for each news-hosting domain.  The averages are
 * generated for articles 1 day old, 3 days old, 7 days old, and all-time.
 * This will allow us to see how a new article's trajectory compares to
 * other popular articles of the same age.
 * 
 * NOTE(jonemerson): Currently only handles Facebook share counts.
 */
public class ShareNormalizationTableBuilder {
  private static final long THREE_HOURS_MILLIS = TimeUnit.HOURS.toMillis(3);
  private static final long ONE_DAY_MILLIS = TimeUnit.DAYS.toMillis(1);
  private static final long THREE_DAYS_MILLIS = TimeUnit.DAYS.toMillis(3);
  private static final long SEVEN_DAYS_MILLIS = TimeUnit.DAYS.toMillis(7);

  public static class ShareNormalizationTable {
    private static final Map<String, String> DOMAIN_CANONICALIZATIONS =
        new ImmutableMap.Builder<String, String>()
            .put("edition.cnn.com", "cnn.com")
            .put("us.cnn.com", "cnn.com")
            .put("economy.money.cnn.com", "money.cnn.com")
            .put("knowmore.washingtonpost.com", "washingtonpost.com")
            .build();

    public final Multiset<String> articlesInThreeHours = HashMultiset.create();
    public final Multiset<String> sharesInThreeHours = HashMultiset.create();
    public final Multiset<String> articlesInOneDay = HashMultiset.create();
    public final Multiset<String> sharesInOneDay = HashMultiset.create();
    public final Multiset<String> articlesInThreeDays = HashMultiset.create();
    public final Multiset<String> sharesInThreeDays = HashMultiset.create();
    public final Multiset<String> articlesInSevenDays = HashMultiset.create();
    public final Multiset<String> sharesInSevenDays = HashMultiset.create();
    public final Multiset<String> articlesAllTime = HashMultiset.create();
    public final Multiset<String> sharesAllTime = HashMultiset.create();

    public ShareNormalizationTable(Iterable<Article> articles) throws MalformedURLException {
      for (Article article : articles) {
        String domain = getDomainForArticle(article);
        SocialEngagement engagement = SocialEngagements.getForArticle(article, Site.FACEBOOK);
        if (engagement != null) {
          Long ageInMillis = getAgeInMillis(article, engagement);
          if (ageInMillis != null) {
            if (ageInMillis < THREE_HOURS_MILLIS) {
              articlesInThreeHours.add(domain);
              sharesInThreeHours.add(domain, (int) engagement.getShareCount());
            }
            if (ageInMillis < ONE_DAY_MILLIS) {
              articlesInOneDay.add(domain);
              sharesInOneDay.add(domain, (int) engagement.getShareCount());
            }
            if (ageInMillis < THREE_DAYS_MILLIS) {
              articlesInThreeDays.add(domain);
              sharesInThreeDays.add(domain, (int) engagement.getShareCount());
            }
            if (ageInMillis < SEVEN_DAYS_MILLIS) {
              articlesInSevenDays.add(domain);
              sharesInSevenDays.add(domain, (int) engagement.getShareCount());
            }
            articlesAllTime.add(domain);
            sharesAllTime.add(domain, (int) engagement.getShareCount());
          }
        }
      }
    }

    /**
     * For a given article, return a canonical domain name for its host.
     * "www."-style prefixes are dropped.
     */
    public static String getDomainForArticle(Article article) throws MalformedURLException {
      URL url = new URL(article.getUrl());
      String host = url.getHost();
      if (host.startsWith("www.") || host.startsWith("www1.")) {
        host = host.substring(host.indexOf(".") + 1);
      }
      if (DOMAIN_CANONICALIZATIONS.containsKey(host)) {
        host = DOMAIN_CANONICALIZATIONS.get(host);
      }
      return host;
    }
  }

  /**
   * Returns the amount of time between when this article was published and
   * when we last received Facebook share data for it.  Returns null of the
   * received article has no social data.
   */
  public static Long getAgeInMillis(Article article, SocialEngagement engagement) {
    return Math.max(TimeUnit.HOURS.toMillis(2),
        engagement.getCreateTime() - article.getPublishedTime());
  }

  public static void main(String args[]) throws Exception {
    ShareNormalizationTable table = new ShareNormalizationTable(Database.with(Article.class).get());
    JSONObject jsonTable = new JSONObject();
    for (String domain : table.articlesAllTime.elementSet()) {
      JSONObject siteObject = new JSONObject();
      siteObject.put("threeHourArticles", table.articlesInThreeHours.count(domain));
      siteObject.put("threeHourShares", table.sharesInThreeHours.count(domain));
      siteObject.put("oneDayArticles", table.articlesInOneDay.count(domain));
      siteObject.put("oneDayShares", table.sharesInOneDay.count(domain));
      siteObject.put("threeDayArticles", table.articlesInThreeDays.count(domain));
      siteObject.put("threeDayShares", table.sharesInThreeDays.count(domain));
      siteObject.put("sevenDayArticles", table.articlesInSevenDays.count(domain));
      siteObject.put("sevenDayShares", table.sharesInSevenDays.count(domain));
      siteObject.put("allTimeArticles", table.articlesAllTime.count(domain));
      siteObject.put("allTimeShares", table.sharesAllTime.count(domain));
      jsonTable.put(domain, siteObject);
    }
    System.out.println(jsonTable);
  }
}
