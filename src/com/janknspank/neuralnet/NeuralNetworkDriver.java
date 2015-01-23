package com.janknspank.neuralnet;

import org.neuroph.core.NeuralNetwork;

import com.janknspank.data.DataInternalException;
import com.janknspank.dom.parser.ParserException;

public class NeuralNetworkDriver {
  static final int inputNodesCount = 3;
  static final int outputNodesCount = 1;
  static final String defaultNNetFile = "neuralnet/default_mlp_3in-1out.nnet";
  private static NeuralNetworkDriver instance = null;
  private NeuralNetwork neuralNetwork;
  private CompleteUser user;
  
  protected NeuralNetworkDriver() {
    setFile(defaultNNetFile);
  }
  
  public static synchronized NeuralNetworkDriver getInstance() {
    if(instance == null) {
       instance = new NeuralNetworkDriver();
    }
    return instance;
  }
  
  public void setUser(String userId) throws DataInternalException, ParserException {
    user = new CompleteUser(userId);
  }
  
  public void setFile(String nnetFile) {
    neuralNetwork = NeuralNetwork.load(nnetFile);
  }
  
  public double getRank(String urlId) throws DataInternalException {
    return getRank(urlId, user, neuralNetwork);
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
    NeuralNetwork neuralNetwork = NeuralNetwork.load(defaultNNetFile);
    CompleteUser user = new CompleteUser(userId);
    return getRank(urlId, user, neuralNetwork);
  }
  
  private static double getRank(String urlId, CompleteUser user, NeuralNetwork neuralNetwork) 
      throws DataInternalException {
    CompleteArticle article = new CompleteArticle(urlId);
    neuralNetwork.setInput(generateInputNodes(user, article));
    neuralNetwork.calculate();
    return neuralNetwork.getOutput()[0];
  }
  
  private static double sigmoid(double x)
  {
    return 1 / (1 + Math.exp(-x));
  }
}