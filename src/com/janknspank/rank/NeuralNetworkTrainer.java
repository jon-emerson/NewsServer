package com.janknspank.rank;

import java.util.Map;
import java.util.Map.Entry;

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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.proto.UserProto.User;

public class NeuralNetworkTrainer implements LearningEventListener {
  private static int MAX_ITERATIONS = 50000;
  private Double[] lastNetworkWeights;
  private double lowestError = 1.0;

  @SuppressWarnings("unused")
  private Double[] lowestErrorNetworkWeights;
  @SuppressWarnings("unused")
  private double lowestErrorIteration = 0;

  private NeuralNetwork<BackPropagation> generateTrainedNetwork(DataSet trainingSet) {
    NeuralNetwork<BackPropagation> neuralNetwork = new MultiLayerPerceptron(
        TransferFunctionType.SIGMOID,
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.HIDDEN_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);

    neuralNetwork.setLearningRule(new MomentumBackpropagation());

    // Set learning parameters.
    LearningRule learningRule = neuralNetwork.getLearningRule();
    learningRule.addListener(this);

    System.out.println("Training neural network...");
    neuralNetwork.learn(trainingSet);
    System.out.println("Trained");

    // NOTE(jonemerson): We used to do this, but I found that about 30% of the
    // time, the neural network would come out ranking crappy articles highly.
    // I'm experimenting for now to see if taking the longest iteration has a
    // higher propensity to generate a high-quality stream.
    // // Return the network with the lowest error.
    // System.out.println("Lowest error: " + lowestError + " at iteration " + lowestErrorIteration);
    // neuralNetwork.setWeights(ArrayUtils.toPrimitive(lowestErrorNetworkWeights));
    neuralNetwork.setWeights(ArrayUtils.toPrimitive(lowestErrorNetworkWeights));

    // Print correlation of each input node to the output.
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
   * Creates a complete training data set given good URLs and bad URLs defined
   * in the user /personas/*.persona files.
   */
  private static DataSet generateTrainingDataSet()
      throws DatabaseSchemaException, BiznessException {
    DataSet trainingSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);

    for (Persona persona : Personas.getPersonaMap().values()) {
      System.out.println("Grabbing URLs for " + persona.getEmail() + " ...");
      User user = Personas.convertToUser(persona);
      Map<String, Article> urlArticleMap = ArticleCrawler.getArticles(
          Iterables.concat(persona.getGoodUrlList(), persona.getBadUrlList()), true /* retain */);

      // Hold back ~20% of the training articles so we can later gauge our
      // ranking performance against the articles the trainer didn't see.
      urlArticleMap = Maps.filterEntries(urlArticleMap,
          new Predicate<Map.Entry<String, Article>>() {
            @Override
            public boolean apply(Entry<String, Article> entry) {
              return !isInTrainingHoldback(entry.getValue());
            }
          });

      for (String goodUrl : persona.getGoodUrlList()) {
        if (urlArticleMap.containsKey(goodUrl)) {
          double[] input = Doubles.toArray(
              NeuralNetworkScorer.generateInputNodes(user, urlArticleMap.get(goodUrl)).values());
          double[] output = new double[] { 1.0 };
          trainingSet.addRow(new DataSetRow(input, output));
        }
      }

      for (String badUrl : persona.getBadUrlList()) {
        if (urlArticleMap.containsKey(badUrl)) {
          double[] input = Doubles.toArray(
              NeuralNetworkScorer.generateInputNodes(user, urlArticleMap.get(badUrl)).values());
          double[] output = new double[] { 0.0 };
          trainingSet.addRow(new DataSetRow(input, output));
        }
      }
    }

    System.out.println("Training set compiled.");
    return trainingSet;
  }

  @Override
  public void handleLearningEvent(LearningEvent event) {
    BackPropagation bp = (BackPropagation) event.getSource();
    double error = bp.getTotalNetworkError();
    if (bp.getCurrentIteration() % 1000 == 0) {
      System.out.println(bp.getCurrentIteration() + ". iteration : "+ error);
    }
    lastNetworkWeights = bp.getNeuralNetwork().getWeights();

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
    DataSet dataSet = generateTrainingDataSet();

    // Calculate average input values, for debugging/optimization purposes.
    double[] averageInputValues = new double[NeuralNetworkScorer.INPUT_NODES_COUNT];
    for (DataSetRow row : dataSet.getRows()) {
      double[] inputs = row.getInput();
      for (int i = 0; i < inputs.length; i++) {
        averageInputValues[i] += inputs[i];
      }
    }

    int numRows = dataSet.size();
    System.out.println("Average input values:");
    for (int i = 0; i < averageInputValues.length; i++) {
      averageInputValues[i] = averageInputValues[i] / numRows;
      System.out.println("  [" + i + "] = " + averageInputValues[i]);
    }

    NeuralNetwork<BackPropagation> neuralNetwork =
        new NeuralNetworkTrainer().generateTrainedNetwork(dataSet);
    neuralNetwork.save(NeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);
  }
}
