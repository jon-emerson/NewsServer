package com.janknspank.rank;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.learning.BackPropagation;

import com.janknspank.common.Asserts;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

public final class NeuralNetworkScorer extends Scorer {
  static final int INPUT_NODES_COUNT = 9;
  static final int OUTPUT_NODES_COUNT = 1;
  static final int HIDDEN_NODES_COUNT = 9;
  static final String DEFAULT_NEURAL_NETWORK_FILE = "neuralnet/backpropagation_" +
      INPUT_NODES_COUNT + "in-" + HIDDEN_NODES_COUNT + "hidden-" +
      OUTPUT_NODES_COUNT + "out.nnet";
  private static NeuralNetworkScorer instance = null;
  private NeuralNetwork<BackPropagation> neuralNetwork;

  private NeuralNetworkScorer() {
    setFile(DEFAULT_NEURAL_NETWORK_FILE);
  }

  public static synchronized NeuralNetworkScorer getInstance() {
    if (instance == null) {
      instance = new NeuralNetworkScorer();
    }
    return instance;
  }

  @SuppressWarnings("unchecked")
  public void setFile(String nnetFile) {
    neuralNetwork = NeuralNetwork.createFromFile(nnetFile);
  }

  static double[] generateInputNodes(User user, Article article) {
    Asserts.assertNotNull(user, "user", NullPointerException.class);
    Asserts.assertNotNull(article, "article", NullPointerException.class);
    return new double[] {
      // 0. Relevance to user's industries
      InputValuesGenerator.relevanceToUserIndustries(user, article),

      // 1. Relevance to social media
      InputValuesGenerator.relevanceToSocialMedia(user, article),

      // 2. Relevance to contacts
      InputValuesGenerator.relevanceToContacts(user, article),

      // 3. Company / organization entities being followed.
      InputValuesGenerator.relevanceToCompanyEntities(user, article),

      // 4. Relevance to startup vector for people with that intent.
      InputValuesGenerator.relevanceToStartupIntent(user, article),

      // 5. Relevance to acquisitions.
      InputValuesGenerator.relevanceToAcquisitions(user, article),

      // 6. Relevance to launches.
      InputValuesGenerator.relevanceToLaunches(user, article),

      // 7. Relevance to start-up fundraising rounds.
      InputValuesGenerator.relevanceToFundraising(user, article),

      // 8. Pop culture score.
      InputValuesGenerator.relevanceToPopCulture(article)
    };
  }

  /**
   * Generate an nodes array with all indexes = 0 except for the specified
   * index.
   */
  static double[] generateIsolatedInputNodes(int enabledIndex) {
    double[] inputs = new double[INPUT_NODES_COUNT];
    for (int i = 0; i < INPUT_NODES_COUNT; i++) {
      inputs[i] = (i == enabledIndex) ? 1.0 : 0.0;
    }
    return inputs;
  }

  @Override
  public double getScore(User user, Article article) {
    Asserts.assertNotNull(user, "user", NullPointerException.class);
    Asserts.assertNotNull(article, "article", NullPointerException.class);
    neuralNetwork.setInput(generateInputNodes(user, article));
    neuralNetwork.calculate();
    return neuralNetwork.getOutput()[0];
  }
}