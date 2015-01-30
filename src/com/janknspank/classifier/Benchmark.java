package com.janknspank.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.IndustryCodes;
import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleIndustry;
import com.janknspank.proto.EnumsProto.IndustryCode;

public class Benchmark {
  /**
   * Runs the IndustryClassifier against a set of files
   * @param industryCode
   * @throws DataInternalException 
   * @throws IOException 
   */
  public static void benchmark(int industryCodeId) throws BiznessException {
    IndustryCode industryCode = IndustryCodes.INDUSTRY_CODE_MAP
        .get(industryCodeId);

    String industryDirectory = IndustryVector.getDirectoryForIndustry(industryCode);
    Iterable<Article> goodArticles = null;
    Iterable<Article> badArticles = null;
    //Load up the good and bad articles
    goodArticles = generateArticlesFromDirectory(industryDirectory + "/benchmarks/good/articles");
    badArticles = generateArticlesFromDirectory(industryDirectory + "/benchmarks/bad/articles");

    // Compute good article similarity scores
    Map<Article, Double> goodArticleSimilarities = 
        getSimilaritiesForArticles(goodArticles, industryCode);
    
    // Compute bad article similarity scores
    Map<Article, Double> badArticleSimilarities = 
        getSimilaritiesForArticles(badArticles, industryCode);
    
    analyzeClassifications(goodArticleSimilarities, badArticleSimilarities, industryCode);
    
    // Write DocumentVectors to file for quality debugging
    writeArticleVectorsToFile(goodArticles, industryDirectory + "/benchmarks/good/vector_outputs");
    writeArticleVectorsToFile(badArticles, industryDirectory + "/benchmarks/bad/vector_outputs");
    
    //Write the IndustryVector to file for quality debugging
//    String industryPath = "classifier/industryvectors/"
//        + industryCode.getId() + ".topwords";
//    printIndustryVectorToFile(industryCode, industryPath);
  }
  
  private static Map<Article, Double> getSimilaritiesForArticles(
      Iterable<Article> articles, IndustryCode industryCode) throws BiznessException {
    IndustryClassifier classifier = IndustryClassifier.getInstance();
    Map<Article, Double> articleSimilarities = new HashMap<>();

    for (Article article : articles) {
      ArticleIndustry classification = 
          classifier.classifyForIndustry(article, industryCode);
      articleSimilarities.put(article, classification.getSimilarity());
    }

    return articleSimilarities;
  }

  private static void analyzeClassifications(Map<Article, Double> goods, 
      Map<Article, Double> bads, IndustryCode industryCode) {
    int totalArticleCount = goods.size() + bads.size();
//    int articlesCount25percent = (int)(Math.ceil(totalArticleCount * 0.25));
    double minSimilarity = 1;
    double maxSimilarity = 0;

    // Get the top 25% and bottom 25% the articles
    TopList<Article, Double> topArticles = new TopList<>(totalArticleCount);
//    TopList<Article, Double> bottomArticles = new TopList<>(articlesCount25percent);
    for (Map.Entry<Article, Double> entry : goods.entrySet()) {
      Article article = entry.getKey();
      double score = entry.getValue();
      topArticles.add(article, score);
//      bottomArticles.add(article, score * -1);
      if (score > maxSimilarity) {
        maxSimilarity = score;
      }
      if (score < minSimilarity) {
        minSimilarity = score;
      }
    }
    for (Map.Entry<Article, Double> entry : bads.entrySet()) {
      Article article = entry.getKey();
      double score = entry.getValue();
      topArticles.add(article, score);
//      bottomArticles.add(article, score * -1);
      if (score > maxSimilarity) {
        maxSimilarity = score;
      }
      if (score < minSimilarity) {
        minSimilarity = score;
      }
    }
    
    // Get bottom 25% cutoff score and top 20% cutoff score
    double range20percent = (maxSimilarity - minSimilarity) * 0.25;
    double bottom20percent = minSimilarity + range20percent;
    double top20percent = maxSimilarity - range20percent;

    // Get % of good articles that are in the top 20% range
    int numGoodInTop20Percent = 0;
    for (Map.Entry<Article, Double> entry : goods.entrySet()) {
      if (entry.getValue() > top20percent) {
        numGoodInTop20Percent++;
      }
    }
    double percentGoodTop25 = (double)numGoodInTop20Percent / (double)goods.size();
    
    int numBadInBottom20Percent = 0;
    for (Map.Entry<Article, Double> entry : bads.entrySet()) {
      if (entry.getValue() < bottom20percent) {
        numBadInBottom20Percent++;
      }
    }
    double percentBadBottom25 = (double)numBadInBottom20Percent / (double)bads.size();
    
    System.out.println("Industry " + industryCode.getId() + ": " 
       + industryCode.getDescription());
    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(2);
    System.out.println(df.format(percentGoodTop25) + "% of good articles score in the top quartile");
    System.out.println(df.format(percentBadBottom25) + "% of bad articles in the bottom quartile");
    System.out.println("\nRanked list:");
    for (Article article : topArticles.getKeys()) {
      String goodOrBad = "G";
      if (bads.containsKey(article)) {
        goodOrBad = "B";
      }
      System.out.println(topArticles.getValue(article) + " - " 
          + goodOrBad + " - " + article.getTitle());
    }
  }
  
