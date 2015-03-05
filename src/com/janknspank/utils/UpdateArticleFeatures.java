package com.janknspank.utils;

import java.util.List;

import com.google.api.client.util.Lists;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.classifier.FeatureClassifier;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;

/**
 * Updates the "features" repeated Feature set on every single article in the
 * corpus.
 */
public class UpdateArticleFeatures {
  public static void main(String args[])
      throws DatabaseSchemaException, ClassifierException, DatabaseRequestException {
    List<Article> articlesToUpdate = Lists.newArrayList();
    int i = 0;
    long numArticles = Database.with(Article.class).getSize();
    for (Article article : Database.with(Article.class).get()) {
      articlesToUpdate.add(article.toBuilder()
          .clearFeature()
          .addAllFeature(FeatureClassifier.classify(article))
          .build());
      ++i;
      if (articlesToUpdate.size() > 200) {
        Database.update(articlesToUpdate);
        articlesToUpdate.clear();
        System.out.print(".");
      }
      if (i % 1000 == 0) {
        System.out.println(i + " of " + numArticles + " (" + (i * 100 / numArticles) + "%)");
      }
    }
    Database.update(articlesToUpdate);
  }
}
