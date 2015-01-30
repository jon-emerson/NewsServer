package com.janknspank.classifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.WordDocumentFrequencies;
import com.janknspank.common.TopList;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.interpreter.KeywordFinder;
import com.janknspank.interpreter.KeywordUtils;
import com.janknspank.proto.ArticleProto.Article;

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

  public DocumentVector(Article article) {
    frequencyVector = generateFrequencyVector(article);
    // Note: tfIDFVector is lazy loaded to prevent a circular
    // dependency on WodDocumentFrequencies
  }

  Multiset<String> getFrequencyVector() {
    return frequencyVector;
  }

  Map<String, Double> getTFIDFVector() throws BiznessException {
    if (tfIdfVector != null) {
      return tfIdfVector;
    } else {
      try {
        tfIdfVector = generateTFIDFVectorFromTF(frequencyVector);
      } catch (DatabaseSchemaException e) {
        throw new BiznessException("Error generating vector: " + e.getMessage(), e);
      }
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
              !STOP_WORDS.contains(token.toLowerCase()) &&
              token.length() > 2 &&
              !NumberUtils.isNumber(token) &&
              !token.startsWith("T.co/")
              ) {
            vector.add(token);
          }
        }
      }
    }
    return vector;
  }

  static Map<String, Double> generateTFIDFVectorFromTF(Multiset<String> tfVector)
      throws DatabaseSchemaException, BiznessException {
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

  public double cosineSimilarityTo(DocumentVector document) throws BiznessException {
    return cosineSimilarity(getTFIDFVector(), document.tfIdfVector);
  }

  public double cosineSimilarityTo(IndustryVector industry) throws BiznessException {
    return cosineSimilarity(getTFIDFVector(), industry.getVector());
  }

  // Note: this function normalizes by length of each document
  // So not necessary to normalize by length elsewhere
  static double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
    Set<String> both = Sets.newHashSet(v1.keySet());
    both.retainAll(v2.keySet());
    double dotProduct = 0, normalizedLength1 = 0, normalizedLength2 = 0;
    for (String k : both) {
      dotProduct += v1.get(k) * v2.get(k);
    }
    for (String k : v1.keySet()) {
      normalizedLength1 += v1.get(k) * v1.get(k);
    }
    for (String k : v2.keySet()) {
      normalizedLength2 += v2.get(k) * v2.get(k);
    }
    return dotProduct / Math.sqrt(normalizedLength1 * normalizedLength2);
  }

  public String toString() {
    TopList<String, Double> topWords = new TopList<>(tfIdfVector.size());
    for (Map.Entry<String, Double> entry : tfIdfVector.entrySet()) {
      String word = entry.getKey();
      double weight = entry.getValue();
      topWords.add(word, weight);
    }
    
    String output = "";
    for (String word : topWords.getKeys()) {
      output += word + ": " + topWords.getValue(word) + "\n";
    }
    return output;
  }
  
  static void saveVectorToFile(Map<String, Double> vector, String path) 
      throws BiznessException {
    File vectorFile = new File(path);
    if (vectorFile.exists()) {
      vectorFile.delete();
    }
    
    Properties prop = new Properties();
    OutputStream output = null;
    
    try {
      output = new FileOutputStream(vectorFile);
      
      for (Map.Entry<String, Double> entry : vector.entrySet()) {
        String word = entry.getKey();
        String value = String.valueOf(entry.getValue());
        prop.setProperty(word, value);
      }
      
      prop.store(output, null);
    } catch (IOException e) {
      throw new BiznessException("Can't save vector to file " 
          + path + ": " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(output);
    }
  }
  
  static Map<String, Double> loadVectorFromFile(String path) 
      throws BiznessException {
    Properties properties = new Properties();
    InputStream inputStream = null;
    Map<String, Double> vector = new HashMap<>();
    try {
      inputStream = new FileInputStream(path);
      properties.load(inputStream);
      
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        vector.put((String)entry.getKey(), Double.parseDouble((String)entry.getValue()));
      }
      
      return vector;
    } catch (IOException e) {
      throw new BiznessException("Error reading " + path + ": " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }
}
