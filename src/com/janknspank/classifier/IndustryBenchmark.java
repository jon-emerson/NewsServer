package com.janknspank.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.Article;

public class IndustryBenchmark {
  /**
   * Runs the IndustryClassifier against a set of files.
   * @param industryCode
   * @throws DataInternalException
   * @throws IOException
   */
  public static void benchmark(int rawFeatureId) throws ClassifierException {
    FeatureId featureId = FeatureId.fromId(rawFeatureId);
    // Currently this only works for VectorFeatures, because only VectorFeatures
    // have directories to put benchmark files in.  We can hopefully improve on
    // this in the future, but for now, it is what it is.
    VectorFeature feature = (VectorFeature) Feature.getFeature(featureId);

    // Load up the good and bad articles.
    File industryDirectory = VectorFeature.getVectorDirectory(featureId);
    Iterable<Article> goodArticles = null;
    Iterable<Article> badArticles = null;
    goodArticles = generateArticlesFromDirectory(industryDirectory + "/benchmarks/good/articles");
    badArticles = generateArticlesFromDirectory(industryDirectory + "/benchmarks/bad/articles");

    // Compute good article similarity scores.
    Map<Article, Double> goodArticleSimilarities =
        getSimilaritiesForArticles(goodArticles, feature);

    // Compute bad article similarity scores.
    Map<Article, Double> badArticleSimilarities =
        getSimilaritiesForArticles(badArticles, feature);

    analyzeClassifications(goodArticleSimilarities, badArticleSimilarities, feature);

    // Write DocumentVectors to file for quality debugging
    writeArticleVectorsToFile(goodArticles, industryDirectory + "/benchmarks/good/vector_outputs");
    writeArticleVectorsToFile(badArticles, industryDirectory + "/benchmarks/bad/vector_outputs");

    //Write the IndustryVector to file for quality debugging
//    String industryPath = "classifier/industryvectors/"
//        + industryCode.getId() + ".topwords";
//    printIndustryVectorToFile(industryCode, industryPath);
  }

  private static Map<Article, Double> getSimilaritiesForArticles(
      Iterable<Article> articles, Feature feature) throws ClassifierException {
    Map<Article, Double> articleSimilarities = new HashMap<>();
    for (Article article : articles) {
      articleSimilarities.put(article, feature.score(article));
    }
    return articleSimilarities;
  }

  private static void analyzeClassifications(
      Map<Article, Double> goods, Map<Article, Double> bads, Feature feature) {
//    int articlesCount25percent = (int)(Math.ceil(totalArticleCount * 0.25));
    double minSimilarity = Double.MAX_VALUE;
    double maxSimilarity = Double.MIN_VALUE;

    // Get the top 25% and bottom 25% the articles
    TopList<Article, Double> topArticles = new TopList<>(goods.size() + bads.size());
//    TopList<Article, Double> bottomArticles = new TopList<>(articlesCount25percent);
    for (Map.Entry<Article, Double> entry : goods.entrySet()) {
      double score = entry.getValue();
      topArticles.add(entry.getKey(), score);
//      bottomArticles.add(entry.getKey(), score * -1);
      maxSimilarity = Math.max(maxSimilarity, score);
      minSimilarity = Math.min(minSimilarity, score);
    }
    for (Map.Entry<Article, Double> entry : bads.entrySet()) {
      double score = entry.getValue();
      topArticles.add(entry.getKey(), score);
//      bottomArticles.add(entry.getKey(), score * -1);
      maxSimilarity = Math.max(maxSimilarity, score);
      minSimilarity = Math.min(minSimilarity, score);
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
    double percentGoodTop25 = (double) numGoodInTop20Percent / (double) goods.size();

    int numBadInBottom20Percent = 0;
    for (Map.Entry<Article, Double> entry : bads.entrySet()) {
      if (entry.getValue() < bottom20percent) {
        numBadInBottom20Percent++;
      }
    }
    double percentBadBottom25 = (double) numBadInBottom20Percent / (double) bads.size();

    System.out.println("Feature " + feature.getFeatureId());
    System.out.println((int) (percentGoodTop25 * 100)
        + "% of good articles score in the top quartile");
    System.out.println((int) (percentBadBottom25 * 100)
        + "% of bad articles in the bottom quartile\n");
    System.out.println("Ranked list:");
    for (Article article : topArticles.getKeys()) {
      String goodOrBad = bads.containsKey(article) ? "B" : "G";
      System.out.println(topArticles.getValue(article) + " - "
          + goodOrBad + " - " + article.getTitle());
    }
  }

  private static Iterable<Article> generateArticlesFromDirectory(String directoryPath)
      throws ClassifierException {
    File articlesDirectory = new File(directoryPath);
    String[] articleFileNames = articlesDirectory.list();
    List<Article> articles = new ArrayList<>();
    for (String articleFileName : articleFileNames) {
      String articleFullPath = directoryPath + "/" + articleFileName;
      try {
        articles.add(generateArticleFromPath(new File(articleFullPath)));
      } catch (IOException e) {
        throw new ClassifierException("Can't generate article from path "
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
        if (line.trim().length() > 0) {
          articleBuilder.addParagraph(line);
        }
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

  /**
   * Returns a decent filename for storing information about the passed article.
   * An extension is not added - that's for the caller to do based on what's
   * being stored.
   */
  private static String getFilename(Article article) {
    return article.getTitle()
        .toLowerCase()
        .replaceAll("[^A-Z0-9a-z]+", " ")
        .trim()
        .replaceAll("\\s+", "-");
  }

  /**
   * This writes a human-readable text vector about each article to disk, so
   * that we can see what article vectors look like and why their respective
   * articles perform the way they do in other parts of the system.
   */
  private static void writeArticleVectorsToFile(Iterable<Article> articles, String path)
      throws ClassifierException {
    FileOutputStream outputStream = null;
    try {
      new File(path).mkdir();
      for (Article article : articles) {
        outputStream = new FileOutputStream(path + "/" + getFilename(article) + ".vectortxt");
            IOUtils.write("# Text-representation of an article vector for:\n", outputStream,
                Charsets.UTF_8);
            IOUtils.write("# " + article.getUrl() + "\n", outputStream, Charsets.UTF_8);
            IOUtils.write(Vector.fromArticle(article).toVectorData().toString(),
                outputStream, Charsets.UTF_8);
      }
    } catch (IOException e) {
      throw new ClassifierException("Could not write article text vector: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
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
   * Called by benchmark.sh.
   * @param args an array of industry code Ids (ex. 6 for Internet) to benchmark
   */
  public static void main(String[] args) throws Exception {
    // Args are the industries code ids to benchmark
    if (args.length == 0) {
      System.out.println("ERROR: You must pass industry IDs as parameters.");
      System.out.println("Example: ./benchmark.sh 6");
      System.exit(-1);
    }
    for (String arg : args) {
      benchmark(Integer.parseInt(arg));
    }
  }
}
