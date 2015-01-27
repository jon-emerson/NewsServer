package com.janknspank.rank;

import java.io.IOException;

import org.neuroph.core.NeuralNetwork;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleFacebookEngagement;

public final class NeuralNetworkScorer implements Scorer {
  static final int INPUT_NODES_COUNT = 8;
  static final int OUTPUT_NODES_COUNT = 1;
  static final int HIDDEN_NODES_COUNT = INPUT_NODES_COUNT + OUTPUT_NODES_COUNT + 1;
  static final String DEFAULT_NEURAL_NETWORK_FILE = "neuralnet/default_mlp_" + 
      INPUT_NODES_COUNT + "in-" + HIDDEN_NODES_COUNT + "hidden-" + 
      OUTPUT_NODES_COUNT + "out.nnet";
  private static NeuralNetworkScorer instance = null;
  private NeuralNetwork neuralNetwork;
  
  private NeuralNetworkScorer() {
    setFile(DEFAULT_NEURAL_NETWORK_FILE);
  }
  
  public static synchronized NeuralNetworkScorer getInstance() {
    if(instance == null) {
       instance = new NeuralNetworkScorer();
    }
    return instance;
  }
  
  public void setFile(String nnetFile) {
    neuralNetwork = NeuralNetwork.load(nnetFile);
  }
  
  static double[] generateInputNodes(CompleteUser user, 
      CompleteArticle article) {
    ArticleFacebookEngagement engagement = article.getLatestFacebookEngagement();
    long likeCount = 0;
    long commentCount = 0;
    long shareCount = 0;
    if (engagement != null) {
      likeCount = engagement.getLikeCount();
      commentCount = engagement.getCommentCount();
      shareCount = engagement.getShareCount();
    }
    
    return new double[] {
      // Input 1: equals 1 if article is about the user's current place of work
      InputValuesGenerator.isAboutCurrentEmployer(user, article),
        
      // Input 2: # of topics matches between user and article
      sigmoid(InputValuesGenerator.matchedInterestsCount(user, article)),
        
      // Input 3: the length of the article
      // TODO: improve normalization
      // Use average wordcount and max word count
      sigmoid(Math.log(article.wordCount())), 
      
      // Input 4: Facebook likes
      sigmoid(likeCount),
      
      // Input 5: Facebook comments
      sigmoid(commentCount),
      
      // Input 6: Facebook shares
      sigmoid(shareCount),
      
      // Input 7: Facebook likes velocity
      sigmoid(article.getLikeVelocity()),
      
      // Input 7: Article age
      sigmoid(article.getAgeInMillis())
      
      // Input 8: User has intent to stay on top of industry
      // which the article is about
      
      // Input 9: Article is educational and about a skill
      // the user wants to develop
      
      // TODO: inputs with article classifications like "data-rich"
      
      // ...
    };
  }
  
  // V1 has a general rank - one neural network for all intents. No mixing.
  // Slow architecture. Makes too many server calls
  public double getScore(Article article, CompleteUser user) {
    try {
      return getScore(article, user, neuralNetwork);      
    }
    catch (DataInternalException | IOException | ValidationException e) {
      System.out.println("Error NeuralNetworkScorer.getScore()");
      e.printStackTrace();
      return 0;
    }
  }
  
  private static double getScore(Article article, CompleteUser user, NeuralNetwork neuralNetwork) 
      throws DataInternalException, IOException, ValidationException {
    long startMillis = System.currentTimeMillis();
    CompleteArticle completeArticle = new CompleteArticle(article);
    long completeArticleAcquiredMillis = System.currentTimeMillis();
    neuralNetwork.setInput(generateInputNodes(user, completeArticle));
    long generateInputNodesMillis = System.currentTimeMillis();
    neuralNetwork.calculate();
    long calculateMillis = System.currentTimeMillis();
    
    double totalTimeToRankArticle = (double)(calculateMillis - startMillis) / 1000;
    double timeToInitializeCompleteArticle = (double)(completeArticleAcquiredMillis - startMillis) / 1000;
    double timeToGenerateInputNodes = (double)(generateInputNodesMillis - completeArticleAcquiredMillis) / 1000;
    double timeToCalculate = (double)(calculateMillis - generateInputNodesMillis) / 1000;
    
    System.out.println("Ranked " + article.getUrl());
    System.out.println("  Score: " + neuralNetwork.getOutput()[0]);
    System.out.println("  Total time: " + totalTimeToRankArticle + "s");
    System.out.println("    init article: " + timeToInitializeCompleteArticle + 
        ", generate input values: " + timeToGenerateInputNodes +
        ", compute output: " + timeToCalculate);
    return neuralNetwork.getOutput()[0];
  }
  
  private static double sigmoid(double x) {
    return 1 / (1 + Math.exp(-x));
  }
}