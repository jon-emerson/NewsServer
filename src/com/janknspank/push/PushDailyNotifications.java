package com.janknspank.push;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.UserInterests;
import com.janknspank.bizness.Users;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.TopList;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.PushNotificationProto.DeviceRegistration;
import com.janknspank.proto.PushNotificationProto.PushNotification;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

/**
 * Sends every user who's enabled iOS push notifications a notification about
 * the top article in their stream.  We'll run this once daily in the morning
 * to help with re-engagement.
 */
public class PushDailyNotifications {
  private static Set<String> getFollowedEntityIds(User user) {
    ImmutableSet.Builder<String> followedEntityIdSetBuilder = ImmutableSet.builder();
    for (Interest interest : UserInterests.getInterests(user)) {
      if (interest.hasEntity()) {
        followedEntityIdSetBuilder.add(interest.getEntity().getId());
      }
    }
    return followedEntityIdSetBuilder.build();
  }

  private static boolean isArticleAboutEvent(Article article) {
    return ArticleFeatures.getFeatureSimilarity(article,
            FeatureId.MANUAL_HEURISTIC_ACQUISITIONS) >= 0.5
        || ArticleFeatures.getFeatureSimilarity(article,
            FeatureId.MANUAL_HEURISTIC_FUNDRAISING) >= 0.5
        || ArticleFeatures.getFeatureSimilarity(article,
            FeatureId.MANUAL_HEURISTIC_LAUNCHES) >= 0.5
        || ArticleFeatures.getFeatureSimilarity(article,
            FeatureId.MANUAL_HEURISTIC_QUARTERLY_EARNINGS) >= 0.5;
  }

