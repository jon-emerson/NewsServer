package com.janknspank.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.janknspank.common.TopList;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.IndustryCodes;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Article.Builder;
import com.janknspank.proto.Core.ArticleIndustryClassification;
import com.janknspank.proto.Core.IndustryCode;

public class Benchmark {
  private static final String ROOT_BENCHMARKS_DIRECTORY = "classifier/benchmarks";
  /**
   * Runs the IndustryClassifier against a set of files
   * @param industryCode
   * @throws DataInternalException 
   * @throws IOException 
   */
  public static void benchmark(int industryCodeId) throws DataInternalException {
    IndustryCode industryCode = IndustryCodes.INDUSTRY_CODE_MAP
        .get(industryCodeId);

    // Load up the good and bad articles
    File rootBenchmarksFolder = new File(ROOT_BENCHMARKS_DIRECTORY);
    String[] industryFolderNames = rootBenchmarksFolder.list();
    Iterable<Article> goodArticles = null;
    Iterable<Article> badArticles = null;
    for (String industryFolderName : industryFolderNames) {
      if (industryFolderName.startsWith(industryCodeId + "-")) {
        String industryFolderFullPath = ROOT_BENCHMARKS_DIRECTORY 
            + "/" + industryFolderName;
        //Load up the good articles
        goodArticles = generateArticlesAtDirectory(industryFolderFullPath + "/Good");

        //Load up the bad articles
        badArticles = generateArticlesAtDirectory(industryFolderFullPath + "/Bad");
      }
    }

    // Compute good article similarity scores
    Map<Article, Double> goodArticleSimilarities = 
        getSimilaritiesForArticles(goodArticles, industryCode);
    
    // Compute bad article similarity scores
    Map<Article, Double> badArticleSimilarities = 
        getSimilaritiesForArticles(badArticles, industryCode);
    
    analyzeClassifications(goodArticleSimilarities, badArticleSimilarities, industryCode);
  }
  
  private static Map<Article, Double> getSimilaritiesForArticles(
      Iterable<Article> articles, IndustryCode industryCode) throws DataInternalException {
    IndustryClassifier classifier = IndustryClassifier.getInstance();
    Map<Article, Double> articleSimilarities = new HashMap<>();

    for (Article article : articles) {
      ArticleIndustryClassification classification = 
          classifier.classifyForIndustry(article, industryCode);
      articleSimilarities.put(article, classification.getSimilarity());
    }

    return articleSimilarities;
  }

  private static void analyzeClassifications(Map<Article, Double> goods, 
      Map<Article, Double> bads, IndustryCode industryCode) {
    int totalArticleCount = goods.size() + bads.size();
    int articlesCount20percent = (int)(totalArticleCount * 0.2);
    double minSimilarity = 1;
    double maxSimilarity = 0;

    // Get the top 20% and bottom 20% the articles
    TopList<Article, Double> topArticles = new TopList<>(articlesCount20percent);
    TopList<Article, Double> bottomArticles = new TopList<>(articlesCount20percent);
    for (Map.Entry<Article, Double> entry : goods.entrySet()) {
      Article article = entry.getKey();
      double score = entry.getValue();
      topArticles.add(article, score);
      bottomArticles.add(article, score * -1);
      if (score > maxSimilarity) {
        maxSimilarity = score;
      }
      if (score < minSimilarity) {
        minSimilarity = score;
      }
    }
    
    // Get bottom 20% cutoff score and top 20% cutoff score
    double range20percent = (maxSimilarity - minSimilarity) * 0.2;
    double bottom20percent = minSimilarity + range20percent;
    double top20percent = maxSimilarity - range20percent;

    // Get % of good articles that are in the top 20% range
    int numGoodInTop20Percent = 0;
    for (Map.Entry<Article, Double> entry : goods.entrySet()) {
      Article article = entry.getKey();
      double score = entry.getValue();
      if (score > top20percent) {
        numGoodInTop20Percent++;
      }
    }
    double percentGoodTop20 = numGoodInTop20Percent / goods.size();
    
    int numBadInBottom20Percent = 0;
    for (Map.Entry<Article, Double> entry : bads.entrySet()) {
      Article article = entry.getKey();
      double score = entry.getValue();
      if (score < bottom20percent) {
        numBadInBottom20Percent++;
      }
    }
    double percentBadBottom20 = numBadInBottom20Percent / bads.size();
    
    System.out.println("Industry " + industryCode.getId() + ": " 
       + industryCode.getDescription());
    System.out.println("% of good articles in the top 20%: " + percentGoodTop20);
    System.out.println("% of bad articles in the bottom 20%: " + percentBadBottom20);
    System.out.println("\n");
    System.out.println("Top 20%:");
    for (Article article : topArticles.getKeys()) {
      String goodOrBad = "G";
      if (bads.containsKey(article)) {
        goodOrBad = "B";
      }
      System.out.println(topArticles.getValue(article) + " - " 
          + goodOrBad + " - " + article.getTitle());
    }
    System.out.println("\n");
    System.out.println("Bottom 20%:");
    for (Article article : bottomArticles.getKeys()) {
      String goodOrBad = "G";
      if (bads.containsKey(article)) {
        goodOrBad = "B";
      }
      System.out.println((-1 * bottomArticles.getValue(article)) + " - " 
          + goodOrBad + " - " + article.getTitle());
    }
  }
  
  private static Iterable<Article> generateArticlesAtDirectory(String directoryPath) 
      throws DataInternalException {
    File articlesDirectory = new File(directoryPath);
    String[] articleFileNames = articlesDirectory.list();
    List<Article> articles = new ArrayList<>();
    for (String articleFileName : articleFileNames) {
      String articleFullPath = directoryPath + "/" + articleFileName;
      try {
        articles.add(generateArticleAtPath(
            new File(articleFullPath)));  
      } catch (IOException e) {
        throw new DataInternalException("Can't generate article from path " 
            + articleFullPath + ": " + e.getMessage(), e);
      }
    }
    return articles;
  }
  
  private static Article generateArticleAtPath(File file) 
      throws IOException {
    Builder articleBuilder = Article.newBuilder()
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
   * Called by benchmark.sh
   * @param args an array of industry code Ids
   * (ex. 6 for Internet) to benchmark
   * @throws DataInternalException 
   * @throws NumberFormatException 
   */
  public static void main(String[] args) 
      throws NumberFormatException, DataInternalException {
    //Args are the industries code ids to benchmark
    for (String arg : args) {
      benchmark(Integer.parseInt(arg));
    }
  }

}
