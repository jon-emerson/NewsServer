package com.janknspank.utils;

import java.util.Map;

import com.google.common.collect.Iterables;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.Feature;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.classifier.VectorFeature;
import com.janknspank.classifier.VectorFeatureCreator;
import com.janknspank.common.TopList;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.rank.Personas;

public class Helper {
  public static void main(String args[]) throws Exception {
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

  public static void main4(String args[]) throws Exception {
    Iterable<Article> seedArticles =
        new VectorFeatureCreator(FeatureId.VENTURE_CAPITAL).getSeedArticles();
    VectorFeature ventureCapitalFeature = (VectorFeature) Feature.getFeature(FeatureId.VENTURE_CAPITAL);
    TopList<String, Double> topArticles = new TopList<String, Double>(2000);
    for (Article seedArticle : seedArticles) {
      topArticles.add(seedArticle.getUrl(), ventureCapitalFeature.rawScore(seedArticle));
    }
    System.out.println("Articles:");
    int i = 0;
    for (String article : topArticles) {
      System.out.println(++i + ". " + topArticles.getValue(article) + ": " + article);
    }

    System.out.println("10% quantile: " + ventureCapitalFeature.getSimilarityThreshold10Percent());
    System.out.println("50% quantile: " + ventureCapitalFeature.getSimilarityThreshold50Percent());
  }

  public static void main2(String args[]) throws Exception {
    for (String email : new String[] { "tom.charytoniuk@gmail.com" }) {
      Persona persona = Personas.getByEmail(email);
      Map<String, Article> goodArticles = ArticleCrawler.getArticles(persona.getGoodUrlList(), true);
      for (Article article : goodArticles.values()) {
        double score = SocialEngagements.getForArticle(article, Site.TWITTER).getShareScore();
        if (score < 0.2) {
          System.out.println(score + " twitter: " + article.getUrl());
        }
        score = SocialEngagements.getForArticle(article, Site.FACEBOOK).getShareScore();
        if (score < 0.2) {
          System.out.println(score + " facebook: " + article.getUrl());
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
