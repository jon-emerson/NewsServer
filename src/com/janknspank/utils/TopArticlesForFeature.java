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
  /**
   * Called by toparticlesforfeature.sh.
   * @param args an array of feature Ids (ex. 30001 for Launches) to query for
   */
  public static void main(String args[]) throws DatabaseSchemaException {
    // Args are the industries code ids to benchmark
    if (args.length == 0) {
      System.out.println("ERROR: You must pass feature IDs as parameters.");
      System.out.println("Example: ./toparticlesforfeature.sh 30001");
      System.exit(-1);
    }

    Iterable<Article> articles = Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(10000));

    for (String arg : args) {
      TopList<Article, Double> topRankingArticles = new TopList<>(100);

      for (Article article : articles) {
        ArticleFeature articleFeature =
            ArticleFeatures.getFeature(article, FeatureId.fromId(Integer.parseInt(arg)));
        if (articleFeature != null) {
          topRankingArticles.add(article, articleFeature.getSimilarity());
        }
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
}