  private static boolean isArticleAboutFollowedCompany(Article article, Set<String> followedEntityIds) {
    for (ArticleKeyword keyword : article.getKeywordList()) {
      if (keyword.getStrength() >= 100
          && keyword.hasEntity()
          && followedEntityIds.contains(keyword.getEntity().getId())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isArticleAboutCompany(Article article) {
    for (ArticleKeyword keyword : article.getKeywordList()) {
      if (keyword.getStrength() >= 100
          && EntityType.fromValue(keyword.getType()).isA(EntityType.ORGANIZATION)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a score between 0 and 300 indicating how important this article
   * would be for notification-purposes for the given user.
   */
  private static int getArticleNotificationScore(
      Article article, Set<String> followedEntityIds, double neuralNetworkScore) {
    // 0 out of 100 possible for ranking score.
    int score = (int) (neuralNetworkScore * 100);

    // Slight punishment for older articles, so that we tend to notify about
    // newly published topics as opposed to things the user might have seen
    // on other news aggregators recently.
    if (((System.currentTimeMillis() - Articles.getPublishedTime(article))
        / TimeUnit.HOURS.toMillis(1)) >= 3) {
      score -= 20;
    }

    // 0, 75, or 100 depending on whether the article's about a company, and
    // whether the user's following that company.
    if (isArticleAboutFollowedCompany(article, followedEntityIds)) {
      score += 100;
    } else if (isArticleAboutCompany(article)) {
      // Don't make this too high.  In many industries, companies are far less
      // interesting than ideas... E.g. architecture, where when this value was
      // to high, we sent articles about airlines to folks because those
      // articles mentioned a company while design articles didn't.
      score += 25;
    }

    // 0, 75, or 100 depending on whether there's dupes or this article seems
    // event-like.
    if (article.getHotCount() > 2) {
      score += 100;
    } else if (isArticleAboutEvent(article) || article.getHotCount() == 2) {
      score += 75;
    }
    return score;
  }

  /**
   * Returns the last time the user used the app.
   */
  private static long getLastAppUseTime(User user) {
    long lastAppUseTime = 0;
    for (long appUseTime : user.getLast5AppUseTimeList()) {
      lastAppUseTime = Math.max(lastAppUseTime, appUseTime);
    }
    return lastAppUseTime;
  }

  private static long getLastNotificationTime(User user) throws DatabaseSchemaException {
    PushNotification lastNotification = Database.with(PushNotification.class).getFirst(
        new QueryOption.WhereEquals("user_id", user.getId()),
        new QueryOption.DescendingSort("create_time"));
    return (lastNotification == null) ? 0 : lastNotification.getCreateTime();
  }

  private static Article getArticleToNotifyAbout(User user)
      throws DatabaseSchemaException, BiznessException {
    UserTimezone userTimezone = UserTimezone.getForUser(user);
    if (userTimezone.isNight()) {
      // Don't even risk sending anything at night...
      return null;
    }

    Set<String> followedEntityIds = getFollowedEntityIds(user);

    // Don't consider articles older than the last time the user used the app or
    // the last time we sent him/her a notification.
    long lastNotificationTime = getLastNotificationTime(user);
    long timeCutoff = Math.max(getLastAppUseTime(user), lastNotificationTime)
        - TimeUnit.MINUTES.toMillis(30);

    // Get the user's stream.
    TopList<Article, Double> rankedArticles =
        Articles.getRankedArticles(user, NeuralNetworkScorer.getInstance(), 40);

    // Find the best article in the user's stream, for notification purposes.
    Article bestArticle = null;
    int bestArticleScore = -1;
    for (Article article : rankedArticles) {
      // Don't consider articles that are older than the time cutoff.
      if (Articles.getPublishedTime(article) < timeCutoff
          || (article.hasOldestHotDuplicateTime()
              && article.getOldestHotDuplicateTime() < timeCutoff)) {
        continue;
      }

      int score = getArticleNotificationScore(
          article, followedEntityIds, rankedArticles.getValue(article));
      if (score > bestArticleScore) {
        bestArticleScore = score;
        bestArticle = article;
      }
    }

    // Depending on how important we find this article and what time of day it
    // is, return it, or null to indicate that no notification should be sent.
    // FYI 300 = the highest possible notification score.  So we have a time
    // fall-off between notifications so that eventually we'll send a
    // notification, and usually it'll be an important one.
    int hoursSinceNotification =
        (int) ((System.currentTimeMillis() - lastNotificationTime) / TimeUnit.HOURS.toMillis(1));
    int scoreNecessaryToTriggerNotification = 250 - (10 * hoursSinceNotification);
    if (userTimezone.isMorning()) {
      // Encourage more notifications in the morning.
      scoreNecessaryToTriggerNotification -= 50;
    }
    if (userTimezone.isWeekend()) {
      // Only notify people on weekends if it's important.
      scoreNecessaryToTriggerNotification = Math.max(scoreNecessaryToTriggerNotification, 200);
    }
    if (bestArticle != null
        && bestArticleScore >= scoreNecessaryToTriggerNotification) {
      return bestArticle.toBuilder()
          .setTitle(bestArticleScore + "/" + scoreNecessaryToTriggerNotification + " "
              + bestArticle.getTitle())
          .build();
    }
    return null;
  }

  public static void testMain(String args[]) throws Exception {
    User user = Users.getByEmail("jon@jonemerson.net");
    Article article = Iterables.getFirst(ArticleCrawler.getArticles(
        ImmutableList.of("http://www.telegraph.co.uk/news/worldnews/asia/"
            + "nepal/11563157/Google-person-finder-tool-deployed-to-help-"
            + "relatives-find-loved-ones-in-Nepal.html"), true /* retain */).values(), null);
    describeArticleUser(article, user);

    TopList<Article, Double> rankedArticles =
        Articles.getRankedArticles(user, NeuralNetworkScorer.getInstance(), 40);
    long lastNotificationTime = getLastNotificationTime(user);
    long timeCutoff = Math.max(getLastAppUseTime(user), lastNotificationTime);
    int i = 0;
    for (Article rankedArticle : rankedArticles) {
      if (rankedArticle.getCrawlTime() < timeCutoff) {
        continue;
      }
      if (i++ >= 10) {
        continue;
      }
      describeArticleUser(rankedArticle, user);
    }
  }

  private static void describeArticleUser(Article article, User user) {
    System.out.println("\"" + article.getTitle() + "\"");
    System.out.println(article.getUrl());
    Set<String> followedEntityIds = getFollowedEntityIds(user);
    System.out.println("isAboutEvent = " + isArticleAboutEvent(article));
    System.out.println("isAboutFollowedCompany = " + isArticleAboutFollowedCompany(article, followedEntityIds));
    System.out.println("isAboutCompany = " + isArticleAboutCompany(article));
    double neuralNetworkScore = NeuralNetworkScorer.getInstance().getScore(user, article);

    int hotScore = 0;
    if (article.getHotCount() > 2) {
      hotScore += 100;
    } else if (isArticleAboutEvent(article) || article.getHotCount() == 2) {
      hotScore += 75;
    }
    System.out.println("hotScore = " + hotScore);

    System.out.println("neuralNetworkScore = " + neuralNetworkScore);
    System.out.println("score = "
        + getArticleNotificationScore(article, followedEntityIds, neuralNetworkScore));
    System.out.println();
  }

  public static void main(String args[]) throws Exception {
    for (User user : Database.with(User.class).get()) {
      try {
        Iterable<DeviceRegistration> registrations =
            IosPushNotificationHelper.getDeviceRegistrations(user);
        if (!Iterables.isEmpty(registrations)) {
          Article bestArticle = getArticleToNotifyAbout(user);
          if (bestArticle != null) {
            System.out.println("Sending \"" + bestArticle.getTitle() + "\" to " + user.getEmail());
            for (DeviceRegistration registration : registrations) {
              PushNotification pushNotification =
                  IosPushNotificationHelper.createPushNotification(registration, bestArticle);
              IosPushNotificationHelper.getInstance().sendPushNotification(pushNotification);
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    System.exit(0);
  }
}
