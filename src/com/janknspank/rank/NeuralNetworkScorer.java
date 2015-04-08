package com.janknspank.rank;

import java.util.LinkedHashMap;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.learning.BackPropagation;

import com.google.api.client.util.Maps;
import com.google.common.primitives.Doubles;
import com.janknspank.common.Asserts;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

public final class NeuralNetworkScorer extends Scorer {
  static final int INPUT_NODES_COUNT = 10;
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

  public static LinkedHashMap<String, Double> generateInputNodes(User user, Article article) {
    Asserts.assertNotNull(user, "user", NullPointerException.class);
    Asserts.assertNotNull(article, "article", NullPointerException.class);
    LinkedHashMap<String, Double> linkedHashMap = Maps.newLinkedHashMap();

    // 0. Relevance to user's industries.
    linkedHashMap.put("industries", InputValuesGenerator.relevanceToUserIndustries(user, article));

    // 1. Relevance to social media.
    linkedHashMap.put("social_media", InputValuesGenerator.relevanceToSocialMedia(user, article));

    // 2. Relevance to contacts.
    linkedHashMap.put("contacts", InputValuesGenerator.relevanceToContacts(user, article));

    // 3. Company / organization entities being followed.
    linkedHashMap.put("companies", InputValuesGenerator.relevanceToCompanyEntities(user, article));

    // 4. Relevance to startup vector for people with that intent.
    linkedHashMap.put("startup", InputValuesGenerator.relevanceToStartupIntent(user, article));

    // 5. Relevance to acquisitions.
    linkedHashMap.put("acquisitions", InputValuesGenerator.relevanceToAcquisitions(user, article));

    // 6. Relevance to launches.
    linkedHashMap.put("launches", InputValuesGenerator.relevanceToLaunches(user, article));

    // 7. Relevance to start-up fundraising rounds.
    linkedHashMap.put("fundraising", InputValuesGenerator.relevanceToFundraising(user, article));

    // 8. Pop culture score.
    linkedHashMap.put("pop_culture", InputValuesGenerator.relevanceToPopCulture(article));

    // 9. Murder Crime War score.
    linkedHashMap.put("murder_crime_war", InputValuesGenerator.relevanceToMurderCrimeWar(article));

    return linkedHashMap;
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
    return getScore(generateInputNodes(user, article));
  }

  public static double getScore(LinkedHashMap<String, Double> inputNodes) {
    NeuralNetworkScorer scorer = getInstance();
    scorer.neuralNetwork.setInput(Doubles.toArray(inputNodes.values()));
    scorer.neuralNetwork.calculate();
    return scorer.neuralNetwork.getOutput()[0];
  }
}