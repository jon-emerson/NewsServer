package com.janknspank.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.api.client.util.Lists;
import com.google.common.base.Charsets;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.janknspank.ArticleCrawler;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.common.ArticleUrlDetector;
import com.janknspank.common.UrlWhitelist;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.rank.DistributionBuilder;

public abstract class VectorFeature extends Feature {
  protected abstract File getVectorsContainerDirectory();
  private static final List<Vector> __DOCUMENT_VECTORS = Lists.newArrayList();

  public VectorFeature(int id, String description, FeatureType type) {
    super(id, description, type);
  }

  public double getScore(ArticleOrBuilder article) throws ClassifierException {
    Vector vector = getVector();
    Vector articleVector = new Vector(article);
    return vector.getCosineSimilarity(UniverseVector.getInstance(), articleVector);
  }
  
  public void generate() throws ClassifierException, DatabaseSchemaException, BiznessException {
    createVector();
    writeVectorToTextFile();
  }
  
  public Vector getVector() throws ClassifierException {
    return Vector.fromFile(getVectorFile());
  }
  
  public synchronized Distribution getDistribution()
      throws ClassifierException {
    return DistributionBuilder.fromFile(getDistributionFile());
  }
  
  protected File getVectorFile() {
    return new File(getVectorDirectory(), "/feature.vector");
  }
  
  private File getDistributionFile() {
    return new File(getVectorDirectory(), "/feature.distribution");
  }
  
  protected void createVector() 
      throws ClassifierException, DatabaseSchemaException, BiznessException {
    // 1. Get seed words for industryCode.id
    System.out.println("Reading keywords and URLs for " + id + ", \""
        + description + "\"...");
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
          urls.add(seed);
        }
      } else {
        words.add(seed);
      }
    }
    Iterable<Article> articles = Iterables.concat(
        ArticleCrawler.getArticles(urls).values(),
        Articles.getArticlesForKeywords(words));
    System.out.println(Iterables.size(articles) + " articles found");

    // 2.5 Output # articles / seed word - make it easy to prune out
    // empty seed words, or find gaps in the corpus
    printSeedWordOccurrenceCounts(seeds, articles);

    // 3. Convert them into the industry vector
    System.out.println("Calculating vector...");
    Vector vector = new Vector(articles, getBlacklist());

    // 4. Write the industry vector and its effective distribution to disk.
    vector.writeToFile(getVectorFile());
    getDistributionForVector(vector).writeToFile(getDistributionFile());
  }
  
  /**
   * Saves the Feature's vector in a human readable text file
   * for debugging.
   * @throws ClassifierException
   */
  private void writeVectorToTextFile() throws ClassifierException {
    FileOutputStream outputStream = null;
    try {
      outputStream = new FileOutputStream(getVectorFile() + ".txt");
      IOUtils.write("# Text-representation of a feature vector for:\n", outputStream,
                Charsets.UTF_8);
      IOUtils.write("# " + description + "\n", outputStream, Charsets.UTF_8);
      IOUtils.write(getVector().toVectorData().toString(), outputStream,
                Charsets.UTF_8);
    } catch (IOException e) {
      throw new ClassifierException("Could not write feature text vector: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }
  
  private DistributionBuilder getDistributionForVector(Vector vector)
      throws ClassifierException, DatabaseSchemaException {
    DistributionBuilder builder = new DistributionBuilder();
    for (Vector documentVector : getDocumentVectors()) {
      builder.add(vector.getCosineSimilarity(UniverseVector.getInstance(), documentVector));
    }
    return builder;
  }
  
  private static List<Vector> getDocumentVectors() throws DatabaseSchemaException {
    if (__DOCUMENT_VECTORS.isEmpty()) {
      for (Article article : Database.with(Article.class).get(new QueryOption.Limit(5000))) {
        __DOCUMENT_VECTORS.add(new Vector(article));
      }
    }
    return __DOCUMENT_VECTORS;
  }
  
  private File getVectorDirectory() {
    File parentDirectory = getVectorsContainerDirectory();
    for (String idFolderName : parentDirectory.list()) {
      if (idFolderName.startsWith(id + "-")) {
        return new File(parentDirectory + "/" + idFolderName);
      }
    }
    return null;
  }
  
  private File getSeedFile() {
    return new File(getVectorDirectory(), "/seed.list");
  }

  private Set<String> getSeeds() throws ClassifierException {
    File seedWordFile = getSeedFile();
    if (!seedWordFile.exists()) {
      throw new ClassifierException("No seed words found for vector feature: " + id);
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
    File blacklistFile = new File(getVectorDirectory(), "/black.list");
    if (!blacklistFile.exists()) {
      return ImmutableSet.of();
    }
    try {
      return readWords(blacklistFile);
    } catch (IOException e) {
      throw new ClassifierException("Couldn't get blacklist words from file: " + e.getMessage(), e);
    }
  }
  
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
}
