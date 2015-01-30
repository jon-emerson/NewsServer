package com.janknspank.bizness;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.janknspank.classifier.DocumentVector;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.WordDocumentFrequency;

/**
 * Computes and returns the # of documents in the corpus that contain a given
 * term. This is the DF part of TF-IDF.
 */
public class WordDocumentFrequencies {
  private Multiset<String> documentFrequency;
  private static WordDocumentFrequencies instance = null;
  private Integer totalDocumentCount;
  private static final String PROPERTY_KEY_N = "numArticlesUsedForModel";
  private static final String PROPERTY_FILE_NAME = "classifier/vectorspace.properties";

  private WordDocumentFrequencies() throws BiznessException {
    try {
      documentFrequency = loadPreviouslyComputedDocumentFrequencies();
      if (documentFrequency == null) {
        documentFrequency = generateDocumentFrequenciesFromCorpus();
        saveDocumentFrequenciesToServer(documentFrequency);
      }
    } catch (DatabaseSchemaException e) {
      throw new BiznessException("Could not calculate word document frequencies: " + e.getMessage(), e);
    }
  }

  public static synchronized WordDocumentFrequencies getInstance() throws BiznessException {
    if (instance == null) {
      instance = new WordDocumentFrequencies();
    }
    return instance;
  }

  // Expensive - only do infrequently as the corpus grows
  private static Multiset<String> generateDocumentFrequenciesFromCorpus()
      throws DatabaseSchemaException, BiznessException {
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
      throws DatabaseSchemaException {
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
          } catch (DatabaseRequestException e) {
            System.out.println("Error saving page of document frequencies to the server,"
                + " but keep going");
            e.printStackTrace();
          }
          pageToSave.clear();
        }
      }
    }
  }

  private Multiset<String> loadPreviouslyComputedDocumentFrequencies() throws DatabaseSchemaException {
    Iterable<WordDocumentFrequency> wdfs;
    wdfs = Database.with(WordDocumentFrequency.class).get();

    Multiset<String> frequencyMap = HashMultiset.create();
    for (WordDocumentFrequency wdf : wdfs) {
      frequencyMap.add(wdf.getWord(), (int) wdf.getFrequency());
    }
    return frequencyMap;
  }

  public int getFrequency(String word) {
    try {
      return documentFrequency.count(word);
    } catch (NullPointerException e) {
      return 0;
    }
  }

  public int getN() throws BiznessException {
    if (totalDocumentCount == null) {
      Properties properties;
      properties = getVectorSpaceProperties();
      String propertyNString = properties.getProperty(PROPERTY_KEY_N);
      if (Strings.isNullOrEmpty(propertyNString)) {
        throw new BiznessException(
            "Property file doesn't contain a valid total document count N");
      }
      totalDocumentCount = Integer.parseInt(propertyNString);
    }
    return totalDocumentCount;
  }

  private static void saveNLocally(int numDocs) throws BiznessException {
    OutputStream out = null;
    try {
      Properties properties = getVectorSpaceProperties();
      properties.setProperty(PROPERTY_KEY_N, String.valueOf(numDocs));
      out = new FileOutputStream(PROPERTY_FILE_NAME);
      properties.store(out, "Updated N");
    } catch (IOException e) {
      throw new BiznessException(
          "Issue saving N - the total # of arcitles in the corpus to file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  private static Properties getVectorSpaceProperties() throws BiznessException {
    Properties properties = new Properties();
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(PROPERTY_FILE_NAME);
      properties.load(inputStream);
      return properties;
    } catch (IOException e) {
      throw new BiznessException("Error reading " + PROPERTY_FILE_NAME + ": " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(WordDocumentFrequency.class).createTable();
    Multiset<String> df = generateDocumentFrequenciesFromCorpus();
    saveDocumentFrequenciesToServer(df);
  }
}