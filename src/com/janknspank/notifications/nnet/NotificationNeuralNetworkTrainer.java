package com.janknspank.notifications.nnet;

import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

import com.google.api.client.util.Maps;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.janknspank.bizness.BiznessException;
import com.janknspank.common.Averager;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.NotificationsProto.DeviceType;
import com.janknspank.proto.NotificationsProto.Notification;
import com.janknspank.rank.DistributionBuilder;

public class NotificationNeuralNetworkTrainer implements LearningEventListener {
  private static final int MAX_ITERATIONS = 20000;

  // Helper object that gives us human readable names for each input node.
  private static List<String> INPUT_NODE_KEYS = null;

  private double lowestError = 1.0;
  private Double[] lowestErrorNetworkWeights;
  private int lowestErrorIteration = 0;

  @SuppressWarnings("unused")
  private Double[] lastNetworkWeights;

  private NeuralNetwork<BackPropagation> generateTrainedNetwork(
      DataSet trainingSet, int hiddenNodeCount) throws DatabaseSchemaException {
    NeuralNetwork<BackPropagation> neuralNetwork;
    List<String> inputNodeKeys = getInputNodeKeys();
    int inputNodesCount = inputNodeKeys.size();
    if (hiddenNodeCount == 0) {
      neuralNetwork = new MultiLayerPerceptron(
          TransferFunctionType.SIGMOID, // TODO(jonemerson): Should this be LINEAR?
          inputNodesCount,
          1 /* output nodes count */);
    } else if (hiddenNodeCount == 0) {
      neuralNetwork = new MultiLayerPerceptron(
          TransferFunctionType.LINEAR, // Trying this out vs. SIGMOID...
          inputNodesCount,
          1 /* output nodes count */);
    } else {
      neuralNetwork = new MultiLayerPerceptron(
          TransferFunctionType.SIGMOID, // TODO(jonemerson): Should this be LINEAR?
          inputNodesCount,
          hiddenNodeCount,
          1 /* output nodes count */);
    }
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
    for (int i = 0; i < inputNodesCount; i++) {
      double[] isolatedInput = generateIsolatedInputNodes(i);
      neuralNetwork.setInput(isolatedInput);
      neuralNetwork.calculate();
      System.out.println("Input node '" + inputNodeKeys.get(i) + "' correlation to output: " 
          + neuralNetwork.getOutput()[0]);
    }

    return neuralNetwork;
  }

  /**
   * Generate an nodes array with all indexes = 0 except for the specified
   * index.
   * @throws DatabaseSchemaException 
   */
  static double[] generateIsolatedInputNodes(int enabledIndex) throws DatabaseSchemaException {
    double[] inputs = new double[getInputNodeKeys().size()];
    for (int i = 0; i < inputs.length; i++) {
      inputs[i] = (i == enabledIndex) ? 1.0 : 0.0;
    }
    return inputs;
  }

  /**
   * Returns a list of Notifications that are applicable for training.  We
   * filter this list down because people might uninstall the app or just
   * never click on notifications, so we want our negatives to be actually
   * worth training against.  Therefore, we only accept notifications that
   * are from people who have clicked a notification in the future.
   *
   * Also, people might click the first notification but never click another,
   * so we'll ignore the first one too.
   */
  public static List<Notification> getApplicableNotifications() throws DatabaseSchemaException {
    Multimap<String, Notification> notificationsPerUserId = HashMultimap.create();
    for (Notification notification : Database.with(Notification.class).get(
        new QueryOption.WhereEqualsEnum("device_type", DeviceType.IOS),
        new QueryOption.WhereNotNull("hot_count"))) {
      notificationsPerUserId.put(notification.getUserId(), notification);
    }

    List<Notification> applicableNotifications = Lists.newArrayList();
    Long lastClickedNotificationTime = null;
    for (String userId : notificationsPerUserId.keySet()) {
      // Get each user's notifications, sorted by creation time.
      List<Notification> notificationsPerUser =
          Lists.newArrayList(notificationsPerUserId.get(userId));
      notificationsPerUser.sort(new Comparator<Notification>() {
        @Override
        public int compare(Notification o1, Notification o2) {
          return Long.compare(o1.getCreateTime(), o2.getCreateTime());
        }
      });

      // Now, go through the notifications in time order.  If the user clicked
      // any, add it and any previous notifications to the training
      // notifications list.
      List<Notification> unclickedNotifications = Lists.newArrayList();
      for (Notification notification : notificationsPerUser) {
        if (notification.hasClickTime()) {
          lastClickedNotificationTime = notification.getClickTime();
          applicableNotifications.add(notification);
          applicableNotifications.addAll(unclickedNotifications);
          unclickedNotifications.clear();
        } else {
          unclickedNotifications.add(notification);
        }
      }

      // Also throw in any notifications 2 days after any click that are at
      // least 8 hours old.
      if (lastClickedNotificationTime != null) {
        for (Notification notification : unclickedNotifications) {
          if ((Math.abs(notification.getCreateTime() - lastClickedNotificationTime)
                  < TimeUnit.DAYS.toMillis(2))
              && (System.currentTimeMillis() - notification.getCreateTime())
                  < TimeUnit.HOURS.toMillis(8)) {
            applicableNotifications.add(notification);
          }
        }
      }
    }

    System.out.println("Applicable notifications: " + applicableNotifications.size());
    return applicableNotifications;
  }

