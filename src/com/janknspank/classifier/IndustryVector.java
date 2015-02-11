package com.janknspank.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.api.client.util.Lists;
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
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.proto.EnumsProto.IndustryCode;
import com.janknspank.rank.DistributionBuilder;

public class IndustryVector {
  private static final File INDUSTRIES_DIRECTORY = new File("classifier/industry");
  private static final Pattern INDUSTRY_SUBDIRECTORY_PATTERN = Pattern.compile("([0-9]+)-.*");
  private static final List<Vector> __DOCUMENT_VECTORS = Lists.newArrayList();

  public static synchronized Vector get(int industryCodeId) throws ClassifierException {
    return get(IndustryCodes.getFromIndustryCodeId(industryCodeId));
  }

  public static synchronized Distribution getDistribution(IndustryCode industryCode)
      throws ClassifierException {
    return DistributionBuilder.fromFile(getDistributionFileForIndustry(industryCode));
  }

  public static Vector get(IndustryCode industryCode) throws ClassifierException {
    return Vector.fromFile(getVectorFileForIndustry(industryCode));
  }

  private static List<Vector> getDocumentVectors() throws DatabaseSchemaException {
    if (__DOCUMENT_VECTORS.isEmpty()) {
      for (Article article : Database.with(Article.class).get(new QueryOption.Limit(5000))) {
        __DOCUMENT_VECTORS.add(new Vector(article));
      }
    }
    return __DOCUMENT_VECTORS;
  }

  private static void createVectorForIndustryCode(IndustryCode industryCode)
      throws ClassifierException, BiznessException, DatabaseSchemaException {
    // 1. Get seed words for industryCode.id
    System.out.println("Reading keywords and URLs for " + industryCode.getId() + ", \""
        + industryCode.getDescription() + "\"...");
    Set<String> seeds = getSeeds(industryCode);
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
    Vector vector = new Vector(articles, getBlacklist(industryCode));

    // 4. Write the industry vector and its effective distribution to disk.
    vector.writeToFile(getVectorFileForIndustry(industryCode));
    getDistributionForVector(vector).writeToFile(getDistributionFileForIndustry(industryCode));
  }

  private static DistributionBuilder getDistributionForVector(Vector industryVector)
      throws ClassifierException, DatabaseSchemaException {
    DistributionBuilder builder = new DistributionBuilder();
    for (Vector documentVector : getDocumentVectors()) {
      builder.add(industryVector.getCosineSimilarity(UniverseVector.getInstance(), documentVector));
    }
    return builder;
  }

  private static File getVectorFileForIndustry(IndustryCode industryCode) {
    return new File(getDirectoryForIndustry(industryCode), "/industry.vector");
  }

  private static File getDistributionFileForIndustry(IndustryCode industryCode) {
    return new File(getDirectoryForIndustry(industryCode), "/industry.distribution");
  }

  static File getDirectoryForIndustry(IndustryCode industryCode) {
    int industryCodeId = industryCode.getId();
    for (String industryFolderName : INDUSTRIES_DIRECTORY.list()) {
      if (industryFolderName.startsWith(industryCodeId + "-")) {
        return new File(INDUSTRIES_DIRECTORY + "/" + industryFolderName);
      }
    }
    return null;
  }

  private static File getSeedFileForIndustry(IndustryCode industryCode) {
    return new File(getDirectoryForIndustry(industryCode), "/seed.list");
  }

  private static Set<String> getSeeds(IndustryCode industryCode) throws ClassifierException {
    File seedWordFile = getSeedFileForIndustry(industryCode);
    if (!seedWordFile.exists()) {
      throw new ClassifierException("No seed words found for industry: " + industryCode.getId());
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
    Set<String> blacklist = Sets.newHashSet();
    try {
      br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      while (line != null) {
        if (!line.startsWith("//") && line.trim().length() > 0) {
          blacklist.add(line);
        }
        line = br.readLine();
      }
    } finally {
      IOUtils.closeQuietly(br);
    }
    return blacklist;
  }

  private static Set<String> getBlacklist(IndustryCode industryCode) throws ClassifierException {
    File blacklistFile = new File(getDirectoryForIndustry(industryCode), "/black.list");
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

  /**
   * Regenerates the vector files for given industry IDs, or "all".  Industry
   * IDs are specified as integers, passed as command-line parameters to this
   * program.
   */
  public static void main(String args[]) throws Exception {
    List<IndustryCode> codes = Lists.newArrayList();

    // If the first argument is "all", treat it as if all the industries were
    // specified.
    if (args.length > 0 && args[0].equals("all")) {
      List<String> newArgs = Lists.newArrayList();
      for (File directory : INDUSTRIES_DIRECTORY.listFiles()) {
        Matcher matcher = INDUSTRY_SUBDIRECTORY_PATTERN.matcher(directory.getName());
        if (matcher.matches()) {
          newArgs.add(matcher.group(1));
        }
      }
      args = newArgs.toArray(new String[0]);
    }

    for (String industryCodeId : args) {
      IndustryCode code = IndustryCodes.getFromIndustryCodeId(NumberUtils.toInt(industryCodeId, -1));
      if (code == null) {
        System.out.println("Invalid industry code: " + code);
        System.exit(-1);
      }
      if (!getDirectoryForIndustry(code).exists()) {
        System.out.println("Skipping industry (seed data does not exist): " + code);
      }
      codes.add(code);
    }

    for (IndustryCode code : codes) {
      createVectorForIndustryCode(code);
    }
  }
}
