package com.janknspank.notifications.nnet;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.learning.BackPropagation;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Doubles;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.TimeRankingStrategy.MainStreamStrategy;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.notifications.NotificationScorer;
import com.janknspank.notifications.UserTimezone;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.proto.NotificationsProto.Notification.Algorithm;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.DistributionBuilder;
import com.janknspank.rank.DiversificationPass;
import com.janknspank.rank.NeuralNetworkScorer;

public final class NotificationNeuralNetworkScorer implements NotificationScorer {
  static final String DEFAULT_NEURAL_NETWORK_FILE =
      "neuralnet/notifications_backpropagation_out.nnet";
  static final String DISTRIBUTION_FILE =
      "neuralnet/notifications_backpropagation_out.distribution";
  private static NotificationNeuralNetworkScorer instance = null;
  private NeuralNetwork<BackPropagation> neuralNetwork;
  private Distribution distribution;

  @SuppressWarnings("unchecked")
  private NotificationNeuralNetworkScorer() {
    neuralNetwork = NeuralNetwork.createFromFile(DEFAULT_NEURAL_NETWORK_FILE);
    try {
      distribution = Distribution.parseFrom(new FileInputStream(DISTRIBUTION_FILE));
    } catch (IOException e) {
      throw new Error("Could not read distribution file", e);
    }
  }

  @Override
  public Algorithm getAlgorithm() {
    return Algorithm.NNET;
  }

  public NotificationNeuralNetworkScorer(NeuralNetwork<BackPropagation> neuralNetwork) {
    this.neuralNetwork = neuralNetwork;
  }

  public static synchronized NotificationNeuralNetworkScorer getInstance() {
    if (instance == null) {
      instance = new NotificationNeuralNetworkScorer();
    }
    return instance;
  }

  /**
   * Returns a score between 0 and 300 indicating how important this article
   * would be for notification-purposes for the given user.
   */
  @Override
  public int getScore(Article article, Set<String> followedEntityIds) {
    double normalizedOutput = getNormalizedOutput(article, followedEntityIds);
    return Math.max(0, (int) (200 * (normalizedOutput * 3 - 2)));
  }

  public double getOutput(ArticleEvaluation evaluation) {
    neuralNetwork.setInput(Doubles.toArray(evaluation.generateInputNodes().values()));
    neuralNetwork.calculate();
    return neuralNetwork.getOutput()[0];
  }

  public double getNormalizedOutput(Article article, Set<String> followedEntityIds) {
    return DistributionBuilder.projectQuantile(distribution,
        getOutput(new ArticleEvaluation(article, followedEntityIds)));
  }

  /**
   * Depending on how important we find this article and what time of day it
   * is, return it, or null to indicate that no notification should be sent.
   * FYI 200 = the highest possible notification score.  So we have a time
   * fall-off between notifications so that eventually we'll send a
   * notification, and usually it'll be an important one.
   */
  @Override
  public int getScoreNecessaryToTriggerNotification(
      long lastNotificationTime, UserTimezone userTimezone) {
    int hoursSinceNotification =
        (int) ((System.currentTimeMillis() - lastNotificationTime) / TimeUnit.HOURS.toMillis(1));
    int scoreNecessaryToTriggerNotification = 200 - (5 * hoursSinceNotification);
    if (userTimezone.isDaytime()) {
      scoreNecessaryToTriggerNotification -= 15;
    }
    // Only notify people on weekends if it's important.
    if (userTimezone.isWeekend()) {
      scoreNecessaryToTriggerNotification = Math.max(scoreNecessaryToTriggerNotification, 180);
    } else {
      scoreNecessaryToTriggerNotification = Math.max(scoreNecessaryToTriggerNotification, 50);
    }
    return scoreNecessaryToTriggerNotification;
  }

  @Override
  public Iterable<Article> getArticles(User user) throws DatabaseSchemaException, BiznessException {
    return Articles.getRankedArticles(
        user,
        NeuralNetworkScorer.getInstance(),
        new MainStreamStrategy(),
        new DiversificationPass.MainStreamPass(),
        25 /* results */,
        ImmutableSet.<String>of() /* excludeUrlIds */,
        false /* videoOnly */);
  }
}
