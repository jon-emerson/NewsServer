package com.janknspank.rank;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.learning.BackPropagation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.janknspank.bizness.Urls;
import com.janknspank.bizness.UserIndustries;
import com.janknspank.bizness.UserInterests;
import com.janknspank.bizness.Users;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.Asserts;
import com.janknspank.crawler.Interpreter;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.InterpretedData;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;

public final class NeuralNetworkScorer extends Scorer {
  static final String DEFAULT_NEURAL_NETWORK_FILE = "neuralnet/backpropagation_out.nnet";
  private static NeuralNetworkScorer instance = null;
  private NeuralNetwork<BackPropagation> neuralNetwork;

  @SuppressWarnings("unchecked")
  private NeuralNetworkScorer() {
    neuralNetwork = NeuralNetwork.createFromFile(DEFAULT_NEURAL_NETWORK_FILE);
  }

  public NeuralNetworkScorer(NeuralNetwork<BackPropagation> neuralNetwork) {
    this.neuralNetwork = neuralNetwork;
  }

  public static synchronized NeuralNetworkScorer getInstance() {
    if (instance == null) {
      instance = new NeuralNetworkScorer();
    }
    return instance;
  }

  public static LinkedHashMap<String, Double> generateInputNodes(User user, Article article) {
    Asserts.assertNotNull(user, "user", NullPointerException.class);
    Asserts.assertNotNull(article, "article", NullPointerException.class);
    LinkedHashMap<String, Double> linkedHashMap = Maps.newLinkedHashMap();

    // 0. Relevant to user's industries?
    // NOTE(jonemerson): This used to be a raw signal of the highest similarity
    // score that matched a user's industries.  Turns out, the neural network
    // does a lot better if we're more explicit about it... to the tune of 3%
    // improvement on error rates (!!!).  Therefore, we return in buckets of 0,
    // 0.5, and 1.0, instead of a true double.
    double relevance = InputValuesGenerator.relevanceToUserIndustries(user, article);
    linkedHashMap.put("industries", (relevance > 0.5) ? ((relevance > 0.9) ? 1 : 0.5) : 0.0);

    // 1. Nearby industry count.  Value relative to the number of industries
    // this article is about that the user is not explicitly interested in.
    linkedHashMap.put("industry-specific",
        InputValuesGenerator.relevanceToNonUserIndustries(user, article));

    // 2. Relevance on Facebook.
    linkedHashMap.put("facebook", InputValuesGenerator.relevanceOnFacebook(user, article));

    // 3. Relevance on Twitter.
    linkedHashMap.put("twitter", InputValuesGenerator.relevanceOnTwitter(user, article));

    // 4. Relevance to contacts.
    linkedHashMap.put("contacts", InputValuesGenerator.relevanceToContacts(user, article));

    // 5. Company / organization entities being followed.
    linkedHashMap.put("companies", InputValuesGenerator.relevanceToCompanyEntities(user, article));

    // 6. Relevance to startup vector.
    linkedHashMap.put("startup", InputValuesGenerator.relevanceToStartups(user, article));

    // 7. Relevance to acquisitions.
    Set<FeatureId> userIndustryFeatureIds = getUserIndustryFeatureIds(user);
    linkedHashMap.put("acquisitions",
        InputValuesGenerator.relevanceToAcquisitions(userIndustryFeatureIds, article));

    // 8. Relevance to launches.
    linkedHashMap.put("launches",
        InputValuesGenerator.relevanceToLaunches(userIndustryFeatureIds, article));

    // 9. Relevance to start-up fundraising rounds.
    linkedHashMap.put("fundraising",
        InputValuesGenerator.relevanceToFundraising(userIndustryFeatureIds, article));

    // 10. Topic scores.  If the user's actually interested in any of these
    // things, then we null out the scores (because otherwise the neural
    // network just learns that some folks like Sports + Politics + etc, without
    // knowing why, which is a really bad thing for overall quality.)
    linkedHashMap.put("entertainment",
        InputValuesGenerator.getOptimizedFeatureValue(article, FeatureId.TOPIC_ENTERTAINMENT));
    linkedHashMap.put("sports", UserIndustries.hasFeatureId(user, FeatureId.SPORTS)
        ? 0 : InputValuesGenerator.getOptimizedFeatureValue(article, FeatureId.TOPIC_SPORTS));
    linkedHashMap.put("politics", UserIndustries.hasFeatureId(user, FeatureId.GOVERNMENT)
        ? 0 : InputValuesGenerator.getOptimizedFeatureValue(article, FeatureId.TOPIC_POLITICS));
    linkedHashMap.put("murder_crime_war", UserIndustries.hasFeatureId(user, FeatureId.MILITARY)
        ? 0 : InputValuesGenerator.getOptimizedFeatureValue(article, FeatureId.TOPIC_MURDER_CRIME_WAR));

    // 12. Relevance to big money
    linkedHashMap.put("big_money",
        InputValuesGenerator.relevanceToBigMoney(userIndustryFeatureIds, article));

    // 13. Relevance to quarterly earnings
    linkedHashMap.put("quarterly_earnings", 
        InputValuesGenerator.relevanceToQuarterlyEarnings(userIndustryFeatureIds, article));

    return linkedHashMap;
  }

  /**
   * Returns a Set of all the industries the passed user is following, as
   * FeatureId objects.
   */
  @VisibleForTesting
  static Set<FeatureId> getUserIndustryFeatureIds(User user) {
    return ImmutableSet.copyOf(
        Iterables.transform(
            Iterables.filter(
                UserInterests.getInterests(user),
                new Predicate<Interest>() {
                  @Override
                  public boolean apply(Interest interest) {
                    return interest.getType() == InterestType.INDUSTRY;
                  }
                }),
            new Function<Interest, FeatureId>() {
              @Override
              public FeatureId apply(Interest interest) {
                return FeatureId.fromId(interest.getIndustryCode());
              }
            }));
  }

  @Override
  public double getScore(User user, Article article) {
    Asserts.assertNotNull(user, "user", NullPointerException.class);
    Asserts.assertNotNull(article, "article", NullPointerException.class);
    return getScore(generateInputNodes(user, article));
  }

  public double getScore(LinkedHashMap<String, Double> inputNodes) {
    neuralNetwork.setInput(Doubles.toArray(inputNodes.values()));
    neuralNetwork.calculate();
    return neuralNetwork.getOutput()[0];
  }

  /**
   * Usage:
   * bin/score.sh jon@jonemerson.net http://path/to/article
   */
  public static void main(String args[]) throws Exception {
    User user = Users.getByEmail(args[0]);
    if (user == null) {
      throw new RuntimeException("User not found: " + args[0]);
    }

    String urlString = args[1];
    Url url = Urls.getByUrl(urlString);
    if (url == null) {
      url = Urls.put(urlString, "");
    }
    InterpretedData data = Interpreter.interpret(url);
    System.out.println(data.getArticle());

    LinkedHashMap<String, Double> inputNodes = generateInputNodes(user, data.getArticle());
    for (Map.Entry<String, Double> entry : inputNodes.entrySet()) {
      System.out.println("Node " + entry.getKey() + ": " + entry.getValue());
    }
    System.out.println("Score: " + getInstance().getScore(inputNodes));
  }
}