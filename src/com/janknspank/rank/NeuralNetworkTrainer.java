package com.janknspank.rank;

import java.util.List;

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

import com.google.common.collect.ImmutableList;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

public class NeuralNetworkTrainer implements LearningEventListener {
  private static final int MAX_ITERATIONS = 20000;

  private double lowestError = 1.0;
  private Double[] lowestErrorNetworkWeights;
  private int lowestErrorIteration = 0;

  @SuppressWarnings("unused")
  private Double[] lastNetworkWeights;

  private NeuralNetwork<BackPropagation> generateTrainedNetwork(DataSet trainingSet) {
    NeuralNetwork<BackPropagation> neuralNetwork = new MultiLayerPerceptron(
        TransferFunctionType.SIGMOID, // TODO(jonemerson): Should this be LINEAR?
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        // NeuralNetworkScorer.HIDDEN_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);
    neuralNetwork.setLearningRule(new MomentumBackpropagation());

    // Set learning parameters.
    LearningRule learningRule = neuralNetwork.getLearningRule();
    learningRule.addListener(this);

    System.out.println("Training neural network...");
    neuralNetwork.learn(trainingSet);
    System.out.println("Trained - iteration " + lowestErrorIteration + " used w/ "
        + lowestError + " error rate");

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
      double[] isolatedInput = NeuralNetworkScorer.generateIsolatedInputNodes(i);
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
  private DataSet generateTrainingDataSet() throws DatabaseSchemaException, BiznessException {
    int goodUrlCount = 0;
    int badUrlCount = 0;
    DataSet trainingSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT, NeuralNetworkScorer.OUTPUT_NODES_COUNT);
    for (TrainingArticle trainingArticle : TrainingArticles.getTrainingArticles()) {
      trainingSet.addRow(trainingArticle.getDataSetRow());
      if (trainingArticle.getScore() > 0.5) {
        goodUrlCount++;
      } else {
        badUrlCount++;
      }
    }
    System.out.println("Training set compiled. good=" + goodUrlCount + ", bad=" + badUrlCount);
    return trainingSet;
  }

  @Override
  public void handleLearningEvent(LearningEvent event) {
    BackPropagation bp = (BackPropagation) event.getSource();
    double error = bp.getTotalNetworkError();
    if (bp.getCurrentIteration() == 1 || bp.getCurrentIteration() % 1000 == 0) {
      System.out.println(bp.getCurrentIteration() + ". iteration : "+ error);
    }
    lastNetworkWeights = bp.getNeuralNetwork().getWeights();

//    if (error < lowestError) {
      lowestError = error;
      lowestErrorIteration = bp.getCurrentIteration();
      lowestErrorNetworkWeights = bp.getNeuralNetwork().getWeights();
//    }
    if (bp.getCurrentIteration() > MAX_ITERATIONS) {
      bp.stopLearning();
    }
  }

  /**
   * Helper class for collecting the average of a progressively received set of
   * numbers.
   */
  private static class Averager {
    private int count = 0;
    private double sum = 0;

    public void add(Number number) {
      count++;
      sum += number.doubleValue();
    }

    public double get() {
      return sum / count;
    }
  }

  /**
   * Definitely not-so-efficient hack for getting a list of input name labels,
   * e.g. "industries", "facebook", "acquisitions", etc.
   */
  private static List<String> getInputNodeKeys() throws DatabaseSchemaException {
    return ImmutableList.copyOf(NeuralNetworkScorer.generateInputNodes(
        Database.with(User.class).getFirst(),
        Database.with(Article.class).getFirst()).keySet());
  }

  private static void printAverageInputValues(DataSet dataSet) throws DatabaseSchemaException {
    // Calculate average input values, for debugging/optimization purposes.
    Averager[] averageInputValues = new Averager[NeuralNetworkScorer.INPUT_NODES_COUNT];
    Averager[] averageInputValuesPositive = new Averager[NeuralNetworkScorer.INPUT_NODES_COUNT];
    Averager[] averageInputValuesNegative = new Averager[NeuralNetworkScorer.INPUT_NODES_COUNT];
    for (int i = 0; i < NeuralNetworkScorer.INPUT_NODES_COUNT; i++) {
      averageInputValues[i] = new Averager();
      averageInputValuesPositive[i] = new Averager();
      averageInputValuesNegative[i] = new Averager();
    }
    for (DataSetRow row : dataSet.getRows()) {
      double[] inputs = row.getInput();
      for (int i = 0; i < inputs.length; i++) {
        averageInputValues[i].add(inputs[i]);
        if (row.getDesiredOutput()[0] > 0.5) {
          averageInputValuesPositive[i].add(inputs[i]);
        }
        if (row.getDesiredOutput()[0] < 0.5) {
          averageInputValuesNegative[i].add(inputs[i]);
        }
      }
    }

    System.out.println("Average input values:");
    List<String> inputNodeKeys = getInputNodeKeys();
    for (int i = 0; i < averageInputValues.length; i++) {
      System.out.println("  [" + inputNodeKeys.get(i) + "] = " + averageInputValues[i].get()
          + " \t(goodUrls=" + averageInputValuesPositive[i].get() + ", \t"
          + "badUrls=" + averageInputValuesNegative[i].get() + ", \t"
          + "diff=" + Math.abs(averageInputValuesPositive[i].get() - averageInputValuesNegative[i].get()) + ")");
    }
  }

  /**
   * Helper method for triggering a train. 
   * run ./trainneuralnet.sh to execute
   * */
  public static void main(String args[]) throws Exception {
    NeuralNetworkTrainer neuralNetworkTrainer = new NeuralNetworkTrainer();
    DataSet dataSet = neuralNetworkTrainer.generateTrainingDataSet();
    printAverageInputValues(dataSet);

    NeuralNetwork<BackPropagation> neuralNetwork =
        neuralNetworkTrainer.generateTrainedNetwork(dataSet);
    // Benchmark.
    neuralNetwork.save(NeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);
  }
}
