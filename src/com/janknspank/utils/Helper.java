package com.janknspank.utils;

import java.util.Map;

import com.google.common.collect.Iterables;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.Feature;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.VectorFeatureCreator;
import com.janknspank.common.TopList;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.rank.Personas;

public class Helper {
  public static void main(String args[]) throws Exception {
    Iterable<Article> seedArticles =
        new VectorFeatureCreator(FeatureId.VENTURE_CAPITAL).getSeedArticles();
    Feature ventureCapitalFeature = Feature.getFeature(FeatureId.VENTURE_CAPITAL);

    TopList<String, Double> bestArticles = new TopList<String, Double>(20);
    TopList<String, Double> worstArticles = new TopList<String, Double>(20);
    for (Article seedArticle : seedArticles) {
      double score = ventureCapitalFeature.score(seedArticle);
      bestArticles.add(seedArticle.getUrl(), score);
      worstArticles.add(seedArticle.getUrl(), 1 - score);
    }
    System.out.println("Best articles:");
    for (String bestArticle : bestArticles) {
      System.out.println("  " + bestArticles.getValue(bestArticle) + ": " + bestArticle);
    }
    System.out.println("Worst articles:");
    for (String worstArticle : worstArticles) {
      System.out.println("  " + (1 - worstArticles.getValue(worstArticle)) + ": " + worstArticle);
    }
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