  private static DataSet generateTrainingDataSet(List<Notification> trainingNotifications)
      throws DatabaseSchemaException, BiznessException {
    int goodNotificationCount = 0;
    int badNotificationCount = 0;
    List<DataSetRow> dataSetRows = Lists.newArrayList();
    for (Notification trainingNotification : trainingNotifications) {
      ArticleEvaluation evaluation = new ArticleEvaluation(trainingNotification);
      DataSetRow row = evaluation.getDataSetRow(trainingNotification.hasClickTime() /* clicked */);
      dataSetRows.add(row);
      if (row.getDesiredOutput()[0] > 0.5) {
        goodNotificationCount++;
      } else {
        badNotificationCount++;
      }
    }
    System.out.println("Training set compiled. good=" + goodNotificationCount
        + ", bad=" + badNotificationCount);

    // UNBELIEVABLE!!  Neuroph comes up with far different results depending on
    // the order of the data set rows.  To combat this, and to allow us to try
    // different configurations to hopefully find an optimal, randomize the
    // rows.
    Collections.shuffle(dataSetRows);

    DataSet trainingSet = new DataSet(getInputNodeKeys().size(), 1 /* output nodes count */);
    for (DataSetRow dataSetRow : dataSetRows) {
      trainingSet.addRow(dataSetRow);
    }
    return trainingSet;
  }

  @Override
  public void handleLearningEvent(LearningEvent event) {
    BackPropagation bp = (BackPropagation) event.getSource();
    double error = bp.getTotalNetworkError();
    if (bp.getCurrentIteration() % 1000 == 1) {
      System.out.println(bp.getCurrentIteration() + ". iteration : "+ error);
    }
    lastNetworkWeights = bp.getNeuralNetwork().getWeights();

//    if (error < lowestError) {
      lowestError = error;
      lowestErrorIteration = bp.getCurrentIteration();
      lowestErrorNetworkWeights = bp.getNeuralNetwork().getWeights();
//    }
    if (bp.getCurrentIteration() >= MAX_ITERATIONS) {
      bp.stopLearning();
    }
  }

  /**
   * Definitely not-so-efficient hack for getting a list of input name labels,
   * e.g. "industries", "facebook", "acquisitions", etc.
   */
  private static synchronized List<String> getInputNodeKeys() throws DatabaseSchemaException {
    if (INPUT_NODE_KEYS == null) {
      INPUT_NODE_KEYS = ImmutableList.copyOf(
          new ArticleEvaluation(
              Database.with(Notification.class).getFirst(
                  new QueryOption.WhereEqualsEnum("device_type", DeviceType.IOS)))
          .generateInputNodes().keySet());
    }
    return INPUT_NODE_KEYS;
  }

