package com.janknspank.rank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.UrlRatings;
import com.janknspank.bizness.Users;
import com.janknspank.common.Logger;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.crawler.ArticleUrlDetector;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.UrlRating;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserAction;

public class NeuralNetworkTrainer implements LearningEventListener {
  private final static Logger LOG = new Logger(NeuralNetworkTrainer.class);
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
   * Generate DataSet to train the neural network from a list of UserActions.
   */
  private static DataSet generateUserActionsTrainingDataSet(Iterable<UserAction> actions) 
      throws DatabaseSchemaException, BiznessException {
    Set<String> urlStrings = Sets.newHashSet();
    Set<String> userIds = Sets.newHashSet();
    List<UserAction> scoreableActions = new ArrayList<>(); 
    Map<String, Double> actionIdScoreMap = Maps.newHashMap();
    for (UserAction userAction : actions) {
      // Double check that we still support the site that was rated.
      // (Sometimes we take down sites that aren't relevant to most people.)
      if (ArticleUrlDetector.isArticle(userAction.getUrl())) {
        double score = getScoreForAction(userAction);
        if (score >= 0) {
          scoreableActions.add(userAction);
          actionIdScoreMap.put(userAction.getId(), score);
          urlStrings.add(userAction.getUrl());
          userIds.add(userAction.getUserId());
        }
      }
    }

    System.out.println("UserActions in the training set: " + scoreableActions.size());

    // Load up fake User objects from the cached properties
    Map<String, User> idUserMap = Maps.newHashMap();
    for (UserAction action : scoreableActions) {
      User tempUser = User.newBuilder()
          .addAllAddressBookContact(action.getAddressBookContactList())
          .addAllLinkedInContact(action.getLinkedInContactList())
          .addAllInterest(action.getInterestList())
          .setId(action.getUserId())
          .setEmail("some.email@gmail.com")
          .setCreateTime(System.currentTimeMillis())
          .setLinkedInAccessToken("_").build();
      idUserMap.put(tempUser.getId(), tempUser);
    }

    // Load up all articles that have ratings
    Map<String, Article> urlArticleMap = ArticleCrawler.getArticles(urlStrings, true /* retain */);

    DataSet trainingSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);

    for (UserAction action : scoreableActions) {
      User user = idUserMap.get(action.getUserId());
      Article article = urlArticleMap.get(action.getUrl());

      // A user rating may outlive any particular Article or User
      // in our system. So check to see that they exist before scoring
      if (article == null) {
        LOG.warning("Can't find Article to score: " + action.getUrl());
      } else if (user == null) {
        LOG.warning("Can't find User to score: " + action.getUserId());
      } else {
        double[] input =
            NeuralNetworkScorer.generateInputNodes(user, article);
        double[] output = new double[] { actionIdScoreMap.get(action.getId()) };
        DataSetRow row = new DataSetRow(input, output);
        trainingSet.addRow(row);
      }
    }

    return trainingSet;
  }
  
  /**
   * Returns a rating for a given user action. If unable to convert an action to a rating
   * the method will return -1;
   */
  private static double getScoreForAction(UserAction action) {
    if (action.getActionType() == UserAction.ActionType.FAVORITE) {
      return 1.0;
    } else if (action.getActionType() == UserAction.ActionType.READ_ARTICLE) {
      return -1; // Not sure how to rate
    } else if (action.getActionType() == UserAction.ActionType.SHARE) {
      return 1.0;
    } else if (action.getActionType() == UserAction.ActionType.TAP_FROM_STREAM) {
      return 0.7;
    } else if (action.getActionType() == UserAction.ActionType.X_OUT) {
      return 0.0;
    } else {
      return -1;
    }
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
      // Double check that we still support the site that was rated.
      // (Sometimes we take down sites that aren't relevant to most people.)
      if (ArticleUrlDetector.isArticle(urlRating.getUrl())) {
        urlStrings.add(urlRating.getUrl());
        userEmails.add(urlRating.getEmail());
      }
    }

    // Load up all users who submitted ratings
    Iterable<User> users = Users.getByEmails(userEmails);
    Map<String, User> emailUserMap = Maps.newHashMap();
    for (User user : users) {
      emailUserMap.put(user.getEmail(), user);
    }

    // Load up all articles that have ratings
    Map<String, Article> urlArticleMap = ArticleCrawler.getArticles(urlStrings, true /* retain */);

    DataSet trainingSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);

    for (UrlRating rating : ratings) {
      User user = emailUserMap.get(rating.getEmail());
      Article article = urlArticleMap.get(rating.getUrl());

      // A user rating may outlive any particular Article or User
      // in our system. So check to see that they exist before scoring
      if (article == null) {
        LOG.warning("Can't find Article to score: " + rating.getUrl());
      } else if (user == null) {
        LOG.warning("Can't find User to score: " + rating.getEmail());
      } else {
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
    for (Article article :
        ArticleCrawler.getArticles(JonBenchmark.BAD_URLS, true /* retain */).values()) {
      // If the article isn't in the holdback, use it for training.
      if (!isInTrainingHoldback(article)) {
        ratings.put(article, 0.0);
      }
    }
    for (Article article :
        ArticleCrawler.getArticles(JonBenchmark.GOOD_URLS, true /* retain */).values()) {
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
    if (bp.getCurrentIteration() % 1000 == 0) {
      System.out.println(bp.getCurrentIteration() + ". iteration : "+ error);
    }

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

    // Train against UserActions like x-out article
    DataSet userActionsDataSet = generateUserActionsTrainingDataSet(
        Database.with(UserAction.class).get());

    DataSet completeDataSet = new DataSet(
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.OUTPUT_NODES_COUNT);

    // Combine Jon's benchmark into completeDataSet
    double[] averageInputValues = new double[NeuralNetworkScorer.INPUT_NODES_COUNT];
    for (DataSetRow row : jonBenchmarkDataSet.getRows()) {
      double[] inputs = row.getInput();
      for (int i = 0; i < inputs.length; i++) {
        averageInputValues[i] += inputs[i];
      }
      completeDataSet.addRow(row);
    }

    // Combine User Ratings into completeDataSet
    for (DataSetRow row : userRatingsDataSet.getRows()) {
      double[] inputs = row.getInput();
      for (int i = 0; i < inputs.length; i++) {
        averageInputValues[i] += inputs[i];
      }
      completeDataSet.addRow(row);
    }
    
    // Combine UserActions ratings into completeDataSet
    for (DataSetRow row : userActionsDataSet.getRows()) {
      double[] inputs = row.getInput();
      for (int i = 0; i < inputs.length; i++) {
        averageInputValues[i] += inputs[i];
      }
      completeDataSet.addRow(row);
    }

    int numRows = completeDataSet.size();
    //LOG.info("Average input values:");
    System.out.println("Average input values:");
    for (int i = 0; i < averageInputValues.length; i++) {
      averageInputValues[i] = averageInputValues[i] / numRows;
      System.out.println("  [" + i + "] = " + averageInputValues[i]);
    }

    NeuralNetwork<BackPropagation> neuralNetwork =
        new NeuralNetworkTrainer().generateTrainedNetwork(completeDataSet);
    neuralNetwork.save(NeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);
  }
}
