package com.janknspank.notifications;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.Lists;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.bizness.TimeRankingStrategy.MainStreamStrategy;
import com.janknspank.bizness.UserInterests;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.notifications.nnet.ArticleEvaluation;
import com.janknspank.notifications.nnet.NotificationNeuralNetworkScorer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.NotificationsProto.DeviceRegistration;
import com.janknspank.proto.NotificationsProto.DeviceType;
import com.janknspank.proto.NotificationsProto.Notification;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.Deduper;
import com.janknspank.rank.DiversificationPass;
import com.janknspank.rank.NeuralNetworkScorer;

/**
 * Sends every user who's enabled iOS push notifications a notification about
 * the top article in their stream.  We'll run this once daily in the morning
 * to help with re-engagement.
 */
public class PushDeviceNotifications {
  private static final Set<String> USERS_TO_INCLUDE_SCORES_ON_NOTIFICATIONS =
      ImmutableSet.of("jon@jonemerson.net", "panaceaa@gmail.com", "tom.charytoniuk@gmail.com");

  private static class PreviousUserNotifications {
    private final Future<Iterable<Notification>> recentNotificationsFuture;

    public PreviousUserNotifications(User user) throws DatabaseSchemaException {
      recentNotificationsFuture = Database.with(Notification.class).getFuture(
          new QueryOption.WhereEquals("user_id", user.getId()),
          new QueryOption.WhereEqualsEnum("device_type", DeviceType.IOS),
          new QueryOption.DescendingSort("create_time"),
          new QueryOption.Limit(5));
    }

    private long getLastNotificationTime() throws DatabaseSchemaException {
      try {
        Notification lastNotification = Iterables.getFirst(recentNotificationsFuture.get(), null);
        if (lastNotification != null) {
          return lastNotification.getCreateTime();
        }
      } catch (InterruptedException | ExecutionException e) {
        Throwables.propagateIfInstanceOf(e.getCause(), DatabaseSchemaException.class);
      }
      return 0;
    }

