package com.janknspank.utils;

import com.google.common.collect.Iterables;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;

public class Helper {
  public static void main(String args[]) throws DatabaseSchemaException {
    for (Article article : Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(20000))) {
      ArticleFeature launchFeature =
          ArticleFeatures.getFeature(article, FeatureId.MANUAL_HEURISTIC_FUNDRAISING);
      if (launchFeature.getSimilarity() > 0.1) {
        System.out.println("\"" + article.getTitle() + "\" (" + launchFeature.getSimilarity() + ")");
        System.out.println(article.getUrl());
        System.out.println("First paragraph: \""
            + Iterables.getFirst(article.getParagraphList(), "") + "\"");
        System.out.println();
      }
    }
  }
}
