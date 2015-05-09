package com.janknspank.utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.janknspank.classifier.FeatureClassifier;
import com.janknspank.database.Database;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;

/**
 * Updates the "features" repeated Feature set on every single article in the
 * corpus.
 */
public class UpdateArticleFeatures {
  private static final int THREAD_COUNT = 10;
  private static int numUpdated = 0;

  private static synchronized int incrementNumUpdated() {
    return ++numUpdated;
  }

  public static class Updater implements Callable<Void> {
    private final Iterable<Article> articles;
    private final int totalArticleCount;

    public Updater(Iterable<Article> articles, int totalArticleCount) {
      this.articles = articles;
      this.totalArticleCount = totalArticleCount;
    }

    @Override
    public Void call() throws Exception {
      List<Article> articlesToUpdate = Lists.newArrayList();
      for (Article article : articles) {
        articlesToUpdate.add(article.toBuilder()
            .clearFeature()
            .addAllFeature(FeatureClassifier.classify(article))
            .build());
        int numUpdated = incrementNumUpdated();
        if (numUpdated % 1000 == 0) {
          System.out.println(numUpdated + " of " + totalArticleCount
              + " (" + (numUpdated * 100 / totalArticleCount) + "%)");
        } else if (numUpdated % 200 == 0) {
          System.out.print(".");
          articlesToUpdate.clear();
        }
      }
      Database.update(articlesToUpdate);
      return null;
    }
  }

  private static void update(boolean retain, long publishTimeStart, long publishTimeEnd)
      throws Exception {
    numUpdated = 0;
    System.out.println("\nReading " + (retain ? "training" : "non-training") + " articles...");
    List<Article> articles = ImmutableList.copyOf(
        Database.with(Article.class).get(
            retain
                ? new QueryOption.WhereTrue("retain")
                : new QueryOption.WhereNotTrue("retain"),
            new QueryOption.WhereGreaterThanOrEquals("published_time", publishTimeStart),
            new QueryOption.WhereLessThanOrEquals("published_time", publishTimeEnd),
            new QueryOption.DescendingSort("published_time")));
    int totalArticleCount = articles.size();
    System.out.println(totalArticleCount + " articles received.  "
        + "Starting " + (retain ? "training" : "non-training") + " update...");

    List<Updater> updaters = Lists.newArrayList();
    for (List<Article> sublist : Lists.partition(articles, 100)) {
      updaters.add(new Updater(sublist, totalArticleCount));
    }

    // Start the threads!
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    executor.invokeAll(updaters);
    executor.shutdown();
  }

  public static void main(String args[]) throws Exception {
    // Update the training articles first so we can start regenerating the
    // neural network when this updater's still only half-way.
    update(true, 0, Long.MAX_VALUE);

    // Now, go in date order, newest first.
    int numDays = 7;
    for (int daysAgo = 1; daysAgo <= numDays; daysAgo++) {
      long publishTimeStart = (daysAgo == numDays)
          ? 0
          : System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysAgo);
      long publishTimeEnd = (daysAgo == 1)
          ? Long.MAX_VALUE
          : System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysAgo - 1);
      update(false, publishTimeStart, publishTimeEnd);
    }
  }
}
