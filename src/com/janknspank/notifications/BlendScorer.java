package com.janknspank.notifications;

import java.util.Set;

import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.notifications.nnet.NotificationNeuralNetworkScorer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.NotificationsProto.Notification.Algorithm;
import com.janknspank.proto.UserProto.User;

public class BlendScorer implements NotificationScorer {
  private final NotificationScorer historicalScorer = new HistoricalNotificationScorer();
  private final NotificationScorer nnetScorer = NotificationNeuralNetworkScorer.getInstance();

  @Override
  public Algorithm getAlgorithm() {
    return Algorithm.BLEND;
  }

  @Override
  public int getScore(Article article, Set<String> followedEntityIds) {
    return (historicalScorer.getScore(article, followedEntityIds)
        + nnetScorer.getScore(article, followedEntityIds)) / 2;
  }

  @Override
  public int getScoreNecessaryToTriggerNotification(long lastNotificationTime,
      UserTimezone userTimezone) {
    return
        (historicalScorer.getScoreNecessaryToTriggerNotification(
            lastNotificationTime, userTimezone)
        + nnetScorer.getScoreNecessaryToTriggerNotification(
            lastNotificationTime, userTimezone)) / 2;
  }

  @Override
  public Iterable<Article> getArticles(User user) throws DatabaseSchemaException, BiznessException {
    return historicalScorer.getArticles(user);
  }
}
