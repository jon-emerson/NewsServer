package com.janknspank.data;

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

import com.google.common.collect.Iterables;
import com.janknspank.classifier.DocumentVector;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.WordDocumentFrequency;

/**
 * Computes and returns the # of documents in the corpus
 * that contain a given term. This is the DF part of TF-IDF
 * @author tomch
 *
 */
public class WordDocumentFrequencies {
  private Map<String, Integer> documentFrequency;
  private static WordDocumentFrequencies instance = null;
  private Integer totalDocumentCount;
  private static final String PROPERTY_KEY_N = "numArticlesUsedForModel";
  private static final String PROPERTY_FILE_NAME = "classifier/vectorspace.properties";
  
  private WordDocumentFrequencies() throws DataInternalException, IOException {
    documentFrequency = loadPreviouslyComputedDocumentFrequencies();
    if (documentFrequency == null) {
      documentFrequency = generateDocumentFrequenciesFromCorpus();
      saveDocumentFrequenciesToServer(documentFrequency);
    }
  }
  
  public static synchronized WordDocumentFrequencies getInstance() 
      throws DataInternalException, IOException {
    if(instance == null) {
       instance = new WordDocumentFrequencies();
    }
    return instance;
  }
  
  // Expensive - only do infrequently as the corpus grows
  private static Map<String, Integer> generateDocumentFrequenciesFromCorpus() 
      throws DataInternalException, IOException {
    System.out.println("Warning: calling expensive function: generateWordFrequenciesFromCorpus");
    System.out.println("Use loadPreviouslyComputedDocumentFrequencies instead");
    
    Map<String, Integer> wordDocumentFrequency = new HashMap<>();
    int limit = 1000;
    int offset = 0;
    int N = 0;
    Iterable<Article> articles;
    Set<String> words;
    Integer frequency;
    do {
      articles = Articles.getPageOfArticles(limit, offset);
      System.out.println("Generating Word Document Frequency - article offset: " + offset);
      for (Article article : articles) {
        words = new DocumentVector(article).getWordsInDocument();
        for (String word : words) {
          frequency = wordDocumentFrequency.get(word);
          if (frequency == null) {
            wordDocumentFrequency.put(word, 1);
          } else {
            wordDocumentFrequency.put(word, frequency + 1);
          }
        }
      }
      offset += limit;
      N += Iterables.size(articles);
    } while (Iterables.size(articles) == limit);
    saveNLocally(N);
    return wordDocumentFrequency;
  }
  
  private static void saveDocumentFrequenciesToServer(Map<String, Integer> frequencies) 
      throws DataInternalException {
    List<WordDocumentFrequency> pageToSave = new ArrayList<>();
    
    for (Map.Entry<String, Integer> frequency : frequencies.entrySet()) {
      String word = frequency.getKey();
      Integer value = frequency.getValue();
      if (word.length() > 0) {
        pageToSave.add(WordDocumentFrequency.newBuilder()
            .setWord(word)
            .setFrequency(value.intValue())
            .build());
        if (pageToSave.size() >= 10) {
          try {
            Database.insert(pageToSave);
          } catch (ValidationException | DataInternalException e) {
            System.out.println("Error saving page of document frequencies to the server,"
                + " but keep going");
            e.printStackTrace();
          }
          pageToSave.clear();
        }
      }
    }
  }
  
  private static void saveNLocally(int numDocs) throws IOException {
    Properties properties = getVectorSpaceProperties();
    properties.setProperty(PROPERTY_KEY_N, String.valueOf(numDocs));
    File f = new File(PROPERTY_FILE_NAME);
    OutputStream out = new FileOutputStream(f);
    properties.store(out, "Updated N");
  }
  
  private Map<String, Integer> loadPreviouslyComputedDocumentFrequencies() 
      throws DataInternalException {
    Map<String, Integer> frequencyMap = new HashMap<>();
    Iterable<WordDocumentFrequency> wdfs = Database.with(WordDocumentFrequency.class).get();
    for (WordDocumentFrequency wdf : wdfs) {
      frequencyMap.put(wdf.getWord(), (int) wdf.getFrequency());
    }
    return frequencyMap;
  }
  
  public int getFrequency(String word) {
    int frequency;
    try {
      frequency = documentFrequency.get(word);      
    } catch (NullPointerException e) {
      frequency = 0;
    }
    return frequency;
  }
  
  public int getN() throws IOException {
    if (totalDocumentCount == null) {
      Properties properties = getVectorSpaceProperties();
      totalDocumentCount = Integer.parseInt(
          properties.getProperty(PROPERTY_KEY_N));
    }
    return totalDocumentCount;
  }
  
  private static Properties getVectorSpaceProperties() throws IOException {
    Properties properties = new Properties();
    InputStream inputStream = new FileInputStream(PROPERTY_FILE_NAME);
    properties.load(inputStream);
    return properties;
  }
  
  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    try {
      Database.with(WordDocumentFrequency.class).createTable();      
    } catch (DataInternalException e) {
      System.out.println("WordDocumentFrequency table already exists. No need to create it.");
    }
    Map<String, Integer> df = generateDocumentFrequenciesFromCorpus();
    saveDocumentFrequenciesToServer(df);
  }
}