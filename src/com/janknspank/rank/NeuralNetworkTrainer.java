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

import com.google.common.collect.Maps;
import com.janknspank.ArticleCrawler;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.UrlRating;
import com.janknspank.proto.UserProto.User;

public class NeuralNetworkTrainer implements LearningEventListener {
  private NeuralNetwork generateTrainedNetwork(DataSet trainingSet) {
    NeuralNetwork neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID,
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.HIDDEN_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);
    //neuralNetwork.setLearningRule(new MomentumBackpropagation());

    neuralNetwork.setLearningRule(new ResilientPropagation()); 
    
    // enable batch if using MomentumBackpropagation
//    if (neuralNetwork.getLearningRule() instanceof MomentumBackpropagation) {
//      ((MomentumBackpropagation)neuralNetwork.getLearningRule()).setBatchMode(true);
//    }

 // set learning parametars
    LearningRule learningRule = neuralNetwork.getLearningRule();
    learningRule.addListener(this);

//    MomentumBackpropagation learningRule = (MomentumBackpropagation) neuralNetwork.getLearningRule();
//    learningRule.setLearningRate(0.7);
//    learningRule.setMomentum(0.4);
//    learningRule.addListener(this);

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
  
  // Train with explicit user ratings from server.
  private static DataSet generateTrainingDataSetFromURLRatings()
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

  /** Helper method for triggering a train. 
   * run ./trainneuralnet.sh to execute
   * */
  public static void main(String args[]) throws Exception {
    // v1: Train against explicit URL ratings
//    NeuralNetwork neuralNetwork =
//        generateTrainedNetwork(generateTrainingDataSetFromURLRatings());

    // v2: Train against Jon's Benchmarks
    User user = Database.with(User.class).getFirst(
        new QueryOption.WhereEquals("email", "panaceaa@gmail.com"));
    Hashtable<Article, Double> ratings = new Hashtable<>();
    for (Article article : ArticleCrawler.getArticles(JonBenchmark.BAD_URLS).values()) {
      ratings.put(article, 0.0 + (Math.random() / 5));
    }
    for (Article article : ArticleCrawler.getArticles(JonBenchmark.GOOD_URLS).values()) {
      ratings.put(article, 1.0 - (Math.random() / 5));
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
