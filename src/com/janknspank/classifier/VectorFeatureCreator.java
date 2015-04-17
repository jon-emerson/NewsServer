package com.janknspank.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.api.client.util.Lists;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.common.TopList;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.crawler.ArticleUrlDetector;
import com.janknspank.crawler.UrlCleaner;
import com.janknspank.crawler.UrlWhitelist;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.CoreProto.IndustryVectorNormalizationData;
import com.janknspank.rank.DistributionBuilder;

/**
 * Generates feature.vector files for Features that are based on TF-IDF vector
 * similarities.
 *
 * Internally, this class reads in a set of seed URLs and seed words from
 * seed.list in the vector's directory, finds Articles that match those, and
 * then builds up word frequency tables from the words in each Article.  The
 * word frequencies are then written to feature.vector as a gzipped proto
 * binary.  Secondly, a large set of documents are scored against the vector
 * to see how they score.  A Distribution is created.  This enables us to see
 * if future articles are in the top 20% of related articles, bottom 50%, etc.
 * This distribution is written to feature.distribution as a gzipped proto
 * also.
 */
public class VectorFeatureCreator {
  private static final Map<Article, Vector> __ARTICLE_VECTOR_MAP = Maps.newHashMap();

  private final FeatureId featureId;

  public VectorFeatureCreator(FeatureId featureId) {
    this.featureId = featureId;
  }

  private static synchronized Map<Article, Vector> getArticleVectorMap()
      throws DatabaseSchemaException {
    if (__ARTICLE_VECTOR_MAP.isEmpty()) {
      for (Article article : Database.with(Article.class).get(
          new QueryOption.Limit(20000),
          new QueryOption.DescendingSort("published_time"))) {
        __ARTICLE_VECTOR_MAP.put(article, Vector.fromArticle(article));
      }
    }
    return __ARTICLE_VECTOR_MAP;
  }

  /**
   * Reads from seed.list and black.list to create a word distribution vector,
   * and then writes said vector and its calculated distribution to
   * feature.vector and feature.distribution.
   */
  public void createVectorAndDistribution()
      throws ClassifierException, DatabaseSchemaException, BiznessException {
    // 1. Get seed words for industryCode.id
    System.out.println("Reading keywords and URLs for " + featureId.getId() + ", \""
        + featureId.getTitle() + "\"...");
    Set<String> seeds = getSeeds();
    System.out.println(seeds.size() + " words/URLs found");

    // 2. Get all documents that contain the seed word
    System.out.println("Reading articles...");
    List<String> words = Lists.newArrayList();
    List<String> urls = Lists.newArrayList();
    for (String seed : seeds) {
      if (seed.startsWith("http://") || seed.startsWith("https://")) {
        if (!UrlWhitelist.isOkay(seed)) {
          System.out.println("Warning: Skipping URL which is not whitelisted: " + seed);
        } else if (!ArticleUrlDetector.isArticle(seed)) {
          System.out.println("Warning: Skipping URL which is not an article: " + seed);
        } else {
          urls.add(UrlCleaner.clean(seed));
        }
      } else {
        words.add(seed);
      }
    }
    Iterable<Article> seedArticles;
    try {
      seedArticles = Iterables.concat(
          ArticleCrawler.getArticles(urls, true /* retain */).values(),
          Articles.getArticlesForKeywordsFuture(words, Article.Reason.INDUSTRY, 1000).get());
    } catch (InterruptedException | ExecutionException e) {
      throw new ClassifierException("Async error: " + e.getMessage(), e);
    }
    System.out.println(Iterables.size(seedArticles) + " articles found");

    // 2.5 Output # articles / seed word - make it easy to prune out
    // empty seed words, or find gaps in the corpus
    printSeedWordOccurrenceCounts(seeds, seedArticles);

    // 3. Convert them into the industry vector
    System.out.println("Calculating vector...");
    Vector vector = new Vector(seedArticles, getBlacklist());

    // 4. Write the industry vector.
    vector.writeToFile(VectorFeature.getVectorFile(featureId));

    // 5. Create and write the normalizer.
    IndustryVectorNormalizationData.Builder normalizationDataBuilder =
        IndustryVectorNormalizationData.newBuilder();
    normalizationDataBuilder.setDistribution(generateDistributionForVector(vector).build());
    normalizationDataBuilder.setSimilarityThreshold10Percent(
        getSimilarityThreshold(vector, seedArticles, 0.1));
    normalizationDataBuilder.setRatioOfArticlesAboveThreshold10Percent(
        getRatioOfArticlesAboveThreshold(
            vector, normalizationDataBuilder.getSimilarityThreshold10Percent()));
    normalizationDataBuilder.setSimilarityThreshold50Percent(
        getSimilarityThreshold(vector, seedArticles, 0.5));
    normalizationDataBuilder.setRatioOfArticlesAboveThreshold50Percent(
        getRatioOfArticlesAboveThreshold(
            vector, normalizationDataBuilder.getSimilarityThreshold50Percent()));
    IndustryVectorNormalizer.writeToFile(
        normalizationDataBuilder.build(), VectorFeature.getNormalizerFile(featureId));
  }

