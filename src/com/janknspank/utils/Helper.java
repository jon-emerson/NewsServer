package com.janknspank.utils;

import java.util.Map;

import com.google.common.collect.Iterables;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.FeatureId;
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

  public static void main2(String args[]) throws DatabaseSchemaException {
    for (Article article : Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(5000))) {
      double featureSimilarity =
          ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_QUARTERLY_EARNINGS);
      if (ArticleFeatures.getFeatureSimilarity(article, FeatureId.SOFTWARE) > 0
          && featureSimilarity > 0.1) {
        System.out.println("\"" + article.getTitle() + "\" (" + featureSimilarity + ")");
        System.out.println(article.getUrl());
        System.out.println("First paragraph: \""
            + Iterables.getFirst(article.getParagraphList(), "") + "\"");
        System.out.println();
      }
    }
  }
}
