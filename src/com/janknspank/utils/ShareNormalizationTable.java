package com.janknspank.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.google.api.client.util.Maps;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.janknspank.ArticleCrawler;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.proto.CoreProto.ShareNormalizationData;
import com.janknspank.proto.CoreProto.ShareNormalizationData.DomainTimedDistribution;
import com.janknspank.proto.CoreProto.ShareNormalizationData.DomainTimedDistribution.TimedDistribution;
import com.janknspank.rank.DistributionBuilder;
import com.janknspank.rank.JonBenchmark;
import com.janknspank.rank.RankException;

/**
 * Contains average share counts for each news-hosting domain, indexed both by
 * time and by domain.  This allows us to compare an article's social engagement
 * trajectory against other articles on the same domain and of a similar age.
 * 
 * NOTE(jonemerson): Currently only handles Facebook share counts.
 */
public class ShareNormalizationTable {
  private static final Map<String, String> DOMAIN_CANONICALIZATIONS =
      new ImmutableMap.Builder<String, String>()
          .put("edition.cnn.com", "cnn.com")
          .put("us.cnn.com", "cnn.com")
          .put("economy.money.cnn.com", "money.cnn.com")
          .put("knowmore.washingtonpost.com", "washingtonpost.com")
          .build();
  public final Map<String, Map<Long, Distribution>> distributionForDomainMap;

  public static class ShareNormalizationTableBuilder {
    public final Map<String, Map<Long, DistributionBuilder>> distributionForDomainMap =
        Maps.newHashMap();

    /**
     * Constructs a ShareNormalizationTable from a set of articles.  The passed
     * articles should represent a statistically significant set - e.g. at least
     * 20k of Articles, so that we can build normalization tables for share
     * rates across many different domains and article ages.
     */
    public ShareNormalizationTableBuilder(Iterable<Article> articles) throws MalformedURLException {
      for (Article article : articles) {
        String domain = getDomainForArticle(article);
        Map<Long, DistributionBuilder> distributionMap = getDistributionMapForDomain(domain);
        SocialEngagement engagement = SocialEngagements.getForArticle(article, Site.FACEBOOK);
        if (engagement != null) {
          Long ageInMillis = getAgeInMillis(article, engagement);
          for (Map.Entry<Long, DistributionBuilder> entry : distributionMap.entrySet()) {
            if (ageInMillis <= entry.getKey()) {
              entry.getValue().add(engagement.getShareCount());
            }
          }
        }
      }
    }

    private Map<Long, DistributionBuilder> getDistributionMapForDomain(String domain) {
      if (!distributionForDomainMap.containsKey(domain)) {
        distributionForDomainMap.put(domain, 
            ImmutableMap.<Long, DistributionBuilder>builder()
                .put(TimeUnit.HOURS.toMillis(1), new DistributionBuilder())
                .put(TimeUnit.HOURS.toMillis(3), new DistributionBuilder())
                .put(TimeUnit.HOURS.toMillis(6), new DistributionBuilder())
                .put(TimeUnit.HOURS.toMillis(12), new DistributionBuilder())
                .put(TimeUnit.DAYS.toMillis(1), new DistributionBuilder())
                .put(TimeUnit.DAYS.toMillis(2), new DistributionBuilder())
                .put(TimeUnit.DAYS.toMillis(3), new DistributionBuilder())
                .put(TimeUnit.DAYS.toMillis(7), new DistributionBuilder())
                .put(TimeUnit.DAYS.toMillis(14), new DistributionBuilder())
                .put(TimeUnit.DAYS.toMillis(30), new DistributionBuilder())
                .put(TimeUnit.DAYS.toMillis(90), new DistributionBuilder())
                .put(TimeUnit.DAYS.toMillis(365), new DistributionBuilder())
                .build());
      }
      return distributionForDomainMap.get(domain);
    }

    public ShareNormalizationTable build() {
      return new ShareNormalizationTable(distributionForDomainMap);
    }
  }

  /**
   * Returns the amount of time between when this article was published and
   * when we last received Facebook share data for it.  Returns null of the
   * received article has no social data.
   */
  @VisibleForTesting
  static Long getAgeInMillis(Article article, SocialEngagement engagement) {
    return Math.max(TimeUnit.HOURS.toMillis(2),
        engagement.getCreateTime() - article.getPublishedTime());
  }