  private DistributionBuilder generateDistributionForVector(Vector vector)
      throws ClassifierException, DatabaseSchemaException {
    DistributionBuilder builder = new DistributionBuilder();
    for (Map.Entry<Article, Vector> articleVectorEntry : getArticleVectorMap().entrySet()) {
      Article article = articleVectorEntry.getKey();
      Vector articleVector = articleVectorEntry.getValue();
      double boost = 0.05 * Feature.getBoost(featureId, article);
      double score = VectorFeature.rawScore(this.featureId, vector, boost, articleVector);
      builder.add(score);
    }
    return builder;
  }

  private double getSimilarityThreshold(
      Vector vector, Iterable<Article> seedArticles, double percentile)
      throws ClassifierException {
    DistributionBuilder seedArticleDistributionBuilder = new DistributionBuilder();
    Vector universeVector = UniverseVector.getInstance();
    TopList<String, Double> bestArticles = new TopList<String, Double>(10);
    TopList<String, Double> worstArticles = new TopList<String, Double>(10);
    for (Article article : seedArticles) {
      Double cosineSimilarity =
          vector.getCosineSimilarity(universeVector, Vector.fromArticle(article));
      seedArticleDistributionBuilder.add(cosineSimilarity);
      bestArticles.add(article.getUrl(), cosineSimilarity);
      worstArticles.add(article.getUrl(), 1 - cosineSimilarity);
    }
    return DistributionBuilder.getValueAtPercentile(
        seedArticleDistributionBuilder.build(), percentile);
  }

  private double getRatioOfArticlesAboveThreshold(Vector vector, double similarityThreshold)
      throws DatabaseSchemaException, ClassifierException {
    Vector universeVector = UniverseVector.getInstance();
    Map<Article, Vector> articleVectorMap = getArticleVectorMap();
    int numArticlesAboveThreshold = 0;
    for (Vector articleVector : articleVectorMap.values()) {
      if (vector.getCosineSimilarity(universeVector, articleVector) >= similarityThreshold) {
        numArticlesAboveThreshold++;
      }
    }
    return ((double) numArticlesAboveThreshold) / articleVectorMap.size();
  }

//  /**
//   * Saves the Feature's vector in a human readable text file for debugging.
//   */
//  private void writeVectorToTextFile(Vector vector) throws ClassifierException {
//    FileOutputStream outputStream = null;
//    try {
//      outputStream = new FileOutputStream(
//          VectorFeature.getVectorFile(featureId).getAbsoluteFile() + ".txt");
//      IOUtils.write("# Text-representation of a feature vector for:\n", outputStream,
//          Charsets.UTF_8);
//      IOUtils.write("# " + featureId.getTitle() + "\n", outputStream, Charsets.UTF_8);
//      IOUtils.write(vector.toVectorData().toString(), outputStream, Charsets.UTF_8);
//    } catch (IOException e) {
//      throw new ClassifierException("Could not write feature text vector: " + e.getMessage(), e);
//    } finally {
//      IOUtils.closeQuietly(outputStream);
//    }
//  }

