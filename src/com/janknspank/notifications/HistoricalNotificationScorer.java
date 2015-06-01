package com.janknspank.notifications;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.janknspank.bizness.Articles;
import com.janknspank.notifications.nnet.ArticleEvaluation;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.NotificationsProto.Notification.Algorithm;

public class HistoricalNotificationScorer implements NotificationScorer {
  @Override
  public Algorithm getAlgorithm() {
    return Algorithm.HISTORICAL;
  }

  /**
   * Returns a score between 0 and 300 indicating how important this article
   * would be for notification-purposes for the given user.
   */
  @Override
  public int getScore(Article article, Set<String> followedEntityIds) {
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
   * Depending on how important we find this article and what time of day it
   * is, return it, or null to indicate that no notification should be sent.
   * FYI 300 = the highest possible notification score.  So we have a time
   * fall-off between notifications so that eventually we'll send a
   * notification, and usually it'll be an important one.
   */
  @Override
  public int getScoreNecessaryToTriggerNotification(
      long lastNotificationTime, UserTimezone userTimezone) {
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
      scoreNecessaryToTriggerNotification = Math.max(scoreNecessaryToTriggerNotification, 180);
    }
    return scoreNecessaryToTriggerNotification;
  }
}
