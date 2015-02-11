package com.janknspank.interpreter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.ClassifierException;
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
import com.janknspank.rank.DistributionBuilder;

public class FacebookShareNormalizer {
  private static final String FILENAME = "classifier/shares/facebooksharenormalizer.bin";

  private static FacebookShareNormalizer instance = null;
  private final ShareNormalizationData data;
  private final Map<String, ShareCount> shareMap;
  private final long totalArticleCount;
  private final long totalShareCount;

  private FacebookShareNormalizer() throws ClassifierException {
    InputStream inputStream = null;
    try {
      inputStream = new GZIPInputStream(new FileInputStream(FILENAME));
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

  public static synchronized FacebookShareNormalizer getInstance() throws ClassifierException {
    if (instance == null) {
      instance = new FacebookShareNormalizer();
    }
    return instance;
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
    return DistributionBuilder.projectQuantile(distribution,
        getNormalizedShareCount(url, shareCount));
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
      double domainShareCount = ((double) count.numShares) / count.numArticles;
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

  private static void test() throws DatabaseSchemaException, ClassifierException {
    for (Article article : Database.with(Article.class).get(new QueryOption.Limit(50))) {
      SocialEngagement engagement = SocialEngagements.getForArticle(article, Site.FACEBOOK);
      if (engagement != null) {
        System.out.println(getInstance().getShareScore(
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

  private static final Map<String, ShareCount> createShareMap(Iterable<Article> articles) {
    ImmutableMap<String, ShareCount> map = ImmutableMap.<String, ShareCount>builder()
        .put("philly.curbed.com", new ShareCount())
        .put("boston.com", new ShareCount())
        .put("la.curbed.com", new ShareCount())
        .put("redherring.com", new ShareCount())
        .put("cnbc.com", new ShareCount())
        .put("siliconbeat.com", new ShareCount())
        .put("nola.curbed.com", new ShareCount())
        .put("cbsnews.com", new ShareCount())
        .put("bloomberg.com", new ShareCount())
        .put("engadget.com", new ShareCount())
        .put("sf.curbed.com", new ShareCount())
        .put("atlanta.curbed.com", new ShareCount())
        .put("well.blogs.nytimes.com", new ShareCount())
        .put("buffalonews.com", new ShareCount())
        .put("forbes.com", new ShareCount())
        .put("miami.curbed.com", new ShareCount())
        .put("wired.com", new ShareCount())
        .put("cbc.ca", new ShareCount())
        .put("blog.sfgate.com", new ShareCount())
        .put("chicago.curbed.com", new ShareCount())
        .put("cnn.com", new ShareCount())
        .put("recode.net", new ShareCount())
        .put("seattle.curbed.com", new ShareCount())
        .put("nytimes.com", new ShareCount())
        .put("bbc.co.uk", new ShareCount())
        .put("boston.curbed.com", new ShareCount())
        .put("businessinsider.com", new ShareCount())
        .put("capecod.curbed.com", new ShareCount())
        .put("venturebeat.com", new ShareCount())
        .put("channelnewsasia.com", new ShareCount())
        .put("startupworkout.com", new ShareCount())
        .put("thenextweb.com", new ShareCount())
        .put("mercurynews.com", new ShareCount())
        .put("hamptons.curbed.com", new ShareCount())
        .put("cleveland.com", new ShareCount())
        .put("medium.com", new ShareCount())
        .put("money.cnn.com", new ShareCount())
        .put("ny.curbed.com", new ShareCount())
        .put("pcmag.com", new ShareCount())
        .put("chron.com", new ShareCount())
        .put("arstechnica.com", new ShareCount())
        .put("ski.curbed.com", new ShareCount())
        .put("dealbook.nytimes.com", new ShareCount())
        .put("insidescoopsf.sfgate.com", new ShareCount())
        .put("theverge.com", new ShareCount())
        .put("businessweek.com", new ShareCount())
        .put("allthingsd.com", new ShareCount())
        .put("abcnews.go.com", new ShareCount())
        .put("america.aljazeera.com", new ShareCount())
        .put("fastcompany.com", new ShareCount())
        .put("latimes.com", new ShareCount())
        .put("breitbart.com", new ShareCount())
        .put("opinionator.blogs.nytimes.com", new ShareCount())
        .put("dc.curbed.com", new ShareCount())
        .put("slate.com", new ShareCount())
        .put("detroit.curbed.com", new ShareCount())
        .put("mashable.com", new ShareCount())
        .put("blog.chron.com", new ShareCount())
        .put("washingtonpost.com", new ShareCount())
        .put("techcrunch.com", new ShareCount())
        .put("sfgate.com", new ShareCount())
        .put("curbed.com", new ShareCount())
        .put("abc.net.au", new ShareCount())
        .put("bits.blogs.nytimes.com", new ShareCount())
        .build();
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
        SocialEngagement engagement = SocialEngagements.getForArticle(article, Site.FACEBOOK);
        if (engagement != null) {
          count.numArticles++;
          count.numShares += engagement.getShareCount();
        }
      }
    }
    return map;
  }

  /**
   * Rebuilds the share normalization proto on disk.
   */
  public static void main(String args[]) throws Exception {
    if (args.length > 0 && args[0].equals("test")) {
      test();
      return;
    }

    Iterable<Article> articles = Database.with(Article.class).get();
    Map<String, ShareCount> shareMap = createShareMap(articles);
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

    System.out.println("Reading all articles...");
    for (Article article : articles) {
      int i = 0;
      if (++i % 100 == 0) {
        System.out.print(".");
      }

      SocialEngagement latestEngagement =
          SocialEngagements.getForArticle(article, Site.FACEBOOK);
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
      System.out.println("Building range " + timeRangeDistributionBuilder.getStartMillis()
          + " - " + timeRangeDistributionBuilder.getEndMillis() + " ...");
      shareNormalizationDataBuilder.addTimeRangeDistribution(
          timeRangeDistributionBuilder
              .setDistribution(distributionBuilders.get(timeRangeDistributionBuilder).build()));
    }
    for (Map.Entry<String, ShareCount> entry : shareMap.entrySet()) {
      shareNormalizationDataBuilder.addDomainShareCount(DomainShareCount.newBuilder()
          .setDomain(entry.getKey())
          .setArticleCount(entry.getValue().numArticles)
          .setShareCount(entry.getValue().numShares));
    }

    OutputStream outputStream = null;
    try {
      outputStream = new GZIPOutputStream(new FileOutputStream(FILENAME));
      shareNormalizationDataBuilder.build().writeTo(outputStream);
    } catch (IOException e) {
      throw new ClassifierException("Could not write file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }
}
