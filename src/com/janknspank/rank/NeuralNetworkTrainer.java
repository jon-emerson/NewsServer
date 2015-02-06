package com.janknspank.rank;

import java.util.Hashtable;
import java.util.Map;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.events.LearningEvent;
import org.neuroph.core.events.LearningEventListener;
import org.neuroph.core.learning.DataSet;
import org.neuroph.core.learning.DataSetRow;
import org.neuroph.core.learning.LearningRule;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.ResilientPropagation;
import org.neuroph.util.TransferFunctionType;

import com.janknspank.ArticleCrawler;
import com.janknspank.database.Database;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

public class NeuralNetworkTrainer implements LearningEventListener {
  private NeuralNetwork generateTrainedNetwork(DataSet trainingSet) {
    NeuralNetwork neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID,
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.HIDDEN_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);

    neuralNetwork.setLearningRule(new ResilientPropagation()); 

    // set learning parameters
    LearningRule learningRule = neuralNetwork.getLearningRule();
    learningRule.addListener(this);

    System.out.println("Training neural network...");
    neuralNetwork.learn(trainingSet);
    System.out.println("Trained");
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

  /** Helper method for triggering a train. 
   * run ./trainneuralnet.sh to execute
   * */
  public static void main(String args[]) throws Exception {
    // Train against Jon's Benchmarks
    User user = Database.with(User.class).getFirst(
        new QueryOption.WhereEquals("email", "panaceaa@gmail.com"));
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

    NeuralNetwork neuralNetwork =
        new NeuralNetworkTrainer().generateTrainedNetwork(generateTrainingDataSet(user, ratings));
    neuralNetwork.save(NeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);
  }

  @Override
  public void handleLearningEvent(LearningEvent event) {
    BackPropagation bp = (BackPropagation)event.getSource();
    System.out.println(bp.getCurrentIteration() + ". iteration : "+ bp.getTotalNetworkError());
    if (bp.getCurrentIteration() > 1000000) {
      bp.stopLearning();
    }
  }
}