  /**
   * Returns a Set of words and URLs that should be used for the creation of
   * this feature vector.
   */
  private Set<String> getSeeds() throws ClassifierException {
    File seedWordFile = new File(VectorFeature.getVectorDirectory(featureId), "/seed.list");
    if (!seedWordFile.exists()) {
      throw new ClassifierException(
          "No seed words found for vector feature: " + featureId.getId());
    }
    try {
      return readWords(seedWordFile);
    } catch (IOException e) {
      throw new ClassifierException("Couldn't get seed words from file: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a set of non-empty non-comment strings from the passed file.  The
   * passed file is often a seed words or blacklist words file.
   */
  private static Set<String> readWords(File file) throws IOException {
    BufferedReader br = null;
    Set<String> words = Sets.newHashSet();
    try {
      br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      while (line != null) {
        if (!line.startsWith("//") && line.trim().length() > 0) {
          words.add(line);
        }
        line = br.readLine();
      }
    } finally {
      IOUtils.closeQuietly(br);
    }
    return words;
  }

  private Set<String> getBlacklist() throws ClassifierException {
    File blacklistFile = new File(VectorFeature.getVectorDirectory(featureId), "/black.list");
    if (!blacklistFile.exists()) {
      return ImmutableSet.of();
    }
    try {
      return readWords(blacklistFile);
    } catch (IOException e) {
      throw new ClassifierException("Couldn't get blacklist words from file: " + e.getMessage(), e);
    }
  }

  /**
   * For debugging: Writes out the number of Articles each word in seed.list
   * matched.
   */
  private static void printSeedWordOccurrenceCounts(Set<String> seeds,
      Iterable<Article> articles) {
    Multiset<String> seedOccurrenceCounts = HashMultiset.create();
    for (Article article : articles) {
      Set<String> keywords = new HashSet<>();
      for (ArticleKeyword articleKeyword : article.getKeywordList()) {
        keywords.add(articleKeyword.getKeyword());
      }
      for (String seed : seeds) {
        if (keywords.contains(seed)) {
          seedOccurrenceCounts.add(seed);
        }
      }
    }
    System.out.println("Seed word occurrences in corpus:");
    for (String seed : seeds) {
      System.out.println("  " + seed + ": " + seedOccurrenceCounts.count(seed));
    }
  }

  /**
   * Regenerates a feature with a given ID, or "all".  Feature IDs are specified
   * as integers, passed as command-line parameters to this program.
   */
  public static void main(String args[]) throws Exception {
    // 1. Figure out which features to regenerate from args.
    List<FeatureId> featuresToRegenerate = Lists.newArrayList();
    if (args.length > 0) {
      if (args[0].equals("all")) {
        Iterables.addAll(featuresToRegenerate, VectorFeature.getDefinedFeatureIds());
      } else if (args[0].endsWith("+")) {
        // Allow folks to do "10512+", which regens everything from 10512 and
        // beyond.
        int featureIdId = Integer.parseInt(args[0].substring(0, args[0].length() - 1));
        for (FeatureId featureId : VectorFeature.getDefinedFeatureIds()) {
          if (featureId.getId() >= featureIdId) {
            featuresToRegenerate.add(featureId);
          }
        }
      } else {
        for (String featureIdStr : args) {
          int id = NumberUtils.toInt(featureIdStr, -1);
          if (id != -1) {
            FeatureId featureId = FeatureId.fromId(id);
            if (featureId == null) {
              throw new Error("Feature ID is not defined: " + id);
            }
            featuresToRegenerate.add(featureId);
          }
        }
      }
    }
    Collections.sort(featuresToRegenerate, new Comparator<FeatureId>() {
      @Override
      public int compare(FeatureId featureId1, FeatureId featureId2) {
        return Integer.compare(featureId1.getId(), featureId2.getId());
      }
    });

    // 2. Regenerate vector and distribution files for the specified features.
    for (FeatureId featureId : featuresToRegenerate) {
      new VectorFeatureCreator(featureId).createVectorAndDistribution();
    }

    System.exit(0);
  }
}
