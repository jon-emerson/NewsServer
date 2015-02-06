package com.janknspank.rank;

import org.neuroph.core.NeuralNetwork;

import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

public final class NeuralNetworkScorer extends Scorer {
  static final int INPUT_NODES_COUNT = 10;
  static final int OUTPUT_NODES_COUNT = 1;
  static final int HIDDEN_NODES_COUNT = INPUT_NODES_COUNT + OUTPUT_NODES_COUNT + 1;
  static final String DEFAULT_NEURAL_NETWORK_FILE = "neuralnet/default_mlp_" +
      INPUT_NODES_COUNT + "in-" + HIDDEN_NODES_COUNT + "hidden-" +
      OUTPUT_NODES_COUNT + "out.nnet";
  private static NeuralNetworkScorer instance = null;
  private NeuralNetwork neuralNetwork;

  private NeuralNetworkScorer() {
    setFile(DEFAULT_NEURAL_NETWORK_FILE);
  }

  public static synchronized NeuralNetworkScorer getInstance() {
    if (instance == null) {
      instance = new NeuralNetworkScorer();
    }
    return instance;
  }

  public void setFile(String nnetFile) {
    neuralNetwork = NeuralNetwork.load(nnetFile);
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

        // 6. Relevance to skills
        InputValuesGenerator.relevanceToSkills(user, article),

        // 7. Relevance to current role
        InputValuesGenerator.relevanceToCurrentRole(user, article),

        // 8. Timeliness
        InputValuesGenerator.timeliness(article),

        // 9. Past employers
        InputValuesGenerator.relevanceToPastEmployers(user, article),

        // 10. Article text quality
        InputValuesGenerator.articleTextQualityScore(article)
    };
  }

  // V1 has a general rank - one neural network for all intents. No mixing.
  // Slow architecture. Makes too many server calls
  @Override
  public double getScore(User user, Article article) {
    long startMillis = System.currentTimeMillis();
    neuralNetwork.setInput(generateInputNodes(user, article));
    long generateInputNodesMillis = System.currentTimeMillis();
    neuralNetwork.calculate();
    long calculateMillis = System.currentTimeMillis();

    double totalTimeToRankArticle = (double)(calculateMillis - startMillis) / 1000;
    double timeToGenerateInputNodes = (double)(generateInputNodesMillis
        - totalTimeToRankArticle) / 1000;
    double timeToCalculate = (double)(calculateMillis - generateInputNodesMillis) / 1000;

    System.out.println("Ranked " + article.getUrl());
    System.out.println("  Score: " + neuralNetwork.getOutput()[0]);
    System.out.println("  Total time: " + totalTimeToRankArticle + "s");
    System.out.println("    generate input nodes: " + timeToGenerateInputNodes +
        ", compute output: " + timeToCalculate);
    return neuralNetwork.getOutput()[0];
  }
}