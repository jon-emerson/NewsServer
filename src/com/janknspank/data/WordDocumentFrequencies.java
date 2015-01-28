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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
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
  private Multiset<String> documentFrequency;
  private static WordDocumentFrequencies instance = null;
  private Integer totalDocumentCount;
  private static final String PROPERTY_KEY_N = "numArticlesUsedForModel";
  private static final String PROPERTY_FILE_NAME = "classifier/vectorspace.properties";
  
  private WordDocumentFrequencies() throws DataInternalException {
    documentFrequency = loadPreviouslyComputedDocumentFrequencies();
    if (documentFrequency == null) {
      try {
        documentFrequency = generateDocumentFrequenciesFromCorpus();
      } catch (IOException e) {
        throw new DataInternalException(
            "Can't generate document frequencies from corpus: " + e.getMessage(), e);
      }
      saveDocumentFrequenciesToServer(documentFrequency);
    }
  }
  
  public static synchronized WordDocumentFrequencies getInstance() 
      throws DataInternalException {
    if (instance == null) {
      instance = new WordDocumentFrequencies();
    }
    return instance;
  }
  
  // Expensive - only do infrequently as the corpus grows
  private static Multiset<String> generateDocumentFrequenciesFromCorpus() 
      throws DataInternalException, IOException {
    System.out.println("Warning: calling expensive function: generateWordFrequenciesFromCorpus");
    System.out.println("Use loadPreviouslyComputedDocumentFrequencies instead");
    
    Multiset<String> wordDocumentFrequency = HashMultiset.create();
    int limit = 1000;
    int offset = 0;
    int totalDocumentCount = 0;
    Iterable<Article> articles;
    do {
      articles = Articles.getPageOfArticles(limit, offset);
      System.out.println("Generating Word Document Frequency - article offset: " + offset);
      for (Article article : articles) {
        Set<String> words = new DocumentVector(article).getUniqueWordsInDocument();
        for (String word : words) {
          wordDocumentFrequency.add(word);
        }
      }
      offset += limit;
      totalDocumentCount += Iterables.size(articles);
    } while (Iterables.size(articles) == limit);
    saveNLocally(totalDocumentCount);
    return wordDocumentFrequency;
  }
  
  private static void saveDocumentFrequenciesToServer(Multiset<String> frequencies) 
      throws DataInternalException {
    // Clear the table
    Database.with(WordDocumentFrequency.class).delete();
    
    // Generate the new rows
    List<WordDocumentFrequency> pageToSave = new ArrayList<>();
    for (Multiset.Entry<String> frequency : frequencies.entrySet()) {
      String word = frequency.getElement();
      Integer value = frequency.getCount();
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
  
  private Multiset<String> loadPreviouslyComputedDocumentFrequencies() {
    Multiset<String> frequencyMap = HashMultiset.create();
    Iterable<WordDocumentFrequency> wdfs;
    try {
      wdfs = Database.with(WordDocumentFrequency.class).get();
    } catch (DataInternalException e) {
      return null;
    }
    for (WordDocumentFrequency wdf : wdfs) {
      frequencyMap.add(wdf.getWord(), (int) wdf.getFrequency());
    }
    return frequencyMap;
  }
  
  public int getFrequency(String word) {
    int frequency;
    try {
      frequency = documentFrequency.count(word);
    } catch (NullPointerException e) {
      frequency = 0;
    }
    return frequency;
  }
  
  public int getN() throws DataInternalException {
    if (totalDocumentCount == null) {
      Properties properties;
      try {
        properties = getVectorSpaceProperties();
      } catch (IOException e) {
        throw new DataInternalException(
            "Can't load N - the total # of articles in the corpus: " + e, e);
      }
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
    Multiset<String> df = generateDocumentFrequenciesFromCorpus();
    saveDocumentFrequenciesToServer(df);
  }
}