    private boolean isDupe(Article article) throws DatabaseSchemaException {
      Deduper.ArticleExtraction articleExtraction = new Deduper.ArticleExtraction(article);
      try {
        for (Notification pushNotification : recentNotificationsFuture.get()) {
          Deduper.ArticleExtraction pushExtraction = new Deduper.ArticleExtraction(
              pushNotification.getArticlePublishedTime(), pushNotification.getDedupingStemsList());
          if (pushExtraction.isDuplicate(articleExtraction)) {
            return true;
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        Throwables.propagateIfInstanceOf(e.getCause(), DatabaseSchemaException.class);
      }
      return false;
    }
  }

  private static Set<String> getFollowedEntityIds(User user) {
    ImmutableSet.Builder<String> followedEntityIdSetBuilder = ImmutableSet.builder();
    for (Interest interest : UserInterests.getInterests(user)) {
      if (interest.hasEntity()) {
        followedEntityIdSetBuilder.add(interest.getEntity().getId());
      }
    }
    return followedEntityIdSetBuilder.build();
  }

  public static boolean isInTestGroup(String userId) {
    char c = userId.charAt(userId.length() - 1);
    return c % 2 == 0;
  }

  /**
   * Returns a score between 0 and 300 indicating how important this article
   * would be for notification-purposes for the given user.
   */
  private static int getArticleNotificationScore(
      User user, Article article, Set<String> followedEntityIds) {
    // A/B test the new algorithm.
    if (isInTestGroup(user.getId())
        || "juliencadot@gmail.com".equals(user.getEmail())
        || USERS_TO_INCLUDE_SCORES_ON_NOTIFICATIONS.contains(user.getEmail())) {
      double score = NotificationNeuralNetworkScorer.getInstance()
          .getNormalizedScore(article, followedEntityIds);
      return Math.max(0, (int) (210 * (score * 3 - 2)));
    }

    // 0 out of 100 possible for ranking score.
    int score = (int) (article.getScore() * 100);

    // Slight punishment for older articles, so that we tend to notify about
    // newly published topics as opposed to things the user might have seen
    // on other news aggregators recently.
    if (((System.currentTimeMillis() - Articles.getPublishedTime(article))
        / TimeUnit.HOURS.toMillis(1)) >= 3) {
      score -= 20;
    }

    // -25 to 100 depending on whether the article's about a company, and
    // whether the user's following that company.
    ArticleEvaluation evaluation = new ArticleEvaluation(article, followedEntityIds);
    if (evaluation.isFollowedCompany()) {
      // Users click on these notifications 57% more than average.
      score += 100;
    } else if (evaluation.isCompany()) {
      // Counter-intuitively, this is a negative signal: If an article's about
      // a company, and the user hasn't specified an interest in that company,
      // he's less likely than average to be interested in it.  In fact, click-
      // through on notifications about companies the user isn't following are
      // 27% lower than average.
      score -= 25;
    }

    // 0 out of 100 depending on whether there's dupes or this article seems
    // event-like.
    if (evaluation.isEvent()) {
      // We actually have not noticed much correlation between this attribute
      // and click-through.  Let's re-evaluate in the future.
      score += 5;
    }
    if (evaluation.getHotCount() > 3) {
      // Articles with dupe scores of 2 actually have no correlation to
      // engagement.  So we only reward very highly duped articles, which
      // can get engagement up to 2x normal.
      score += 95;
    } else if (evaluation.getHotCount() == 3) {
      score += 50;
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

  private static Article getArticleToNotifyAbout(User user, Set<String> followedEntityIds)
      throws DatabaseSchemaException, BiznessException {
    PreviousUserNotifications previousUserNotifications = new PreviousUserNotifications(user);

    UserTimezone userTimezone = UserTimezone.getForUser(user, true /* update */);
    if (userTimezone.isNight()) {
      // Don't even risk sending anything at night...
      return null;
    }

    // Get the user's stream.
    Iterable<Article> rankedArticles = Articles.getRankedArticles(
        user,
        NeuralNetworkScorer.getInstance(),
        new MainStreamStrategy(),
        new DiversificationPass.MainStreamPass(),
        25 /* results */,
        ImmutableSet.<String>of());

    // Don't consider articles older than the last time the user used the app,
    // the last time we sent him/her a notification, or 8 hours.
    long lastNotificationTime = previousUserNotifications.getLastNotificationTime();
    long timeCutoff = Math.max(getLastAppUseTime(user), lastNotificationTime)
        - TimeUnit.MINUTES.toMillis(30);
    timeCutoff = Math.max(timeCutoff, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(8));

    // Find the best article in the user's stream, for notification purposes.
    Article bestArticle = null;
    int bestArticleScore = -1;
    for (Article article : rankedArticles) {
      // Don't consider articles that are older than the time cutoff or articles
      // that are duplicates of notifications we previously sent.
      if (Articles.getPublishedTime(article) < timeCutoff
          || (article.hasOldestHotDuplicateTime()
              && article.getOldestHotDuplicateTime() < timeCutoff)
          || previousUserNotifications.isDupe(article)) {
        continue;
      }

      int score = getArticleNotificationScore(user, article, followedEntityIds);
      if (score > 0 && score > bestArticleScore) {
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
    int scoreNecessaryToTriggerNotification = 200 - (10 * hoursSinceNotification);
    if (userTimezone.isMorning()) {
      // Encourage more notifications in the morning.
      scoreNecessaryToTriggerNotification -= 35;
    } else if (userTimezone.isDaytime()) {
      // And some encouragement in the afternoon, too, but not as much...
      scoreNecessaryToTriggerNotification -= 20;
    }
    if (userTimezone.isWeekend()) {
      // Only notify people on weekends if it's important.
      scoreNecessaryToTriggerNotification = Math.max(scoreNecessaryToTriggerNotification, 160);
    }
    if (bestArticle != null
        && bestArticleScore >= scoreNecessaryToTriggerNotification) {
      if (USERS_TO_INCLUDE_SCORES_ON_NOTIFICATIONS.contains(user.getEmail())) {
        return bestArticle.toBuilder()
            .setTitle(bestArticleScore + "/" + scoreNecessaryToTriggerNotification + " "
                + bestArticle.getTitle())
            .build();
      } else {
        return bestArticle;
      }
    }
    return null;
  }

  private static class NotificationCallable implements Callable<Void> {
    private final User user;

    private NotificationCallable(User user) {
      this.user = user;
    }

    @Override
    public Void call() {
      try {
        Iterable<DeviceRegistration> registrations =
            IosPushNotificationHelper.getDeviceRegistrations(user);
        if (!Iterables.isEmpty(registrations)) {
          Set<String> followedEntityIds = getFollowedEntityIds(user);
          Article bestArticle = getArticleToNotifyAbout(user, followedEntityIds);
          if (bestArticle != null) {
            System.out.println("Sending \"" + bestArticle.getTitle() + "\" to " + user.getEmail());
            ArticleEvaluation evaluation = new ArticleEvaluation(bestArticle, followedEntityIds);
            for (DeviceRegistration registration : registrations) {
              Notification.Builder pushNotificationBuilder =
                  IosPushNotificationHelper.createPushNotification(registration, bestArticle)
                      .toBuilder()
                      .setIsEvent(evaluation.isEvent())
                      .setIsCompany(evaluation.isCompany())
                      .setIsFollowedCompany(evaluation.isFollowedCompany())
                      .setHotCount(bestArticle.getHotCount())
                      .setScore(bestArticle.getScore())
                      .setNotificationScore(getArticleNotificationScore(
                          user, bestArticle, followedEntityIds))
                      .setNnetScore(NotificationNeuralNetworkScorer.getInstance()
                          .getNormalizedScore(bestArticle, followedEntityIds))
                      .setAgeInMillis(System.currentTimeMillis()
                          - Articles.getPublishedTime(bestArticle));

              SocialEngagement twitterEngagement =
                  SocialEngagements.getForArticle(bestArticle, Site.TWITTER);
              if (twitterEngagement != null) {
                pushNotificationBuilder.setTwitterScore(twitterEngagement.getShareScore());
              }

              SocialEngagement facebookEngagement =
                  SocialEngagements.getForArticle(bestArticle, Site.FACEBOOK);
              if (facebookEngagement != null) {
                pushNotificationBuilder.setTwitterScore(facebookEngagement.getShareScore());
              }

              IosPushNotificationHelper.getInstance().sendPushNotification(
                  pushNotificationBuilder.build());
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  public static void main(String args[]) throws Exception {
    // As part of this process, also send emails.
    SendWelcomeEmails.sendWelcomeEmails();
    SendLunchEmails.sendLunchEmails();

    // OK, now send push notifications.
    ExecutorService executor = Executors.newFixedThreadPool(20);
    List<Callable<Void>> callables = Lists.newArrayList();
    for (User user : Database.with(User.class).get()) {
      callables.add(new NotificationCallable(user));
    }
    executor.invokeAll(callables);
    executor.shutdown();

    System.exit(0);
  }
}
