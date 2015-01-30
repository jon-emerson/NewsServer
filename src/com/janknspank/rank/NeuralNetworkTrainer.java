package com.janknspank.rank;

import java.util.HashMap;
import java.util.Map;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;

import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.UserUrlRatings;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.UserUrlRating;

public class NeuralNetworkTrainer {
  private static NeuralNetwork<BackPropagation> generateTrainedNetwork(DataSet trainingSet) {
    NeuralNetwork<BackPropagation> neuralNetwork = new MultiLayerPerceptron(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.HIDDEN_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);
    neuralNetwork.learn(trainingSet);
    return neuralNetwork;
  }

  // Train with data from server.
  private static DataSet generateTrainingDataSet()
      throws DatabaseSchemaException, ParserException, BiznessException, DatabaseRequestException {
    Iterable<UserUrlRating> allRatings = UserUrlRatings.getAll();
    Map<String, CompleteUser> userCache = new HashMap<String, CompleteUser>();

    // Create training set.
    DataSet trainingSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);

    CompleteUser user;
    for (UserUrlRating rating : allRatings) {
      String userId = rating.getUserId();
      if (userCache.containsKey(userId)) {
        user = userCache.get(userId);
      } else {
        user = new CompleteUser(rating.getUserId());
        userCache.put(userId, user);
      }
      CompleteArticle article = new CompleteArticle(rating.getUrlId());
      double[] input = NeuralNetworkScorer.generateInputNodes(user, article);
      double[] output = new double[]{(double)rating.getRating() / 100};
      DataSetRow row = new DataSetRow(input, output);
      trainingSet.addRow(row);
    }

    return trainingSet;
  }

  /** Helper method for triggering a train. */
  public static void main(String args[]) throws Exception {
    NeuralNetwork<BackPropagation> neuralNetwork =
        generateTrainedNetwork(generateTrainingDataSet());
    neuralNetwork.save(NeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);
  }
}