  private static void printAverageInputValues(DataSet dataSet) throws DatabaseSchemaException {
    // Calculate average input values, for debugging/optimization purposes.
    int inputNodesCount = getInputNodeKeys().size();
    Averager[] averageInputValues = new Averager[inputNodesCount];
    Averager[] averageInputValuesPositive = new Averager[inputNodesCount];
    Averager[] averageInputValuesNegative = new Averager[inputNodesCount];
    for (int i = 0; i < inputNodesCount; i++) {
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
          + "diff=" + Math.abs(averageInputValuesPositive[i].get()
              - averageInputValuesNegative[i].get()) + ")");
    }
  }

  public static class NeuralNetworkFinder implements Callable<NeuralNetwork<BackPropagation>> {
    private final int hiddenNodeCount;
    private final DataSet dataSet;

    public NeuralNetworkFinder(DataSet dataSet, int hiddenNodeCount) {
      this.dataSet = dataSet;
      this.hiddenNodeCount = hiddenNodeCount;
    }

    @Override
    public NeuralNetwork<BackPropagation> call() throws Exception {
      NotificationNeuralNetworkTrainer neuralNetworkTrainer =
          new NotificationNeuralNetworkTrainer();
      NeuralNetwork<BackPropagation> neuralNetwork =
          neuralNetworkTrainer.generateTrainedNetwork(dataSet, hiddenNodeCount);
      System.out.println("** SCORE FOR " + hiddenNodeCount + " HIDDEN NODES...");
      return neuralNetwork;
    }
  }

  /**
   * Grades a neural network based on a set of holdback notification.
   * Higher scores are better.
   */
  public static double getOutput(NeuralNetwork<BackPropagation> neuralNetwork,
      List<Notification> holdbackNotifications) {
    Averager averagePositiveScore = new Averager();
    Averager averageNegativeScore = new Averager();
    NotificationNeuralNetworkScorer scorer = new NotificationNeuralNetworkScorer(neuralNetwork);
    for (Notification holdbackNotification : holdbackNotifications) {
      double output = scorer.getOutput(new ArticleEvaluation(holdbackNotification));
      if (holdbackNotification.hasClickTime()) {
        averagePositiveScore.add(output);
      } else {
        averageNegativeScore.add(output);
      }
    }
    System.out.println("Average positive: " + averagePositiveScore.get()
        + ", average negative: " + averageNegativeScore.get());

    // The article's score is the # of true positives * 2 + true negatives - the
    // falsies, as determined by whether they're over/under a divider.
    double divider = (averagePositiveScore.get() * 3 + averageNegativeScore.get()) / 4;
    double score = 0;
    for (Notification holdbackNotification : holdbackNotifications) {
      double output = scorer.getOutput(new ArticleEvaluation(holdbackNotification));
      if (holdbackNotification.hasClickTime()) {
        score += (output - divider);
      } else {
        score += (divider - output);
      }
    }
    System.out.println("Divider = " + divider + ", score = " + score);
    return score;
  }

  public static void main(String args[]) throws Exception {
    // Collect previously sent notifications, for which we've recorded whether
    // the user engaged with them or not.  Use 75% of them for training, then
    // hold back 25% for judging which neural network is the best.
    List<Notification> applicableNotifications = getApplicableNotifications();
    List<Notification> trainingNotifications = Lists.newArrayList();
    List<Notification> holdbackNotifications = Lists.newArrayList();
    for (int i = 0; i < applicableNotifications.size(); i++) {
      if (i % 4 == 0) {
        holdbackNotifications.add(applicableNotifications.get(i));
      } else {
        trainingNotifications.add(applicableNotifications.get(i));
      }
    }

    DataSet dataSet = generateTrainingDataSet(trainingNotifications);
    printAverageInputValues(dataSet);

    // Create a threads to general neural networks for each of our
    // desired hidden node counts.
    ExecutorService executor = Executors.newFixedThreadPool(8);
    List<NeuralNetworkFinder> neuralNetworkFinderList = Lists.newArrayList();
    Map<NeuralNetworkFinder, Future<NeuralNetwork<BackPropagation>>> neuralNetworkFutureMap =
        Maps.newHashMap();
    for (int hiddenNodeCount : new int[] { 2, 3, 4, 5, 6 }) {
      for (int tries = 0; tries < 15; tries++) {
        NeuralNetworkFinder neuralNetworkFinder = new NeuralNetworkFinder(dataSet, hiddenNodeCount);
        neuralNetworkFinderList.add(neuralNetworkFinder);
        neuralNetworkFutureMap.put(neuralNetworkFinder, executor.submit(neuralNetworkFinder));
      }
    }
    executor.shutdown();

    // Evaluate the outcomes from each thread.
    int bestHiddenNodeCount = Integer.MIN_VALUE;
    double bestNeuralNetworkOutput = Double.MIN_VALUE;
    Map<Integer, Double> bestScorePerTopology = Maps.newHashMap();
    NeuralNetwork<BackPropagation> bestNeuralNetwork = null;
    for (NeuralNetworkFinder neuralNetworkFinder : neuralNetworkFinderList) {
      int hiddenNodeCount = neuralNetworkFinder.hiddenNodeCount;
      NeuralNetwork<BackPropagation> neuralNetwork =
          neuralNetworkFutureMap.get(neuralNetworkFinder).get();
      double output = getOutput(neuralNetwork, holdbackNotifications);
      if (output > bestNeuralNetworkOutput) {
        bestNeuralNetworkOutput = output;
        bestNeuralNetwork = neuralNetwork;
        bestHiddenNodeCount = hiddenNodeCount;
      }
      if (bestScorePerTopology.containsKey(hiddenNodeCount)) {
        bestScorePerTopology.put(hiddenNodeCount,
            Math.max(output, bestScorePerTopology.get(hiddenNodeCount)));
      } else {
        bestScorePerTopology.put(hiddenNodeCount, output);
      }
    }

    // Display the outcomes per hidden node count.
    System.out.println();
    System.out.println("Performances of different topologies:");
    for (int i = 0; i < 100; i++) {
      if (bestScorePerTopology.containsKey(i)) {
        System.out.println(i + " hidden nodes: " + bestScorePerTopology.get(i));
      }
    }

    // Save the best one.
    System.out.println("Saving best neural network "
        + "(btw it has " + bestHiddenNodeCount + " hidden nodes)");
    bestNeuralNetwork.save(NotificationNeuralNetworkScorer.DEFAULT_NEURAL_NETWORK_FILE);

    // Save a distribution for the holdback.
    DistributionBuilder builder = new DistributionBuilder();
    NotificationNeuralNetworkScorer scorer = new NotificationNeuralNetworkScorer(bestNeuralNetwork);
    for (Notification holdbackNotification : holdbackNotifications) {
      builder.add(scorer.getOutput(new ArticleEvaluation(holdbackNotification)));
    }
    FileOutputStream fos = new FileOutputStream(NotificationNeuralNetworkScorer.DISTRIBUTION_FILE);
    builder.build().writeTo(fos);
    fos.close();
  }
}
