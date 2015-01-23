package com.janknspank.neuralnet;

import org.neuroph.core.NeuralNetwork;

import com.janknspank.data.Articles;
import com.janknspank.data.DataInternalException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.Article;

public final class NeuralNetworkDriver {
  static final int inputNodesCount = 3;
  static final int outputNodesCount = 1;
  static final String defaultNeuralNetworkFile = "neuralnet/default_mlp_3in-1out.nnet";
  private static NeuralNetworkDriver instance = null;
  private NeuralNetwork neuralNetwork;
  
  private NeuralNetworkDriver() {
    setFile(defaultNeuralNetworkFile);
  }
  
  public static synchronized NeuralNetworkDriver getInstance() {
    if(instance == null) {
       instance = new NeuralNetworkDriver();
    }
    return instance;
  }
  
  public void setFile(String nnetFile) {
    neuralNetwork = NeuralNetwork.load(nnetFile);
  }
  
  public static double[] generateInputNodes(CompleteUser user, 
      CompleteArticle article) {
    return new double[] {
      // TODO Input 1: Bool if article is about the user's current place of work
      0,
        
      // Input 2: # of topics matches between user and article
      sigmoid(HeuristicInputs.matchedInterestsCount(user, article)),
        
      // Input 3: the length of the article
      sigmoid(article.wordCount()) // Maybe use max word count instead
        
      // ...
    };
  }
  
  // V1 has a general rank - one neural network for all intents. No mixing.
  // Slow architecture. Makes too many server calls
  public static double getRank(String urlId, String userId) 
      throws DataInternalException, ParserException {
    NeuralNetwork neuralNetwork = NeuralNetwork.load(defaultNeuralNetworkFile);
    CompleteUser user = new CompleteUser(userId);
    return getRank(urlId, user, neuralNetwork);
  }
  
  public double getRank(String urlId, CompleteUser user) throws DataInternalException {
    return getRank(urlId, user, neuralNetwork);
  }
  
  public double getRank(Article article, CompleteUser user) throws DataInternalException {
    return getRank(article, user, neuralNetwork);
  }
  
  private static double getRank(String urlId, CompleteUser user, NeuralNetwork neuralNetwork) 
      throws DataInternalException {
    Article article = Articles.getArticle(urlId);
    return getRank(article, user, neuralNetwork);
  }
  
  private static double getRank(Article article, CompleteUser user, NeuralNetwork neuralNetwork) 
      throws DataInternalException {
    CompleteArticle completeArticle = new CompleteArticle(article);
    neuralNetwork.setInput(generateInputNodes(user, completeArticle));
    neuralNetwork.calculate();
    return neuralNetwork.getOutput()[0];
  }
  
  private static double sigmoid(double x) {
    return 1 / (1 + Math.exp(-x));
  }
}