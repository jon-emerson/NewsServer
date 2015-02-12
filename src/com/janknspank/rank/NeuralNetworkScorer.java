package com.janknspank.rank;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.learning.BackPropagation;

import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

public final class NeuralNetworkScorer extends Scorer {
  static final int INPUT_NODES_COUNT = 9;
  static final int OUTPUT_NODES_COUNT = 1;
  static final int HIDDEN_NODES_COUNT = INPUT_NODES_COUNT + OUTPUT_NODES_COUNT + 1;
  static final String DEFAULT_NEURAL_NETWORK_FILE = "neuralnet/default_mlp_" +
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
    return new double[] {
        // 1. Relevance to user's industries
        InputValuesGenerator.relevanceToUserIndustries(user, article),

        // 2. Relevance to social media
        InputValuesGenerator.relevanceToSocialMedia(user, article),

        // 3. Relevance to contacts
        InputValuesGenerator.relevanceToContacts(user, article),

        // 4. Relevance to current employer
        InputValuesGenerator.relevanceToCurrentEmployer(user, article),

        // 5. Relevance to companies the user wants to work at
        InputValuesGenerator.relevanceToCompaniesTheUserWantsToWorkAt(user, article),

        // 6. Relevance to current role
        InputValuesGenerator.relevanceToCurrentRole(user, article),

        // 7. Timeliness
        //InputValuesGenerator.timeliness(article),

        // 7. Past employers
        InputValuesGenerator.relevanceToPastEmployers(user, article),

        // 8. Article text quality
        InputValuesGenerator.articleTextQualityScore(article),
        
        // 9. Relevance to startup vector for people with that intent
        InputValuesGenerator.relevanceToStartupIntent(user, article)
    };
  }
  
  /**
   * generate an nodes array with all indexes = 0 except for the specified
   * index 
   * @param i
   * @return
   */
  static double[] generateIsolatedInputNodes(int enabledIndex) {
    double[] inputs = new double[INPUT_NODES_COUNT];
    for (int i = 0; i < INPUT_NODES_COUNT; i++) {
      inputs[i] = (i == enabledIndex) ? 1.0 : 0.0;
    }
    return inputs;
  }

  // V1 has a general rank - one neural network for all intents. No mixing.
  // Slow architecture. Makes too many server calls
  @Override
  public double getScore(User user, Article article) {
    neuralNetwork.setInput(generateInputNodes(user, article));
    neuralNetwork.calculate();
    return neuralNetwork.getOutput()[0];
  }
}