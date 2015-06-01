package com.janknspank.notifications;

import java.util.Set;

import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.NotificationsProto.Notification.Algorithm;

public interface NotificationScorer {
  public Algorithm getAlgorithm();

  /**
   * Returns a score between 0 and 300 indicating how important this article
   * would be for notification-purposes for the given user.
   */
  public int getScore(Article article, Set<String> followedEntityIds);

  /**
   * Returns the minimum score necessary for actually sending an article out
   * to a user via push.
   */
  public int getScoreNecessaryToTriggerNotification(
      long lastNotificationTime, UserTimezone userTimezone);
}
