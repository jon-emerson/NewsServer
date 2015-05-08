package com.janknspank.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.api.client.util.Lists;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.Feature;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.classifier.VectorFeature;
import com.janknspank.classifier.VectorFeatureCreator;
import com.janknspank.common.Averager;
import com.janknspank.common.TopList;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.rank.Personas;

public class Helper {
  public static void main4(String args[]) throws Exception {
    Averager launchFacebookAverager = new Averager();
    Averager launchTwitterAverager = new Averager();
    Averager notLaunchFacebookAverager = new Averager();
    Averager notLaunchTwitterAverager = new Averager();

    Set<String> launchArticleUrls = Sets.newHashSet();
//    launchArticleUrls.addAll(
//        UrlFinder.findUrls("http://mashable.com/category/launch/"));
//    launchArticleUrls.addAll(
//        UrlFinder.findUrls("http://mashable.com/category/apps-and-software/"));
//    launchArticleUrls.addAll(
//        UrlFinder.findUrls("http://mashable.com/category/startups/"));
    Map<String, Article> launchArticles = Maps.newHashMap(
        ArticleCrawler.getArticles(launchArticleUrls, true /* retain */));
    for (Article article : Database.with(Article.class).get(
        //new QueryOption.WhereEqualsNumber("feature.feature_id", FeatureId.INTERNET.getId()),
        new QueryOption.WhereLike("url", "http://readwrite.*"))) {
      launchArticles.put(article.getUrl(), article);
    }

    for (Article article : launchArticles.values()) {
      boolean isLaunch =
          article.getUrl().contains("launch")
          || ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_LAUNCHES) > 0.1;
      if (isLaunch) {
        System.out.println(article.getUrl());
      }
      SocialEngagement twitterEngagement =
          SocialEngagements.getForArticle(article, Site.TWITTER);
      if (twitterEngagement != null) {
        if (isLaunch) {
          launchTwitterAverager.add(twitterEngagement.getShareCount());
        } else {
          notLaunchTwitterAverager.add(twitterEngagement.getShareCount());
        }
      }
      SocialEngagement facebookEngagement =
          SocialEngagements.getForArticle(article, Site.FACEBOOK);
      if (facebookEngagement != null) {
        if (isLaunch) {
          launchFacebookAverager.add(facebookEngagement.getShareCount());
        } else {
          notLaunchFacebookAverager.add(facebookEngagement.getShareCount());
        }
      }
    }
    System.out.println();
    System.out.println("launchFacebookAverager = " + launchFacebookAverager.get()
        + " from " + launchFacebookAverager.getCount() + " data points");
    System.out.println("notLaunchFacebookAverager = " + notLaunchFacebookAverager.get()
        + " from " + notLaunchFacebookAverager.getCount() + " data points");
    System.out.println("Facebook difference: "
        + launchFacebookAverager.get() / notLaunchFacebookAverager.get());
    System.out.println();
    System.out.println("launchTwitterAverager = " + launchTwitterAverager.get()
        + " from " + launchTwitterAverager.getCount() + " data points");
    System.out.println("notLaunchTwitterAverager = " + notLaunchTwitterAverager.get()
        + " from " + notLaunchTwitterAverager.getCount() + " data points");
    System.out.println("Twitter difference: "
        + launchTwitterAverager.get() / notLaunchTwitterAverager.get());
  }

  public static void main5(String args[]) throws Exception {
    int[] bucket = new int[100];
    for (int i = 0; i < bucket.length; i++) {
      bucket[i] = 0;
    }
    int count = 0;
    for (Article article : Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(10000))) {
      for (ArticleFeature articleFeature : article.getFeatureList()) {
        FeatureId featureId = FeatureId.fromId(articleFeature.getFeatureId());
        if (featureId != null && featureId.getFeatureType() == FeatureType.INDUSTRY) {
          count++;
          double similarity = articleFeature.getSimilarity();
          int bucketNum = (int) (similarity * 100);
          if (bucketNum == 100) {
            bucketNum = 99;
          }
          bucket[bucketNum]++;
        }
      }
    }
    for (int i = 0; i < bucket.length; i++) {
      System.out.println("0." + i + ": "
          + bucket[i] + " (" + ((double) bucket[i] / count) + "%)");
    }
  }

  public static void main(String args[]) throws Exception {
    Iterable<Article> seedArticles =
        new VectorFeatureCreator(FeatureId.VIDEO_PRODUCTION).getSeedArticles();
    VectorFeature ventureCapitalFeature =
        (VectorFeature) Feature.getFeature(FeatureId.VIDEO_PRODUCTION);
    TopList<Article, Double> topArticles = new TopList<Article, Double>(2000);
    for (Article seedArticle : seedArticles) {
      topArticles.add(seedArticle, ventureCapitalFeature.rawScore(seedArticle));
    }
    System.out.println("Articles:");
    int i = 0;
    for (Article article : topArticles) {
      if (article.getUrl().equals("http://bits.blogs.nytimes.com/2014/10/28/youtube-weighing-new-subscription-service/")) {
      System.out.println(++i + ". " + topArticles.getValue(article) + ": " + article.getUrl()
          + " -> " + ventureCapitalFeature.score(article));
      }
    }

    System.out.println("10% quantile: " + ventureCapitalFeature.getSimilarityThreshold10Percent());
    System.out.println("50% quantile: " + ventureCapitalFeature.getSimilarityThreshold50Percent());
  }

  public static void main2(String args[]) throws Exception {
    for (String email : new String[] { "panaceaa@gmail.com" }) {
      Persona persona = Personas.getByEmail(email);
      Map<String, Article> goodArticles = ArticleCrawler.getArticles(persona.getGoodUrlList(), true);
      for (Article article : goodArticles.values()) {
        double internet = ArticleFeatures.getFeatureSimilarity(article, FeatureId.INTERNET);
        if (internet > 0.95) {
          System.out.println(article.getUrl());
        }
      }
    }
  }

  public static void main3(String args[]) throws DatabaseSchemaException {
    for (Article article : Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(10000))) {
      double featureSimilarity =
          ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_IS_LIST);
      if (featureSimilarity > 0.1
          && ArticleFeatures.getFeatureSimilarity(article, FeatureId.ARCHITECTURE_AND_PLANNING) > 0.8) {
        System.out.println("\"" + article.getTitle() + "\" (" + featureSimilarity + ")");
        System.out.println(article.getUrl());
        System.out.println("First paragraph: \""
            + Iterables.getFirst(article.getParagraphList(), "") + "\"");
        System.out.println();
      }
    }
  }
}
