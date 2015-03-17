package com.janknspank.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.crawler.facebook.FacebookData;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption.WhereOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;

/**
 * A scheduled task that gives all recent articles updated social engagements.
 */
public class UpdateSocialEngagements {
  private static class Updater implements Callable<Void> {
    private final Article article;

    public Updater(Article article) {
      this.article = article;
    }

    @Override
    public Void call() throws Exception {
      long start = System.currentTimeMillis();
      SocialEngagement socialEngagement = FacebookData.getEngagementForURL(article);
      Database.set(article, "social_engagement", ImmutableList.of((Object) socialEngagement));
      return null;
    }
  }

  private static boolean needsUpdate(Article article) {
    double articleAgeInHours =
        (double) (System.currentTimeMillis() - article.getPublishedTime())
        / TimeUnit.HOURS.toMillis(1);
    SocialEngagement socialEngagement = SocialEngagements.getForArticle(article, Site.FACEBOOK);
    double socialEngagementAgeInHours =
        socialEngagement == null
            ? Double.MAX_VALUE
            : (double) (System.currentTimeMillis() - socialEngagement.getCreateTime())
                  / TimeUnit.HOURS.toMillis(1);

    // For articles between 0 and 8 hours old, update social engagements if we
    // haven't updated them in at least 3 hours.
    if (articleAgeInHours < 8) {
      return socialEngagementAgeInHours > 3;
    }

    // For articles between 8 and 24 hours old, update social engagements if we
    // haven't updated them in at least 6 hours.
    if (articleAgeInHours < 24) {
      return socialEngagementAgeInHours > 6;
    }

    // For articles between 24 hours and 2 1/2 days old, update social
    // engagements if we haven't updated them in at least 12 hours.
    if (articleAgeInHours < (24 + 24 + 12)) {
      return socialEngagementAgeInHours > 12;
    }

    return false;
  }

  public static void main(String args[]) throws DatabaseSchemaException {
    Iterable<Article> articles = Database.with(Article.class).get(
        new WhereOption.WhereGreaterThan("published_time",
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24 + 24 + 12)));
    System.out.println(Iterables.size(articles) + " articles found");

    ExecutorService executor = Executors.newFixedThreadPool(20);
    for (Article article : articles) {
      if (needsUpdate(article)) {
        executor.submit(new Updater(article));
      }
    }
    executor.shutdown();
    try {
      executor.awaitTermination(TimeUnit.MINUTES.toMillis(20), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {}
  }
}
