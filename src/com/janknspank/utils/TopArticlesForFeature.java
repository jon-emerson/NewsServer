package com.janknspank.utils;

import com.google.common.collect.Iterables;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;

public class TopArticlesForFeature {
  public static void main(String args[]) throws DatabaseSchemaException {
    TopList<Article, Double> topRankingArticles = new TopList<>(100);

    for (Article article : Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(5000))) {
      ArticleFeature launchFeature =
          ArticleFeatures.getFeature(article, FeatureId.MANUAL_HEURISTIC_LAUNCHES);
      topRankingArticles.add(article, launchFeature.getSimilarity());
    }

    for (Article article : topRankingArticles.getKeys()) {
      System.out.println("\"" + article.getTitle() + "\" (" + topRankingArticles.getValue(article) + ")");
      System.out.println(article.getUrl());
      System.out.println("First paragraph: \""
          + Iterables.getFirst(article.getParagraphList(), "") + "\"");
      System.out.println();
    }
  }
}
