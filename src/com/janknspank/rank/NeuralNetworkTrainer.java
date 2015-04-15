package com.janknspank.rank;

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

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserAction;
import com.janknspank.proto.UserProto.UserAction.ActionType;

public class NeuralNetworkTrainer implements LearningEventListener {
  private static final int MAX_ITERATIONS = 40000;
  private static final boolean IN_DELETE_MODE = false;

  private double lowestError = 1.0;
  private Double[] lowestErrorNetworkWeights;
  private int lowestErrorIteration = 0;

  @SuppressWarnings("unused")
  private Double[] lastNetworkWeights;

  private NeuralNetwork<BackPropagation> generateTrainedNetwork(DataSet trainingSet) {
    NeuralNetwork<BackPropagation> neuralNetwork = new MultiLayerPerceptron(
        TransferFunctionType.SIGMOID, // TODO(jonemerson): Should this be LINEAR?
        NeuralNetworkScorer.INPUT_NODES_COUNT,
        NeuralNetworkScorer.HIDDEN_NODES_COUNT,
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
   * Returns the users we trust to give us good training data via UserActions.
   */
  private static Map<String, User> getUserActionTrustedUsers() throws DatabaseSchemaException {
    Map<String, User> users = Maps.newHashMap();
    for (User user : Database.with(User.class).get(
        new QueryOption.WhereEquals("email", ImmutableList.of(
            "dvoytenko@yahoo.com",
            "jon@jonemerson.net",
            "panaceaa@gmail.com",
            "virendesai87@gmail.com")))) {
      users.put(user.getId(), user);
    }
    return users;
  }

  /**
   * Returns a list of DataSetRows derived from VOTE_UP user actions from users
   * we trust.
   */
  private static List<DataSetRow> getUserActionVoteUpDataSetRows()
      throws DatabaseSchemaException, BiznessException {
    Map<String, User> users = getUserActionTrustedUsers();

    // These are URLs the users happened to "unvote".  For convenience, we
    // just blacklist any vote up actions against these URLs.
    Set<String> urlIdsToIgnore = Sets.newHashSet();
    for (UserAction userAction : Database.with(UserAction.class).get(
        new QueryOption.WhereEquals("user_id", users.keySet()),
        new QueryOption.WhereEqualsEnum("action_type", ActionType.UNVOTE_UP))) {
      urlIdsToIgnore.add(userAction.getUrlId());
    }

    // Figure out data set rows for each of the unblacklisted vote up actions.
    List<DataSetRow> dataSetRows = Lists.newArrayList();
    for (User user : users.values()) {
      Iterable<UserAction> userActions = Database.with(UserAction.class).get(
          new QueryOption.WhereEquals("user_id", user.getId()),
          new QueryOption.WhereEqualsEnum("action_type", ActionType.VOTE_UP));
      System.out.println("For " + user.getEmail() + ", " + Iterables.size(userActions)
          + " VOTE_UP user actions found");
      Set<String> urlsToCrawl = Sets.newHashSet();

      for (UserAction userAction : userActions) {
        if (!urlIdsToIgnore.contains(userAction.getUrlId())) {
          urlsToCrawl.add(userAction.getUrl());
        }
      }
      if (IN_DELETE_MODE) {
        Database.with(Article.class).delete(new QueryOption.WhereEquals("url", urlsToCrawl));
        continue;
      }
      Map<String, Article> articleMap =
          ArticleCrawler.getArticles(urlsToCrawl, true /* retain */);
      for (UserAction userAction : userActions) {
        if (userAction.hasOnStreamForInterest()) {
          // Ignore these for now.  They're from substreams, e.g. the user is
          // viewing a specific entity or topic, not their main stream.
          continue;
        }
        if (articleMap.containsKey(userAction.getUrl())) {
          User modifiedUser = user.toBuilder()
              .clearInterest()
              .addAllInterest(userAction.getInterestList())
              .build();
          double[] input = Doubles.toArray(
              NeuralNetworkScorer.generateInputNodes(
                  modifiedUser, articleMap.get(userAction.getUrl())).values());
          double[] output = new double[] { 1.0 };
          dataSetRows.add(new DataSetRow(input, output));
        }
      }
    }
    return dataSetRows;
  }

  /**
   * Returns a list of DataSetRows derived from X_OUT user actions from
   * users we trust.
   */
  private static List<DataSetRow> getUserActionXOutDataSetRows()
      throws DatabaseSchemaException, BiznessException {
    Map<String, User> users = getUserActionTrustedUsers();

    // Figure out data set rows for each of the unblacklisted vote up actions.
    List<DataSetRow> dataSetRows = Lists.newArrayList();
    for (User user : users.values()) {
      Iterable<UserAction> userActions = Database.with(UserAction.class).get(
          new QueryOption.WhereEquals("user_id", user.getId()),
          new QueryOption.WhereEqualsEnum("action_type", ActionType.X_OUT));
      System.out.println("For " + user.getEmail() + ", " + Iterables.size(userActions)
          + " X_OUT user actions found");
      Set<String> urlsToCrawl = Sets.newHashSet();
      for (UserAction userAction : userActions) {
        urlsToCrawl.add(userAction.getUrl());
      }
      if (IN_DELETE_MODE) {
        Database.with(Article.class).delete(new QueryOption.WhereEquals("url", urlsToCrawl));
        continue;
      }
      Map<String, Article> articleMap =
          ArticleCrawler.getArticles(urlsToCrawl, true /* retain */);
      for (UserAction userAction : userActions) {
        if (articleMap.containsKey(userAction.getUrl())) {
          User modifiedUser = user.toBuilder()
              .clearInterest()
              .addAllInterest(userAction.getInterestList())
              .build();
          double[] input = Doubles.toArray(
              NeuralNetworkScorer.generateInputNodes(
                  modifiedUser, articleMap.get(userAction.getUrl())).values());
          double[] output = new double[] { 0.0 };
          dataSetRows.add(new DataSetRow(input, output));
        }
      }
    }
    return dataSetRows;
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
      System.out.println("Grabbing articles for " + persona.getEmail() + " ...");
      User user = Personas.convertToUser(persona);

      if (IN_DELETE_MODE) {
        Database.with(Article.class).delete(new QueryOption.WhereEquals("url",
            Iterables.concat(persona.getGoodUrlList(), persona.getBadUrlList())));
        continue;
      }
      Map<String, Article> urlArticleMap = ArticleCrawler.getArticles(
          Iterables.concat(persona.getGoodUrlList(), persona.getBadUrlList()), true /* retain */);

      // Hold back ~20% of the training articles so we can later gauge our
      // ranking performance against the articles the trainer didn't see.
//      urlArticleMap = Maps.filterEntries(urlArticleMap,
//          new Predicate<Map.Entry<String, Article>>() {
//            @Override
//            public boolean apply(Entry<String, Article> entry) {
//              return !isInTrainingHoldback(entry.getValue());
//            }
//          });

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

    for (DataSetRow dataSetRow : getUserActionVoteUpDataSetRows()) {
      trainingSet.addRow(dataSetRow);
    }

    for (DataSetRow dataSetRow : getUserActionXOutDataSetRows()) {
      trainingSet.addRow(dataSetRow);
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

    if (error < lowestError && bp.getCurrentIteration() > 5000) {
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
