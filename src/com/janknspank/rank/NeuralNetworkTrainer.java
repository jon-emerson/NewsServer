package com.janknspank.rank;

import java.util.Hashtable;
import java.util.Map;

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

import com.janknspank.ArticleCrawler;
import com.janknspank.bizness.Users;
import com.janknspank.proto.ArticleProto.Article;
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

  private static DataSet generateTrainingDataSet(User user, Hashtable<Article, Double> ratings) {
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

  /** Helper method for triggering a train. 
   * run ./trainneuralnet.sh to execute
   * */
  public static void main(String args[]) throws Exception {
    // Train against Jon's Benchmarks
    User user = Users.getByEmail("panaceaa@gmail.com");
    if (user == null) {
      throw new Error("User panaceaa@gmail.com does not exist.");
    }

    Hashtable<Article, Double> ratings = new Hashtable<>();
    for (Article article : ArticleCrawler.getArticles(JonBenchmark.BAD_URLS).values()) {
      // If the article isn't in the holdback, use it for training.
      if (!JonBenchmark.isInTrainingHoldback(article)) {
        ratings.put(article, 0.0);
      }
    }
    for (Article article : ArticleCrawler.getArticles(JonBenchmark.GOOD_URLS).values()) {
      // If the article isn't in the holdback, use it for training.
      if (!JonBenchmark.isInTrainingHoldback(article)) {
        ratings.put(article, 1.0);
      }
    }

    NeuralNetwork<BackPropagation> neuralNetwork =
        new NeuralNetworkTrainer().generateTrainedNetwork(generateTrainingDataSet(user, ratings));
    neuralNetwork.save(NeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);
  }
}
