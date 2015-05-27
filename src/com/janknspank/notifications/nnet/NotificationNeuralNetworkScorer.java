package com.janknspank.notifications.nnet;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.learning.BackPropagation;

import com.google.common.primitives.Doubles;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.rank.DistributionBuilder;

public final class NotificationNeuralNetworkScorer {
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

  public NotificationNeuralNetworkScorer(NeuralNetwork<BackPropagation> neuralNetwork) {
    this.neuralNetwork = neuralNetwork;
  }

  public static synchronized NotificationNeuralNetworkScorer getInstance() {
    if (instance == null) {
      instance = new NotificationNeuralNetworkScorer();
    }
    return instance;
  }

  public double getScore(Article article, Set<String> followedEntityIds) {
    return getScore(new ArticleEvaluation(article, followedEntityIds));
  }

  public double getScore(ArticleEvaluation evaluation) {
    neuralNetwork.setInput(Doubles.toArray(evaluation.generateInputNodes().values()));
    neuralNetwork.calculate();
    return neuralNetwork.getOutput()[0];
  }

  public double getNormalizedScore(Article article, Set<String> followedEntityIds) {
    return DistributionBuilder.projectQuantile(distribution,
        getScore(new ArticleEvaluation(article, followedEntityIds)));
  }
}
