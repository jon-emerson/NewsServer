package com.janknspank.classifier;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.WordDocumentFrequencies;
import com.janknspank.interpreter.KeywordFinder;
import com.janknspank.interpreter.KeywordUtils;
import com.janknspank.proto.Core.Article;

public class DocumentVector {
  private Multiset<String> frequencyVector;
  private Map<String, Double> tfIdfVector;
  private static final Set<String> STOP_WORDS;
  
  static {
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream("neuralnet/english_stopwords");
      STOP_WORDS = ImmutableSet.copyOf(
          IOUtils.toString(inputStream, "UTF-8").split("\r?\n"));
    } catch (IOException e) {
      throw new Error("Can't read stopwords file. Won't be able to create valid DocumentVectors");
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
    
  }
  
  public DocumentVector(Article article) throws DataInternalException {
    frequencyVector = generateFrequencyVector(article);
    // Note: tfIDFVector is lazy loaded to prevent a circular
    // dependency on WodDocumentFrequencies
  }
  
  Multiset<String> getFrequencyVector() {
    return frequencyVector;
  }
  
  Map<String, Double> getTFIDFVector() 
      throws DataInternalException {
    if (tfIdfVector != null) {
      return tfIdfVector;
    } else {
      tfIdfVector = generateTFIDFVectorFromTF(frequencyVector);
      return tfIdfVector;
    }
  }
  
  private static Multiset<String> generateFrequencyVector(Article article) {
    Multiset<String> vector = HashMultiset.create();
    
    // Get all words
    List<String> paragraphs = new ArrayList<>(article.getParagraphList());
    paragraphs.add(article.getTitle());
    paragraphs.add(article.getDescription());
    
    for (String paragraph : paragraphs) {
      // For each word increment the frequencyVector
      String[] tokens = KeywordFinder.getTokens(paragraph);
      if (tokens != null) {
        for (String token : tokens) {
          token = KeywordUtils.cleanKeyword(token);
          if (!Strings.isNullOrEmpty(token) &&
              !STOP_WORDS.contains(token.toLowerCase())) {
            vector.add(token);
          }
        }
      }
    }
    return vector;
  }
  
  static Map<String, Double> generateTFIDFVectorFromTF(Multiset<String> tfVector) 
      throws DataInternalException {
    Map<String, Double> tfIdfVector = new HashMap<>();
    WordDocumentFrequencies dfs = null;
    int totalDocumentsN;
    dfs = WordDocumentFrequencies.getInstance();
    totalDocumentsN = dfs.getN();
    
    for (Multiset.Entry<String> wordFrequency : tfVector.entrySet()) {
      String word = wordFrequency.getElement();
      int tf = wordFrequency.getCount();
      int df = dfs.getFrequency(word);
      // There's a chance a new word in an article is not
      // yet in the WordDocumentFrequencies table.
      // For now ignore it. Solution is to regenerate WDF
      if (df != 0) {
        double idf = Math.log(totalDocumentsN / df);
        double tfIdf = tf * idf;
        tfIdfVector.put(word, tfIdf);
      }
    }
    
    return tfIdfVector;
  }
  
  public Set<String> getUniqueWordsInDocument() {
    return frequencyVector.elementSet();
  }
  
  public double cosineSimilarityTo(DocumentVector document) 
      throws DataInternalException, IOException {
    return cosineSimilarity(getTFIDFVector(), document.tfIdfVector);
  }
  
  public double cosineSimilarityTo(IndustryVector industry) 
      throws DataInternalException {
    return cosineSimilarity(getTFIDFVector(), industry.getVector());
  }
  
  // Note: this function normalizes by length of each document
  // So not necessary to normalize by length elsewhere
  static double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
    Set<String> both = Sets.newHashSet(v1.keySet());
    both.retainAll(v2.keySet());
    double sclar = 0, norm1 = 0, norm2 = 0;
    for (String k : both) {
      sclar += v1.get(k) * v2.get(k);
    }
    for (String k : v1.keySet()) {
      norm1 += v1.get(k) * v1.get(k);
    }
    for (String k : v2.keySet()) {
      norm2 += v2.get(k) * v2.get(k);
    }
    return sclar / Math.sqrt(norm1 * norm2);
  }
  
  public String toString() {
    return "DocumentVector.tfIdfVector: " + tfIdfVector;
  }
}
