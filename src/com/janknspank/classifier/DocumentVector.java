package com.janknspank.classifier;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Sets;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.WordDocumentFrequencies;
import com.janknspank.interpreter.KeywordFinder;
import com.janknspank.interpreter.KeywordUtils;
import com.janknspank.proto.Core.Article;

public class DocumentVector {
  private Map<String, Integer> frequencyVector;
  private Map<String, Double> tfIdfVector;
  private static Set<String> STOP_WORDS;
  
  static {
    try {
      InputStream inputStream = 
          new FileInputStream("neuralnet/english_stopwords");
        String myString = IOUtils.toString(inputStream, "UTF-8");
      String words[] = myString.split("\\r?\\n");
      STOP_WORDS = new HashSet<>(words.length);
      for (String word : words) {
        STOP_WORDS.add(word);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public DocumentVector(Article article) throws DataInternalException {
    frequencyVector = generateFrequencyVector(article);
    // Note: tfIDFVector is lazy loaded to prevent a circular
    // dependency on WodDocumentFrequencies being generated
  }
  
  static Map<String, Double> generateTFIDFVectorFromTF(Map<String, Integer> tfVector) 
      throws DataInternalException, IOException {
    System.out.println("generateTFIDFVectorFromTF");
    Map<String, Double> tfIdfVector = new HashMap<>();
    int tf;
    double idf;
    double tfidf;
    
    WordDocumentFrequencies df = WordDocumentFrequencies.getInstance();
    int N = df.getN();
    
    for (Map.Entry<String, Integer> wordFrequency : tfVector.entrySet()) {
      String word = wordFrequency.getKey();
      tf = wordFrequency.getValue().intValue();
      idf = Math.log(N / df.getFrequency(word));
      tfidf = tf * idf;
      tfIdfVector.put(word, tfidf);
    }
    
    return tfIdfVector;
  }
  
  Map<String, Integer> getFrequencyVector() {
    return frequencyVector;
  }
  
  Map<String, Double> getTFIDFVector() 
      throws DataInternalException, IOException {
    if (tfIdfVector != null) {
      return tfIdfVector;
    }
    else {
      tfIdfVector = generateTFIDFVectorFromTF(frequencyVector);
      return tfIdfVector;
    }
  }
  
  private static Map<String, Integer> generateFrequencyVector(Article article) {
    Map<String, Integer> vector = new HashMap<>();
    // Get all words
    List<String> paragraphs = new ArrayList<>(article.getParagraphList());
    paragraphs.add(article.getTitle());
    paragraphs.add(article.getDescription());
    
    Integer tokenFrequency;
    for (String paragraph : paragraphs) {
      // For each word increment the frequencyVector
      for (String token : KeywordFinder.getTokens(paragraph)) {
        token = KeywordUtils.cleanKeyword(token);
        if (!STOP_WORDS.contains(token)) {
          tokenFrequency = vector.get(token);
          if (tokenFrequency == null) {
            vector.put(token, new Integer(0));
          }
          else {
            vector.put(token, new Integer(tokenFrequency.intValue() + 1));
          }
        }
      }
    }
    return vector;
  }
  
  public Set<String> getWordsInDocument() {
    return frequencyVector.keySet();
  }
  
  public double cosineSimilarityTo(DocumentVector document) 
      throws DataInternalException, IOException {
    return cosineSimilarity(getTFIDFVector(), document.tfIdfVector);
  }
  
  public double cosineSimilarityTo(IndustryVector industry) 
      throws DataInternalException, IOException {
    return cosineSimilarity(getTFIDFVector(), industry.getVector());
  }
  
  // Note: this function normalizes by length of each document
  // So not necessary to normalize by length elsewhere
  static double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
    Set<String> both = Sets.newHashSet(v1.keySet());
    both.retainAll(v2.keySet());
    double sclar = 0, norm1 = 0, norm2 = 0;
    for (String k : both) sclar += v1.get(k) * v2.get(k);
    for (String k : v1.keySet()) norm1 += v1.get(k) * v1.get(k);
    for (String k : v2.keySet()) norm2 += v2.get(k) * v2.get(k);
    return sclar / Math.sqrt(norm1 * norm2);
  }
}
