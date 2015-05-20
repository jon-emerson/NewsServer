package com.janknspank.utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.Lists;
import com.google.common.collect.Iterables;
import com.google.protobuf.Message;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.crawler.social.FacebookData;
import com.janknspank.crawler.social.TwitterData;
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
      List<Message> engagements = Lists.newArrayList();
      try {
        SocialEngagement engagement = FacebookData.getEngagementForArticle(article);
        if (engagement != null) {
          engagements.add(engagement);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      try {
        SocialEngagement engagement = TwitterData.getEngagementForArticle(article);
        if (engagement != null) {
          engagements.add(engagement);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (engagements.size() > 0) {
        Database.push(article, "social_engagement", engagements);
      }
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

    // NOTE: WE USED TO BE SUPER AGGRESSIVE HERE BUT FACEBOOK STARTED THROTTLING
    // OUR REQUESTS!!  This was especially bad because we shared our Facebook app
    // ID across both frontend and crawling, which broke facebook authentication
    // for end users.  To fix this, we switched to a secondary app ID, so that
    // the two quotas are isolated - but this means we'll have a small quota for
    // the crawler for the foreseeable future.  Someday we might want to switch
    // back to the frontend app ID / secret so that we can get higher QPS quotas
    // (since query quota seems to be associated with # of users an app has).

    // For articles between 0 and 8 hours old, update social engagements if we
    // haven't updated them in at least 6 hours.
    if (articleAgeInHours < 8) {
      return socialEngagementAgeInHours > 6;
    }

    // For articles between 8 and 24 hours old, update social engagements if we
    // haven't updated them in at least 12 hours.
    if (articleAgeInHours < 24) {
      return socialEngagementAgeInHours > 12;
    }

    // For articles between 24 hours and 2 1/2 days old, update social
    // engagements if we haven't updated them in at least 24 hours.
    if (articleAgeInHours < (24 + 24 + 12)) {
      return socialEngagementAgeInHours > 24;
    }

    return false;
  }

  public static void main(String args[]) throws DatabaseSchemaException {
    Iterable<Article> articles = Database.with(Article.class).get(
        new WhereOption.WhereGreaterThan("published_time",
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24 + 24 + 12)));
    System.out.println(Iterables.size(articles) + " articles found");

    ExecutorService executor = Executors.newFixedThreadPool(20);
    List<Updater> callables = Lists.newArrayList();
    for (Article article : articles) {
      if (needsUpdate(article)) {
        callables.add(new Updater(article));
      }
    }
    try {
      executor.invokeAll(callables);
      executor.shutdown();
      executor.awaitTermination(TimeUnit.MINUTES.toMillis(20), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {}
  }
}
