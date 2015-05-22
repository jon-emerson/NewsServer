package com.janknspank.crawler.social;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.protobuf.TextFormat;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.common.Logger;
import com.janknspank.crawler.SiteManifests;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.proto.CoreProto.ShareNormalizationData;
import com.janknspank.proto.CoreProto.ShareNormalizationData.DomainShareCount;
import com.janknspank.proto.CoreProto.ShareNormalizationData.TimeRangeDistribution;
import com.janknspank.proto.CoreProto.ShareNormalizationData.TimeRangeDistribution.Builder;
import com.janknspank.proto.CrawlerProto.SiteManifest;
import com.janknspank.rank.DistributionBuilder;

public class ShareNormalizer {
  private static final Logger LOG = new Logger(ShareNormalizer.class);
  private static final Map<Site, String> FILENAME_MAP = ImmutableMap.of(
      Site.FACEBOOK, "classifier/shares/facebooksharenormalizer.bin",
      Site.TWITTER, "classifier/shares/twittersharenormalizer.bin");

  private static Map<Site, ShareNormalizer> instanceMap = Maps.newHashMap();
  private final ShareNormalizationData data;
  private final Map<String, ShareCount> shareMap;
  private final long totalArticleCount;
  private final long totalShareCount;

  private ShareNormalizer(Site site) throws ClassifierException {
    InputStream inputStream = null;
    try {
      inputStream = new GZIPInputStream(new FileInputStream(FILENAME_MAP.get(site)));
      data = ShareNormalizationData.parseFrom(inputStream);
      shareMap = createShareMap(data);
      totalArticleCount = getTotalArticleCount(shareMap);
      totalShareCount = getTotalShareCount(shareMap);
    } catch (IOException e) {
      throw new ClassifierException("Could not read file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public static synchronized ShareNormalizer getInstance(Site site) throws ClassifierException {
    if (!instanceMap.containsKey(site)) {
      instanceMap.put(site, new ShareNormalizer(site));
    }
    return instanceMap.get(site);
  }

  public double getShareScore(String url, long shareCount, long ageInMillis) {
    Distribution distribution = null;
    for (TimeRangeDistribution timeRangeDistribution : data.getTimeRangeDistributionList()) {
      distribution = timeRangeDistribution.getDistribution();
      if (timeRangeDistribution.getStartMillis() <= ageInMillis &&
          timeRangeDistribution.getEndMillis() > ageInMillis) {
        break;
      }
    }
    long normalizedShareCount = getNormalizedShareCount(url, shareCount);
    double shareScore = DistributionBuilder.projectQuantile(distribution, normalizedShareCount);
    return shareScore;
  }

  private static class ShareCount {
    long numArticles = 0;
    long numShares = 0;
  }

  private static long getTotalArticleCount(Map<String, ShareCount> shareMap) {
    int sum = 0;
    for (ShareCount count : shareMap.values()) {
      sum += count.numArticles;
    }
    return sum;
  }

  private static long getTotalShareCount(Map<String, ShareCount> shareMap) {
    int sum = 0;
    for (ShareCount count : shareMap.values()) {
      sum += count.numShares;
    }
    return sum;
  }

  private long getNormalizedShareCount(String urlString, long shareCount) {
    return getNormalizedShareCount(shareMap, totalArticleCount, totalShareCount, urlString,
        shareCount);
  }

  /**
   * Normalizes the share count in the passed {@code engagement} so that it
   * matches the general intensity of shares from all sites in our corpus.
   * (Basically, domains that are over-shared are brought into line, domains
   * that are undershared are brought up to match.)
   */
  private static long getNormalizedShareCount(
      Map<String, ShareCount> shareMap, long totalArticleCount, long totalShareCount,
      String urlString, long shareCount) {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      return 0;
    }
    String domain = url.getHost();

    ShareCount count = null;
    while (count == null && domain.contains(".")) {
      count = shareMap.get(domain);
      domain = domain.substring(domain.indexOf(".") + 1);
    }
    if (count == null) {
      return shareCount;
    } else {
      // Lower floor at 30 shares/article so that articles from sites with few
      // shares don't look like absolute monsters when they get a little action.
      double domainShareCount = Math.max(30.0, ((double) count.numShares) / count.numArticles);
      double averageShareCount = ((double) totalShareCount) / totalArticleCount;
      return (long) (shareCount * averageShareCount / domainShareCount);
    }
  }

  /**
   * Returns the starting point for building the normalizer: A big ole' list of
   * TimeRangeDistribution.Builders for each article age range we want to track.
   */
  private static List<TimeRangeDistribution.Builder> getTimeRangeDistributionBuilders() {
    ImmutableList.Builder<TimeRangeDistribution.Builder> timeRangeDistributionBuilders =
        ImmutableList.<TimeRangeDistribution.Builder>builder();
    long lastTimeBreak = 0;
    for (long timeBreak : new long[] {
        TimeUnit.HOURS.toMillis(1),
        TimeUnit.HOURS.toMillis(2),
        TimeUnit.HOURS.toMillis(3),
        TimeUnit.HOURS.toMillis(6),
        TimeUnit.HOURS.toMillis(12),
        TimeUnit.HOURS.toMillis(18),
        TimeUnit.DAYS.toMillis(1),
        TimeUnit.DAYS.toMillis(2),
        TimeUnit.DAYS.toMillis(3),
        TimeUnit.DAYS.toMillis(5),
        TimeUnit.DAYS.toMillis(7),
        TimeUnit.DAYS.toMillis(14),
        TimeUnit.DAYS.toMillis(30),
        TimeUnit.DAYS.toMillis(90)}) {
      timeRangeDistributionBuilders.add(TimeRangeDistribution.newBuilder()
          .setStartMillis(lastTimeBreak)
          .setEndMillis(timeBreak));
      lastTimeBreak = timeBreak;
    }
    timeRangeDistributionBuilders.add(TimeRangeDistribution.newBuilder()
        .setStartMillis(lastTimeBreak)
        .setEndMillis(Long.MAX_VALUE));
    return timeRangeDistributionBuilders.build();
  }

  private static void test(Site site) throws DatabaseSchemaException, ClassifierException {
    for (Article article : Database.with(Article.class).get(new QueryOption.Limit(50))) {
      SocialEngagement engagement = SocialEngagements.getForArticle(article, site);
      if (engagement != null) {
        LOG.info(getInstance(site).getShareScore(
            article.getUrl(),
            engagement.getShareCount(),
            Math.max(0, engagement.getCreateTime() - article.getPublishedTime()))
            + " for " + article.getUrl());
      }
    }
  }

  private static final Map<String, ShareCount> createShareMap(ShareNormalizationData data) {
    ImmutableMap.Builder<String, ShareCount> builder = ImmutableMap.<String, ShareCount>builder();
    for (DomainShareCount domainShareCount : data.getDomainShareCountList()) {
      ShareCount shareCount = new ShareCount();
      shareCount.numArticles = domainShareCount.getArticleCount();
      shareCount.numShares = domainShareCount.getShareCount();
      builder.put(domainShareCount.getDomain(), shareCount);
    }
    return builder.build();
  }

  private static final Map<String, ShareCount> createShareMap(
      Iterable<Article> articles, Site site) {
    ImmutableMap.Builder<String, ShareCount> mapBuilder = ImmutableMap.<String, ShareCount>builder();
    for (SiteManifest siteManifest : SiteManifests.getList()) {
      mapBuilder.put(siteManifest.getRootDomain(), new ShareCount());
      for (String akaRootDomain : siteManifest.getAkaRootDomainList()) {
        mapBuilder.put(akaRootDomain, new ShareCount());
      }
    }
    ImmutableMap<String, ShareCount> map = mapBuilder.build();

    for (Article article : articles) {
      URL url;
      try {
        url = new URL(article.getUrl());
      } catch (MalformedURLException e) {
        continue;
      }
      String domain = url.getHost();

      ShareCount count = null;
      while (count == null && domain.contains(".")) {
        count = map.get(domain);
        domain = domain.substring(domain.indexOf(".") + 1);
      }
      if (count != null) {
        SocialEngagement engagement = SocialEngagements.getForArticle(article, site);
        if (engagement != null) {
          count.numArticles++;
          count.numShares += engagement.getShareCount();
        }
      }
    }
    return map;
  }

  private static void createShareNormalizationFile(Iterable<Article> articles, Site site)
      throws ClassifierException {
    Map<String, ShareCount> shareMap = createShareMap(articles, site);
    long totalArticleCount = getTotalArticleCount(shareMap);
    long totalShareCount = getTotalShareCount(shareMap);

    // Let's build distributions for articles depending on how old they are.
    List<TimeRangeDistribution.Builder> timeRangeDistributionBuilders =
        getTimeRangeDistributionBuilders();
    Map<TimeRangeDistribution.Builder, DistributionBuilder> distributionBuilders =
        Maps.toMap(timeRangeDistributionBuilders,
            new Function<TimeRangeDistribution.Builder, DistributionBuilder>() {
              @Override
              public DistributionBuilder apply(Builder distributionBuilder) {
                return new DistributionBuilder();
              }
            });

    for (Article article : articles) {
      int i = 0;
      if (++i % 100 == 0) {
        System.out.print(".");
      }

      SocialEngagement latestEngagement = SocialEngagements.getForArticle(article, site);
      if (latestEngagement != null) {
        long ageInMillis = Math.max(0,
            latestEngagement.getCreateTime() - article.getPublishedTime());
        long normalizedShareCount = getNormalizedShareCount(shareMap, totalArticleCount,
            totalShareCount, article.getUrl(), latestEngagement.getShareCount());

        for (TimeRangeDistribution.Builder builder : timeRangeDistributionBuilders) {
          if (builder.getStartMillis() <= ageInMillis &&
              builder.getEndMillis() > ageInMillis) {
            distributionBuilders.get(builder).add(normalizedShareCount);
          }
        }
      }
    }

    ShareNormalizationData.Builder shareNormalizationDataBuilder =
        ShareNormalizationData.newBuilder();
    for (TimeRangeDistribution.Builder timeRangeDistributionBuilder
        : timeRangeDistributionBuilders) {
      LOG.info("Building range " + timeRangeDistributionBuilder.getStartMillis()
          + " - " + timeRangeDistributionBuilder.getEndMillis() + " ...");
      shareNormalizationDataBuilder.addTimeRangeDistribution(
          timeRangeDistributionBuilder.setDistribution(
              distributionBuilders.get(timeRangeDistributionBuilder).build()));
    }
    for (Map.Entry<String, ShareCount> entry : shareMap.entrySet()) {
      shareNormalizationDataBuilder.addDomainShareCount(DomainShareCount.newBuilder()
          .setDomain(entry.getKey())
          .setArticleCount(entry.getValue().numArticles)
          .setShareCount(entry.getValue().numShares));
    }

    OutputStream outputStream = null;
    try {
      outputStream = new GZIPOutputStream(new FileOutputStream(FILENAME_MAP.get(site)));
      shareNormalizationDataBuilder.build().writeTo(outputStream);
    } catch (IOException e) {
      throw new ClassifierException("Could not write file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }

  /**
   * Rebuilds the share normalization proto on disk.
   */
  public static void main(String args[]) throws Exception {
    if (args.length > 0 && args[0].equals("test")) {
      test(Site.FACEBOOK);
      return;
    } else if (args.length > 0 && args[0].equals("print")) {
      InputStream inputStream = null;
      try {
        inputStream = new GZIPInputStream(new FileInputStream(FILENAME_MAP.get(Site.FACEBOOK)));
        ShareNormalizationData data = ShareNormalizationData.parseFrom(inputStream);
        TextFormat.print(data, System.out);
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
      return;
    }

    System.out.println("Reading 60k articles...");
    Future<Iterable<Article>> newArticles = Database.with(Article.class).getFuture(
        new QueryOption.Limit(45000),
        new QueryOption.DescendingSort("published_time"));
    Future<Iterable<Article>> oldArticles = Database.with(Article.class).getFuture(
        new QueryOption.Limit(15000),
        new QueryOption.AscendingSort("published_time"));
    Iterable<Article> articles = Iterables.concat(oldArticles.get(), newArticles.get());
    System.out.println(Iterables.size(articles) + " articles received.");
    System.out.println("Calculating Facebook share normalization table...");
    createShareNormalizationFile(articles, Site.FACEBOOK);
    System.out.println("Calculating Twitter share normalization table...");
    createShareNormalizationFile(articles, Site.TWITTER);
    System.out.println("Done!");
  }
}