  /**
   * For a given article, return a canonical domain name for its host.
   * "www."-style prefixes are dropped.
   */
  @VisibleForTesting
  static String getDomainForArticle(Article article) {
    URL url;
    try {
      url = new URL(article.getUrl());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    String host = url.getHost();
    if (host.startsWith("www.") || host.startsWith("www1.")) {
      host = host.substring(host.indexOf(".") + 1);
    }
    if (DOMAIN_CANONICALIZATIONS.containsKey(host)) {
      host = DOMAIN_CANONICALIZATIONS.get(host);
    }
    return host;
  }


  private ShareNormalizationTable(Map<String, Map<Long, DistributionBuilder>> map) {
    ImmutableMap.Builder<String, Map<Long, Distribution>> builder = ImmutableMap.builder();
    for (Map.Entry<String, Map<Long, DistributionBuilder>> entry : map.entrySet()) {
      ImmutableMap.Builder<Long, Distribution> innerBuilder = ImmutableMap.builder();
      for (Map.Entry<Long, DistributionBuilder> innerEntry : entry.getValue().entrySet()) {
        innerBuilder.put(innerEntry.getKey(), innerEntry.getValue().build());
      }
      builder.put(entry.getKey(), innerBuilder.build());
    }
    this.distributionForDomainMap = builder.build();
  }

  private ShareNormalizationTable(ShareNormalizationData data) {
    ImmutableMap.Builder<String, Map<Long, Distribution>> builder = ImmutableMap.builder();
    for (DomainTimedDistribution domainTimedDistribution : data.getDomainTimedDistributionList()) {
      ImmutableMap.Builder<Long, Distribution> innerBuilder = ImmutableMap.builder();
      for (TimedDistribution timedDistribution :
            domainTimedDistribution.getTimedDistributionList()) {
        innerBuilder.put(timedDistribution.getTimeSincePublished(),
            timedDistribution.getDistribution());
      }
      builder.put(domainTimedDistribution.getDomain(), innerBuilder.build());
    }
    this.distributionForDomainMap = builder.build();
  }

  /**
   * Converts this ShareNormalizationTable to proto format, usually so that it
   * can be stored to disk.
   */
  public ShareNormalizationData toData() {
    ShareNormalizationData.Builder dataBuilder = ShareNormalizationData.newBuilder();
    for (Map.Entry<String, Map<Long, Distribution>> entry : distributionForDomainMap.entrySet()) {
      DomainTimedDistribution.Builder domainTimedDistributionBuilder =
          DomainTimedDistribution.newBuilder();
      domainTimedDistributionBuilder.setDomain(entry.getKey());
      for (Map.Entry<Long, Distribution> innerEntry : entry.getValue().entrySet()) {
        domainTimedDistributionBuilder.addTimedDistribution(TimedDistribution.newBuilder()
            .setTimeSincePublished(innerEntry.getKey())
            .setDistribution(innerEntry.getValue()));
      }
      dataBuilder.addDomainTimedDistribution(domainTimedDistributionBuilder);
    }
    return dataBuilder.build();
  }

  /**
   * Writes the proto representation of this share normalization table in
   * compressed format to the specified share normalization file.
   */
  public void writeToFile(File shareNormalizationFile) throws RankException {
    OutputStream outputStream = null;
    try {
      outputStream = new GZIPOutputStream(new FileOutputStream(shareNormalizationFile));
      toData().writeTo(outputStream);
    } catch (IOException e) {
      throw new RankException("Could not write file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }

  /**
   * Returns a number between 0 and 1 representing this article's relative
   * social engagement versus other articles on the same site with similar
   * ages.
   */
  public double getEngagementRank(Article article) {
    return getEngagementRank(article, getDomainForArticle(article));
  }

  /**
   * Returns what an average article on {@code domain} receives in terms of
   * Facebook likes over the given {@code ageInMillis} time since publishing.
   */
  public double getAverageEngagement(String domain, long ageInMillis) {
    Map<Long, Distribution> domainDistributionMap = distributionForDomainMap.get(domain);

    // This will become the oldest distribution that's older than
    // millisSincePublished.
    Long olderDistributionMillis = getNearbyDistributionAge(domain, ageInMillis, true /* older */);
    Distribution olderDistribution = domainDistributionMap.get(olderDistributionMillis);

    // This will become the youngest distribution that's newer than
    // millisSincePublished.
    Long newerDistributionMillis = getNearbyDistributionAge(domain, ageInMillis, true /* older */);
    Distribution newerDistribution = domainDistributionMap.get(newerDistributionMillis);

    long range = olderDistributionMillis - newerDistributionMillis;
    double ratioTowardsOlder =
        (range == 0) ? 1 : (ageInMillis - newerDistributionMillis) / range;
    return
        DistributionBuilder.getValueAtPercentile(olderDistribution, 50)
            * ratioTowardsOlder +
        DistributionBuilder.getValueAtPercentile(newerDistribution, 50)
            * (1 - ratioTowardsOlder);
  }

  /**
   * Returns the age in millis for the nearest Distribution for the specified
   * {@code domain} that's either {@code older} or newer than the specified
   * article age.
   * @param domain the domain to use for grabbing a nearby distribution
   * @param ageInMillis the number of milliseconds since now that we should try
   *     to find the nearest distribution
   * @param older if there is not an exact match for the {@code ageInMillis},
   *     whether this function should return a distribution for articles newer
   *     or older than the specified age.  If {@code above} is true, the 
   *     returned distribution time Long will be bigger than ageInMillis.
   */
  public Long getNearbyDistributionAge(String domain, long ageInMillis, boolean older) {
    Map<Long, Distribution> domainDistributionMap = distributionForDomainMap.get(domain);
    Long nearestDistributionMillis = null;

    for (Map.Entry<Long, Distribution> timedDistribution : domainDistributionMap.entrySet()) {
      if (((older && timedDistribution.getKey() >= ageInMillis) ||
              (!older && timedDistribution.getKey() <= ageInMillis)) &&
          (nearestDistributionMillis == null ||
              Math.abs(timedDistribution.getKey() - ageInMillis) <
                  Math.abs(nearestDistributionMillis - ageInMillis))) {
        nearestDistributionMillis = timedDistribution.getKey();
      }
    }
    return nearestDistributionMillis;
  }

  /**
   * Private method: Enables this article's social ranking to be compared to
   * articles on a different domain.
   */
  public double getEngagementRank(Article article, String domain) {
    SocialEngagement engagement =
        SocialEngagements.getForArticle(article, SocialEngagement.Site.FACEBOOK);
    long ageInMillis = getAgeInMillis(article, engagement);

    Map<Long, Distribution> domainDistributionMap = distributionForDomainMap.get(domain);
    if (domainDistributionMap.isEmpty()) {
      // Compute an average of 20 other domains.
      int i = 0;
      int sum = 0;
      for (String otherDomain : distributionForDomainMap.keySet()) {
        sum += getEngagementRank(article, otherDomain);
        if (i++ > 20) {
          break;
        }
      }
      return sum / i;
    }

    // This will become the oldest distribution that's older than
    // millisSincePublished.
    Long olderDistributionMillis = getNearbyDistributionAge(domain, ageInMillis, true /* older */);
    Distribution olderDistribution = domainDistributionMap.get(olderDistributionMillis);

    // This will become the youngest distribution that's newer than
    // millisSincePublished.
    Long newerDistributionMillis = getNearbyDistributionAge(domain, ageInMillis, true /* older */);
    Distribution newerDistribution = domainDistributionMap.get(newerDistributionMillis);

    long range = olderDistributionMillis - newerDistributionMillis;
    double ratioTowardsOlder =
        (range == 0) ? 1 : (ageInMillis - newerDistributionMillis) / range;
    return
        (DistributionBuilder.projectQuantile(olderDistribution, engagement.getShareCount())
            * ratioTowardsOlder +
        DistributionBuilder.projectQuantile(newerDistribution, engagement.getShareCount())
            * (1 - ratioTowardsOlder)) / 100;
  }

  public static ShareNormalizationTable fromFile(File shareNormalizationFile)
      throws RankException {
    InputStream inputStream = null;
    try {
      inputStream = new GZIPInputStream(new FileInputStream(shareNormalizationFile));
      return new ShareNormalizationTable(ShareNormalizationData.parseFrom(inputStream));
    } catch (IOException e) {
      throw new RankException("Could not read file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public static void main(String args[])
      throws BiznessException, MalformedURLException, DatabaseSchemaException {
    ShareNormalizationTableBuilder builder = new ShareNormalizationTableBuilder(
        Database.with(Article.class).get(new QueryOption.Limit(1000)));
    ShareNormalizationTable table = builder.build();
    Collection<Article> articles = ArticleCrawler.getArticles(JonBenchmark.GOOD_URLS).values();
    for (Article article : articles) {
      System.out.println(table.getEngagementRank(article) + ": " + article.getUrl());
    }
  }
}