  private static Iterable<Article> generateArticlesFromDirectory(String directoryPath) 
      throws BiznessException {
    File articlesDirectory = new File(directoryPath);
    String[] articleFileNames = articlesDirectory.list();
    List<Article> articles = new ArrayList<>();
    for (String articleFileName : articleFileNames) {
      String articleFullPath = directoryPath + "/" + articleFileName;
      try {
        articles.add(generateArticleFromPath(
            new File(articleFullPath)));  
      } catch (IOException e) {
        throw new BiznessException("Can't generate article from path " 
            + articleFullPath + ": " + e.getMessage(), e);
      }
    }
    return articles;
  }
  
  private static Article generateArticleFromPath(File file) 
      throws IOException {
    Article.Builder articleBuilder = Article.newBuilder()
        .setTitle(file.getName());
    BufferedReader br = new BufferedReader(new FileReader(file));
    try {
      String line = br.readLine();

      while (line != null) {
        articleBuilder.addParagraph(line);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
    return articleBuilder.build();
  }
  
  /**
   * For debugging each industry vector
   */
//  private static void printIndustryVectorToFile(IndustryCode industryCode, String path) {
//    IndustryClassifier classifier = IndustryClassifier.getInstance();
//    IndustryVector industryVector = classifier.getIndustryVector(industryCode);
//    File file = new File(path);
//    FileWriter writer = null;
//    PrintWriter out = null;
//    try {
//      writer = new FileWriter(file, true);
//      out = new PrintWriter(writer);
//      out.println(industryVector);
//    } catch (IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } finally {
//      out.close();
//    }
//  }
  
  private static void writeArticleVectorsToFile(Iterable<Article> articles, String path) 
      throws BiznessException {
    for (Article article : articles) {
      DocumentVector documentVector = new DocumentVector(article);
      DocumentVector.saveVectorToFile(documentVector.getTFIDFVector(), 
          path + "/" + article.getTitle() + ".vector");
    }
  }
  
  /**
   * For debugging each document vector
   */
//  private static void printDocumentVectorToFile(DocumentVector vector, String path) {
//    File file = new File(path);
//    FileWriter writer = null;
//    PrintWriter out = null;
//    try {
//      writer = new FileWriter(file, true);
//      out = new PrintWriter(writer);
//      out.println(vector);
//    } catch (IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } finally {
//      out.close();
//    }
//  }
  
  
  /**
   * Called by benchmark.sh
   * @param args an array of industry code Ids
   * (ex. 6 for Internet) to benchmark
   * @throws DataInternalException 
   * @throws NumberFormatException 
   */
  public static void main(String[] args) 
      throws NumberFormatException, BiznessException {
    //Args are the industries code ids to benchmark
    for (String arg : args) {
      benchmark(Integer.parseInt(arg));
    }
  }

}
