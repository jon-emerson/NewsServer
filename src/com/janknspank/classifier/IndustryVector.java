package com.janknspank.classifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.io.Files;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.EnumsProto.IndustryCode;

public class IndustryVector {
  private Map<String, Double> tfIdfVector;
  private IndustryCode industryCode;

  public IndustryVector(IndustryCode code) throws DatabaseSchemaException, BiznessException {
    industryCode = code;
    tfIdfVector = loadVector(industryCode);
    if (tfIdfVector == null) {
      // Couldn't load IndustryVector from file
      // Try to generate it from scratch
      tfIdfVector = generateVectorForIndustryCode(industryCode);
      save(tfIdfVector, industryCode);
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
    return DocumentVector.generateTFIDFVectorFromTF(frequencyVector);
  }

  private static Multiset<String> sumVectors(List<DocumentVector> documentVectors) {
    Multiset<String> sum = HashMultiset.create();

    for (DocumentVector document : documentVectors) {
      sum = Multisets.union(sum, document.getFrequencyVector());
    }

    return sum;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Double> loadVector(IndustryCode industryCode) {
    try {
      String fileName = getFileNameForIndustry(industryCode);
      FileInputStream fis = new FileInputStream(fileName);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Map<String, Double> map = (Map<String, Double>) ois.readObject();
      ois.close();
    return map;
    } catch (IOException | ClassNotFoundException e) {
      return null;
    }
  }

  private static void save(Map<String, Double> vector, IndustryCode industryCode) throws BiznessException {
    try {
      FileOutputStream fos = new FileOutputStream(
          getFileNameForIndustry(industryCode));
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(vector);
      oos.close();
    } catch (IOException e) {
      throw new BiznessException("Couldn't save industry vector to file: " + e.getMessage(), e);
    }
  }

  private static String getFileNameForIndustry(IndustryCode industryCode) {
    return "classifier/industryvectors/" + industryCode.getId() + ".vector";
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
    return "classifier/industryseeds/" + industryCode.getId() + ".seedwords";
  }

  Map<String, Double> getVector() {
    return tfIdfVector;
  }
}
