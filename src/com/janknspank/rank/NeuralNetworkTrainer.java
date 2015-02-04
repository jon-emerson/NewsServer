package com.janknspank.rank;

import java.util.Map;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.DataSet;
import org.neuroph.core.learning.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;

import com.google.common.collect.Maps;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.UrlRating;
import com.janknspank.proto.UserProto.User;

public class NeuralNetworkTrainer {
  private static NeuralNetwork generateTrainedNetwork(DataSet trainingSet) {
    NeuralNetwork neuralNetwork = new MultiLayerPerceptron(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.HIDDEN_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);
    neuralNetwork.learn(trainingSet);
    return neuralNetwork;
  }

  // Train with data from server.
  private static DataSet generateTrainingDataSet()
      throws DatabaseSchemaException, ParserException, BiznessException, DatabaseRequestException {
    Map<UrlRating, User> urlRatingToUserMap = Maps.newHashMap();
    for (User user : Database.with(User.class).get(
        new QueryOption.WhereNotNull("url_rating"))) {
      for (UrlRating urlRating : user.getUrlRatingList()) {
        urlRatingToUserMap.put(urlRating, user);
      }
    }

    // Create training set.
    DataSet trainingSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);
    for (UrlRating rating : urlRatingToUserMap.keySet()) {
      Article article = Database.with(Article.class).get(rating.getUrlId());
      double[] input =
          NeuralNetworkScorer.generateInputNodes(urlRatingToUserMap.get(rating), article);
      double[] output = new double[]{(double)rating.getRating() / 100};
      DataSetRow row = new DataSetRow(input, output);
      trainingSet.addRow(row);
    }

    return trainingSet;
  }

  /** Helper method for triggering a train. */
  public static void main(String args[]) throws Exception {
    NeuralNetwork neuralNetwork =
        generateTrainedNetwork(generateTrainingDataSet());
    neuralNetwork.save(NeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);
  }
}
