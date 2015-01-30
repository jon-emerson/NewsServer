package com.janknspank.rank;

import org.neuroph.core.NeuralNetwork;

import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.UserProto.User;

public final class NeuralNetworkScorer extends Scorer {
  static final int INPUT_NODES_COUNT = 9;
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
    SocialEngagement engagement = getLatestFacebookEngagement(article);
    if (engagement == null) {
      engagement = SocialEngagement.getDefaultInstance();
    }

    return new double[] {
      // Input 1: equals 1 if article is about the user's current place of work
      InputValuesGenerator.isAboutCurrentEmployer(user, article) ? 1 : 0,

      // Input 2: # of topics matches between user and article
      sigmoid(InputValuesGenerator.matchedInterestsCount(user, article)),

      // Input 3: the length of the article
      // TODO: improve normalization
      // Use average wordcount and max word count
      sigmoid(Math.log(article.getWordCount())),

      // Input 4: Facebook likes
      sigmoid(engagement.getLikeCount()),

      // Input 5: Facebook comments
      sigmoid(engagement.getCommentCount()),

      // Input 6: Facebook shares
      sigmoid(engagement.getShareCount()),

      // Input 7: Facebook likes velocity
      sigmoid(getLikeVelocity(article)),

      // Input 8: Article age
      sigmoid(System.currentTimeMillis() - article.getPublishedTime()),

      // Input 9: User has intent to stay on top of industry
      Math.min(1, InputValuesGenerator.industryRelevance(user, article))

      // Input 10: Article is educational and about a skill
      // the user wants to develop

      // TODO: inputs with article classifications like "data-rich"

      // More inputs...
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

  private static double sigmoid(double x) {
    return 1 / (1 + Math.exp(-x));
  }
}