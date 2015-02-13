package com.janknspank.rank;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.events.LearningEvent;
import org.neuroph.core.events.LearningEventListener;
import org.neuroph.core.learning.LearningRule;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.util.TransferFunctionType;

import com.google.api.client.util.Sets;
import com.google.common.collect.Maps;
import com.janknspank.ArticleCrawler;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.UrlRatings;
import com.janknspank.bizness.Users;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.UrlRating;
import com.janknspank.proto.UserProto.User;

public class NeuralNetworkTrainer implements LearningEventListener {
  private static int MAX_ITERATIONS = 100000;
  private Double[] lowestErrorNetworkWeights;
  private double lowestError = 1.0;
  private double lowestErrorIteration = 0;

  private NeuralNetwork<BackPropagation> generateTrainedNetwork(DataSet trainingSet) {
    NeuralNetwork<BackPropagation> neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID,
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.HIDDEN_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);

    //neuralNetwork.setLearningRule(new ResilientPropagation());
    neuralNetwork.setLearningRule(new MomentumBackpropagation());

    // set learning parameters
    LearningRule learningRule = neuralNetwork.getLearningRule();
    learningRule.addListener(this);

    System.out.println("Training neural network...");
    neuralNetwork.learn(trainingSet);
    System.out.println("Trained");

    // Return the network with the lowest error
    System.out.println("Lowest error: " + lowestError + " at iteration " + lowestErrorIteration);
    neuralNetwork.setWeights(ArrayUtils.toPrimitive(lowestErrorNetworkWeights));

    // Print correlation of each input node to the output
    for (int i = 0; i < NeuralNetworkScorer.INPUT_NODES_COUNT; i++) {
      double [] isolatedInput = NeuralNetworkScorer.generateIsolatedInputNodes(i);
      neuralNetwork.setInput(isolatedInput);
      neuralNetwork.calculate();
      System.out.println("Input node " + i + " correlation to output: " 
          + neuralNetwork.getOutput()[0]);
    }

    return neuralNetwork;
  }

  /**
   * Generate DataSet to train neural network from a list of UrlRatings.
   * @return DataSet of training data
   */
  private static DataSet generateTrainingDataSet(Iterable<UrlRating> ratings) 
      throws BiznessException, DatabaseSchemaException {
    Set<String> urlStrings = Sets.newHashSet();
    Set<String> userEmails = Sets.newHashSet();
    for (UrlRating urlRating : ratings) {
      urlStrings.add(urlRating.getUrl());
      userEmails.add(urlRating.getEmail());
    }

    // Load up all users who submitted ratings
    Iterable<User> users = Users.getByEmails(userEmails);
    Map<String, User> emailUserMap = Maps.newHashMap();
    for (User user: users) {
      emailUserMap.put(user.getEmail(), user);
    }

    // Load up all articles that have ratings
    Map<String, Article> urlArticleMap = ArticleCrawler.getArticles(urlStrings);
    
    DataSet trainingSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);

    for (UrlRating rating : ratings) {
      User user = emailUserMap.get(rating.getEmail());
      Article article = urlArticleMap.get(rating.getUrl());

      if (article != null & user != null) {
        double[] input =
            NeuralNetworkScorer.generateInputNodes(user, article);
        double[] output = new double[] { rating.getRating() };
        DataSetRow row = new DataSetRow(input, output);
        trainingSet.addRow(row);
      }
    }

    return trainingSet;
  }

  /**
   * Generate a training data set from JonsBenchmark. Note this uses only the percentage
   * of the benchmark that is not in the training holdback.
   * @return DataSet of training data
   */
  private static DataSet generateJonsTrainingDataSet() 
      throws DatabaseSchemaException, BiznessException {
    User user = Users.getByEmail("panaceaa@gmail.com");
    if (user == null) {
      throw new Error("User panaceaa@gmail.com does not exist.");
    }

    HashMap<Article, Double> ratings = new HashMap<>();
    for (Article article : ArticleCrawler.getArticles(JonBenchmark.BAD_URLS).values()) {
      // If the article isn't in the holdback, use it for training.
      if (!isInTrainingHoldback(article)) {
        ratings.put(article, 0.0);
      }
    }
    for (Article article : ArticleCrawler.getArticles(JonBenchmark.GOOD_URLS).values()) {
      // If the article isn't in the holdback, use it for training.
      if (!isInTrainingHoldback(article)) {
        ratings.put(article, 1.0);
      }
    }

    // Create training set.
    DataSet trainingSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);
    for (Map.Entry<Article, Double> entry : ratings.entrySet()) {
      Article article = entry.getKey();
      double rating = entry.getValue();
      double[] input =
          NeuralNetworkScorer.generateInputNodes(user, article);
      double[] output = new double[]{rating};
      DataSetRow row = new DataSetRow(input, output);
      trainingSet.addRow(row);
    }

    return trainingSet;
  }

  @Override
  public void handleLearningEvent(LearningEvent event) {
    BackPropagation bp = (BackPropagation)event.getSource();
    double error = bp.getTotalNetworkError();
    System.out.println(bp.getCurrentIteration() + ". iteration : "+ error);
    if (error < lowestError) {
      lowestError = error;
      lowestErrorIteration = bp.getCurrentIteration();
      lowestErrorNetworkWeights = bp.getNeuralNetwork().getWeights();
    }
    if (bp.getCurrentIteration() > MAX_ITERATIONS) {
      bp.stopLearning();
    }
  }

  /**
   * Returns if an article should be excluded from use in
   * training the neural network. Articles are excluded so
   * they can be used as a benchmark to determine the quality of the scorer.
   * @return true if should be excluded from training
   */
  static boolean isInTrainingHoldback(Article article) {
    if (article.getUrl().hashCode() % 5 == 0) {
      return true;
    }
    return false;
  }

  /** 
   * Helper method for triggering a train. 
   * run ./trainneuralnet.sh to execute
   * */
  public static void main(String args[]) throws Exception {
    // Train against Jon's Benchmarks
    DataSet jonBenchmarkDataSet = generateJonsTrainingDataSet();

    // Train against User URL Ratings
    DataSet userRatingsDataSet = generateTrainingDataSet(UrlRatings.getAllRatings());
    
    // Combine the two training sets
    for (DataSetRow row : jonBenchmarkDataSet.getRows()) {
      userRatingsDataSet.addRow(row);
    }

    NeuralNetwork<BackPropagation> neuralNetwork =
        new NeuralNetworkTrainer().generateTrainedNetwork(userRatingsDataSet);
    neuralNetwork.save(NeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);
  }
}
