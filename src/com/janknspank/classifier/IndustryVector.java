package com.janknspank.classifier;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.io.Files;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.common.TopList;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.IndustryCode;

public class IndustryVector {
  private Map<String, Double> tfIdfVector;
  private IndustryCode industryCode;
  private static final String INDUSTRIES_DIRECTORY = "classifier";

  public IndustryVector(IndustryCode code) throws DatabaseSchemaException, BiznessException {
    industryCode = code;
    tfIdfVector = loadVector(industryCode);
    if (tfIdfVector == null) {
      // Couldn't load IndustryVector from file
      // Try to generate it from scratch
      tfIdfVector = generateVectorForIndustryCode(industryCode);
      saveVector(tfIdfVector, industryCode);
    }
  }

  private static Map<String, Double> generateVectorForIndustryCode(IndustryCode industryCode)
      throws DatabaseSchemaException, BiznessException {
    // 1. Get seed words for industryCode.id
    List<String> words = getSeedWords(industryCode);

    // 2. Get all documents that contain the seed word
    Iterable<Article> articles = Articles.getArticlesForKeywords(words);

    // 3. Convert them into the industry vector
    List<DocumentVector> documentVectors = new ArrayList<>();
    for (Article article : articles) {
      documentVectors.add(new DocumentVector(article));
    }
    Multiset<String> frequencyVector = sumVectors(documentVectors);
    
    // 4. Remove all blacklist words which decrease the quality of the vector
    frequencyVector = filterAgainstBlacklist(frequencyVector, industryCode);    
    
    // This normalization by document count is completely unnecessary
    // but it helps to debug industry vectors so they have values
    // similar in scale to individual documents
    Map<String, Double> returnVector = DocumentVector.generateTFIDFVectorFromTF(frequencyVector);
    int numArticlesInIndustry = documentVectors.size();
    for (Map.Entry<String, Double> entry : returnVector.entrySet()) {
      returnVector.put(entry.getKey(), entry.getValue() / numArticlesInIndustry);
    }
    return returnVector;
  }

  private static Multiset<String> sumVectors(List<DocumentVector> documentVectors) {
    Multiset<String> sum = HashMultiset.create();

    for (DocumentVector document : documentVectors) {
      sum = Multisets.union(sum, document.getFrequencyVector());
    }

    return sum;
  }
  
  private static Multiset<String> filterAgainstBlacklist(Multiset<String> frequencyVector,
      IndustryCode industryCode) throws BiznessException {
    Set<String> blacklist = getBlacklist(industryCode);
    
    Multiset<String> filteredVector = HashMultiset.create();
    for (String word : frequencyVector.elementSet()) {
      if (!blacklist.contains(word)) {
        filteredVector.add(word, frequencyVector.count(word));
      }
    }
    
    return filteredVector;
  }

  private static Map<String, Double> loadVector(IndustryCode industryCode) {
    try {
      return DocumentVector.loadVectorFromFile(
          getFileNameForIndustry(industryCode));
    } catch (BiznessException e) {
      return null;
    }
  }

  private static void saveVector(Map<String, Double> vector, IndustryCode industryCode) 
      throws BiznessException {
    DocumentVector.saveVectorToFile(vector, getFileNameForIndustry(industryCode));
  }

  private static String getFileNameForIndustry(IndustryCode industryCode) {
    return getDirectoryForIndustry(industryCode) + "/industry.vector";
  }
  
  static String getDirectoryForIndustry(IndustryCode industryCode) {
    int industryCodeId = industryCode.getId();
    File rootIndustriesFolder = new File(INDUSTRIES_DIRECTORY);
    String[] industryFolderNames = rootIndustriesFolder.list();
    String industryFolderFullPath = null;
    for (String industryFolderName : industryFolderNames) {
      if (industryFolderName.startsWith(industryCodeId + "-")) {
        industryFolderFullPath = INDUSTRIES_DIRECTORY 
            + "/" + industryFolderName;
      }
    }
    return industryFolderFullPath;
  }

  private static List<String> getSeedWords(IndustryCode industryCode) throws BiznessException {
    List<String> words = null;

    try {
      String seedFileContents = Files.toString(
          new File(getSeedWordsFileName(industryCode)), Charset.defaultCharset());
      words = IOUtils.readLines(new StringReader(seedFileContents));
    } catch (IOException e) {
      throw new BiznessException("Couldn't get seed words from file: " + e.getMessage(), e);
    }

    // remove all comments
    Iterator<String> itr = words.iterator();
    while (itr.hasNext()) {
      String line = itr.next();
      if (line.startsWith("//")) {
        itr.remove();
      }
    }

    return words;
  }

  private static String getSeedWordsFileName(IndustryCode industryCode) {
    return getDirectoryForIndustry(industryCode) + "seed.list";
  }
  
  private static Set<String> getBlacklist(IndustryCode industryCode) throws BiznessException {
    List<String> words = null;
    
    try {
      String blacklistFileContents = Files.toString(
          new File(getBlacklistFileName(industryCode)), Charset.defaultCharset());
      words = IOUtils.readLines(new StringReader(blacklistFileContents));
    } catch (IOException e) {
      throw new BiznessException("Couldn't get blacklist words from file: " + e.getMessage(), e);
    }

    // remove all comments
    // and create Set to return
    Set<String> blacklist = new HashSet<>();
    Iterator<String> itr = words.iterator();
    while (itr.hasNext()) {
      String line = itr.next();
      if (!line.startsWith("//") && line.trim().length() != 0) {
        blacklist.add(line);
      }
    }

    return blacklist;
  }
  
  private static String getBlacklistFileName(IndustryCode industryCode) {
    return getDirectoryForIndustry(industryCode) + "black.list";
  }

  Map<String, Double> getVector() {
    return tfIdfVector;
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
}
