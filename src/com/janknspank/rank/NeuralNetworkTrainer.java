package com.janknspank.rank;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.DataSet;
import org.neuroph.core.learning.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.UserUrlRatings;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.UserUrlRating;

public class NeuralNetworkTrainer {
  private static NeuralNetwork generateTrainedNetwork(DataSet trainingSet) {
    NeuralNetwork neuralNetwork = new MultiLayerPerceptron(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.HIDDEN_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);
    neuralNetwork.learn(trainingSet);
    return neuralNetwork;
  }
  
  // train with data from server
  private static DataSet generateTrainingDataSet()
      throws DataInternalException, ParserException, IOException {
    List<UserUrlRating> allRatings = UserUrlRatings.getAll();
    CompleteUser user;
    CompleteArticle article;
    Map<String, CompleteUser> userCache = new HashMap<String, CompleteUser>();
    
    // create training set
    DataSet trainingSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT, 
        NeuralNetworkScorer.OUTPUT_NODES_COUNT); 
    DataSetRow row;
    double[] input;
    double[] output;
    String userId;
    
    for (UserUrlRating rating : allRatings) {
      userId = rating.getUserId();
      if (userCache.containsKey(userId)) {
        user = userCache.get(userId);
      } else {
        user = new CompleteUser(rating.getUserId());
        userCache.put(userId, user);
      }
      article = new CompleteArticle(rating.getUrlId());
      input = NeuralNetworkScorer.generateInputNodes(user, article);
      System.out.println("data set row input.length: " + input.length);
      
      output = new double[]{(double)rating.getRating() / 100};
      row = new DataSetRow(input, output);
      trainingSet.addRow(row);
    }
    
    return trainingSet;
  }
  
  /** Helper method for triggering a train. */
  public static void main(String args[]) throws Exception {
    System.out.println(new File(".").getAbsolutePath());
    NeuralNetwork neuralNetwork = generateTrainedNetwork(generateTrainingDataSet());
    neuralNetwork.save(NeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);
  }
}
