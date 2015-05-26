package com.janknspank.notifications.nnet;

import java.util.Set;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.learning.BackPropagation;

import com.google.common.primitives.Doubles;
import com.janknspank.proto.ArticleProto.Article;

public final class NotificationNeuralNetworkScorer {
  static final String DEFAULT_NEURAL_NETWORK_FILE =
      "neuralnet/notifications_backpropagation_out.nnet";
  private static NotificationNeuralNetworkScorer instance = null;
  private NeuralNetwork<BackPropagation> neuralNetwork;

  @SuppressWarnings("unchecked")
  private NotificationNeuralNetworkScorer() {
    neuralNetwork = NeuralNetwork.createFromFile(DEFAULT_NEURAL_NETWORK_FILE);
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
    neuralNetwork.setInput(Doubles.toArray(
        new ArticleEvaluation(article, followedEntityIds).generateInputNodes().values()));
    neuralNetwork.calculate();
    return neuralNetwork.getOutput()[0];
  }
}
