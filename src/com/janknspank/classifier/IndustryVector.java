package com.janknspank.classifier;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.janknspank.data.Articles;
import com.janknspank.data.DataInternalException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.IndustryCode;

public class IndustryVector {
  private Map<String, Double> tfIdfVector;
  private IndustryCode industryCode;
  
  public IndustryVector(IndustryCode code) throws IOException, DataInternalException {
    industryCode = code;
    try {
      tfIdfVector = loadVector(industryCode);
      return;
    } catch (ClassNotFoundException | IOException e) {
      System.out.println("Could not load IndustryVector from file - code: " + 
          industryCode.getId());
      System.out.println("Going to generate it from scratch");
      e.printStackTrace();
      tfIdfVector = generateVectorForIndustryCode(industryCode);
      save(tfIdfVector, industryCode);
    }
  }
  
  private static Map<String, Double> generateVectorForIndustryCode(IndustryCode industryCode) 
      throws DataInternalException, IOException {
    // 1. Get seed words for industryCode.id
    List<String> words = getSeedWords(industryCode);
    
    // 2. Get all documents that contain the seed word
    List<Article> articles = Articles.getArticlesForKeywords(words);
    
    // 3. Convert them into the industry vector
    List<DocumentVector> documentVectors = new ArrayList<>(); 
    for (Article article : articles) {
      documentVectors.add(new DocumentVector(article));
    }
    Map<String, Integer> frequencyVector = sumVectors(documentVectors);
    return DocumentVector.generateTFIDFVectorFromTF(frequencyVector);
  }
  
  private static Map<String, Integer> sumVectors(List<DocumentVector> documentVectors) {
    HashMap<String, Integer> sum = new HashMap<>();
    Integer newSum;
    Integer tf;
    
    for (DocumentVector document : documentVectors) {
      for (Map.Entry<String, Integer> wordFrequency : document.getFrequencyVector().entrySet()) {
        // Get frequency of the word in the document
        String word = wordFrequency.getKey();
        tf = wordFrequency.getValue();
        
        // Add that to the total frequency of the word in the collection
        newSum = sum.get(word);
        if (newSum != null) {
          sum.put(word, newSum + tf);
        }
      }
    }
    
    return sum;
  }
  
  @SuppressWarnings("unchecked")
  private static Map<String, Double> loadVector(IndustryCode industryCode)
      throws IOException, ClassNotFoundException {
    String fileName = getFileNameForIndustry(industryCode);
    System.out.println("loadVector for " + fileName);
    FileInputStream fis = new FileInputStream(
        fileName);
    ObjectInputStream ois = new ObjectInputStream(fis);
    Map<String, Double> map = (Map<String, Double>) ois.readObject();
    ois.close();
    return map;
  }
  
  private static void save(Map<String, Double> vector, IndustryCode industryCode) 
      throws IOException {
    FileOutputStream fos = new FileOutputStream(
        getFileNameForIndustry(industryCode));
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(vector);
    oos.close();
  }
  
  private static String getFileNameForIndustry(IndustryCode industryCode) {
    return "classifier/industryvectors/" + industryCode.getId() + ".vector";
  }
  
  private static List<String> getSeedWords(IndustryCode industryCode) 
      throws IOException {
    String seedFileContents = readFile(getSeedWordsFileName(industryCode),
        Charset.defaultCharset());
    return Arrays.asList(seedFileContents.split("[\\s,;\\n\\t]+"));
  }
  
  private static String getSeedWordsFileName(IndustryCode industryCode) {
    return "classifier/industryseeds/" + industryCode.getId() + ".seedwords";
  }
  
  static String readFile(String path, Charset encoding) 
      throws IOException 
  {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }
  
  Map<String, Double> getVector() {
    return tfIdfVector;
  }
}
