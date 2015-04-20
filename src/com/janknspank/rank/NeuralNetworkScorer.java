package com.janknspank.rank;

import java.util.LinkedHashMap;
import java.util.Map;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.learning.BackPropagation;

import com.google.api.client.util.Maps;
import com.google.common.primitives.Doubles;
import com.janknspank.bizness.Urls;
import com.janknspank.bizness.Users;
import com.janknspank.common.Asserts;
import com.janknspank.crawler.Interpreter;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.InterpretedData;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.UserProto.User;

public final class NeuralNetworkScorer extends Scorer {
  static final int INPUT_NODES_COUNT = 12;
  static final int OUTPUT_NODES_COUNT = 1;
 //  static final int HIDDEN_NODES_COUNT = 9;
  static final String DEFAULT_NEURAL_NETWORK_FILE = "neuralnet/backpropagation_out.nnet";
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

    // 0. Relevant to user's industries?
    double relevance = InputValuesGenerator.relevanceToUserIndustries(user, article);
    linkedHashMap.put("industries", (relevance > 0.5) ? ((relevance > 0.9) ? 1 : 0.5) : 0.0);

    // 1. Nearby industry count.  Value relative to the number of industries
    // this article is about that the user is not explicitly interested in.
    linkedHashMap.put("industry-specific",
        InputValuesGenerator.relevanceToNonUserIndustries(user, article));

    // 2. Relevance on Facebook.
    linkedHashMap.put("facebook", InputValuesGenerator.relevanceOnFacebook(user, article));

    // 3. Relevance on Twitter.
    linkedHashMap.put("twitter", InputValuesGenerator.relevanceOnTwitter(user, article));

    // 4. Relevance to contacts.
    linkedHashMap.put("contacts", InputValuesGenerator.relevanceToContacts(user, article));

    // 5. Company / organization entities being followed.
    linkedHashMap.put("companies", InputValuesGenerator.relevanceToCompanyEntities(user, article));

    // 6. Relevance to startup vector.
    linkedHashMap.put("startup", InputValuesGenerator.relevanceToStartups(user, article));

    // 7. Relevance to acquisitions.
    linkedHashMap.put("acquisitions", 0.0); // InputValuesGenerator.relevanceToAcquisitions(user, article));

    // 8. Relevance to launches.
    linkedHashMap.put("launches", 0.0); // , InputValuesGenerator.relevanceToLaunches(user, article));

    // 9. Relevance to start-up fundraising rounds.
    linkedHashMap.put("fundraising", 0.0); // , InputValuesGenerator.relevanceToFundraising(user, article));

    // 10. Pop culture score.
    linkedHashMap.put("pop_culture", InputValuesGenerator.relevanceToPopCulture(article));

    // 11. Murder Crime War score.
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

  /**
   * Usage:
   * bin/score.sh jon@jonemerson.net http://path/to/article
   */
  public static void main(String args[]) throws Exception {
    User user = Users.getByEmail(args[0]);
    if (user == null) {
      throw new RuntimeException("User not found: " + args[0]);
    }

    String urlString = args[1];
    Url url = Urls.getByUrl(urlString);
    if (url == null) {
      url = Urls.put(urlString, "");
    }
    InterpretedData data = Interpreter.interpret(url);
    System.out.println(data.getArticle());

    LinkedHashMap<String, Double> inputNodes = generateInputNodes(user, data.getArticle());
    for (Map.Entry<String, Double> entry : inputNodes.entrySet()) {
      System.out.println("Node " + entry.getKey() + ": " + entry.getValue());
    }
    System.out.println("Score: " + getScore(inputNodes));
  }
}