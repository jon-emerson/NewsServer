package com.janknspank.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.google.api.client.util.Maps;
import com.google.api.client.util.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import com.janknspank.common.Asserts;
import com.janknspank.interpreter.KeywordFinder;
import com.janknspank.interpreter.KeywordUtils;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.CoreProto.VectorData;
import com.janknspank.proto.CoreProto.VectorData.WordFrequency;

/**
 * Represents a word frequency vector, either for a document, industry, or the
 * entire universe of documents.  Provides functions for finding similarities
 * between this vector and other vectors.  Also provides disk serialization and
 * deserialization functionality.
 */
public class Vector {
  private static final LoadingCache<ArticleOrBuilder, Vector> ARTICLE_VECTOR_CACHE =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build(
              new CacheLoader<ArticleOrBuilder, Vector>() {
                 public Vector load(ArticleOrBuilder article) {
                   return new Vector(article);
                 }
              });
  private static final ImmutableSet<String> STOP_WORDS;
  private static final KeywordFinder KEYWORD_FINDER = KeywordFinder.getInstance();

  private final int documentCount;

  /**
   * This is the total number of times we saw this word across all documents
   * added to this vector.
   */
  private final Multiset<String> wordFrequencyMap = HashMultiset.create();

  /**
   * This is the number of documents we saw the given word across all documents.
   * If a word lives in a document multiple times, here it is only counted once.
   */
  private final Multiset<String> numDocumentOccurencesMap = HashMultiset.create();

  /**
   * A map of this document Vector's TF-IDF versus a specific universe Vector.
   * Typically there will only be 1 universe.
   */
  private final Map<Vector, Map<String, Double>> tfIdfAgainstUniverseMap =
      Maps.newHashMap();

  static {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader("neuralnet/english_stopwords"));
      ImmutableSet.Builder<String> stopWordSetBuilder = ImmutableSet.builder();
      String stopWord = br.readLine();
      while (stopWord != null) {
        stopWordSetBuilder.add(stopWord);
        stopWord = br.readLine();
      }
      STOP_WORDS = stopWordSetBuilder.build();
    } catch (IOException e) {
      throw new Error("Can't read stopwords file.");
    } finally {
      IOUtils.closeQuietly(br);
    }
  }

  public Vector(VectorData data) {
    documentCount = data.hasDocumentCount() ? data.getDocumentCount() : 1;
    for (int i = 0; i < data.getWordFrequencyCount(); i++) {
      WordFrequency w = data.getWordFrequency(i);
      wordFrequencyMap.add(w.getWord(), w.getFrequency());
      numDocumentOccurencesMap.add(w.getWord(), w.getDocumentOccurences());
    }
  }

  /**
   * Constructs a document frequency vector for a specific Article.
   */
  private <T extends ArticleOrBuilder> Vector(T article) {
    this(ImmutableList.of(article), ImmutableSet.<String>of());
  }

  /**
   * Returns a Vector for the word occurrences in the passed article.  Vectors
   * are cached for 1 minute, to allow this method to be called multiple times
   * efficiently.
   */
  public static Vector fromArticle(ArticleOrBuilder article) {
    try {
      return ARTICLE_VECTOR_CACHE.get(article);
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause());
      throw new RuntimeException(e);
    }
  }

  /**
   * Constructs a frequency vector for a set of Articles.  Optionally you can
   * pass in additional stop word sets that will not be included in the
   * resulting Vector.
   */
  @SafeVarargs
  public <T extends ArticleOrBuilder> Vector(
      Iterable<T> articles, Set<String>... additionalStopWords) {
    documentCount = Iterables.size(articles);

    // Handle additional stop words, as necessary.
    Set<String> stopWords = STOP_WORDS;
    if (additionalStopWords.length > 0) {
      stopWords = Sets.newHashSet(STOP_WORDS);
      for (Set<String> additionalStopWordSet : additionalStopWords) {
        stopWords.addAll(additionalStopWordSet);
      }
    }

    Set<String> words = Sets.newHashSet();
    for (ArticleOrBuilder article : articles) {
      Set<String> articleWords = Sets.newHashSet();
      for (String paragraph : article.getParagraphList()) {
        String[] additionalPhrases = new String[] {
            article.getTitle(),
            article.getDescription(),
            article.getAuthor()
        };
        for (String sentence : ObjectArrays.concat(
            KEYWORD_FINDER.getSentences(paragraph), additionalPhrases, String.class)) {
          for (String word : KEYWORD_FINDER.getTokens(sentence)) {
            if (word != null && !stopWords.contains(word)) {
              word = KeywordUtils.cleanKeyword(word);
              if (word.length() > 2 && KeywordUtils.isValidKeyword(word)) {
                words.add(word);
                articleWords.add(word);
                if (!stopWords.contains(word.toLowerCase())) {
                  wordFrequencyMap.add(word);
                }
              }
            }
          }
        }
      }
      for (String word : articleWords) {
        numDocumentOccurencesMap.add(word);
      }
      articleWords.clear();
    }
  }

  public static Vector fromFile(File vectorFile) throws ClassifierException {
    InputStream inputStream = null;
    try {
      inputStream = new GZIPInputStream(new FileInputStream(vectorFile));
      return new Vector(VectorData.parseFrom(inputStream));
    } catch (IOException e) {
      throw new ClassifierException("Could not read file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public void writeToFile(File vectorFile) throws ClassifierException {
    OutputStream outputStream = null;
    try {
      outputStream = new GZIPOutputStream(new FileOutputStream(vectorFile));
      toVectorData().writeTo(outputStream);
    } catch (IOException e) {
      throw new ClassifierException("Could not write file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }

  public VectorData toVectorData() {
    VectorData.Builder builder = VectorData.newBuilder();
    builder.setDocumentCount(documentCount);
    for (String word : wordFrequencyMap.elementSet()) {
      builder.addWordFrequency(WordFrequency.newBuilder()
          .setWord(word)
          .setFrequency(wordFrequencyMap.count(word))
          .setDocumentOccurences(numDocumentOccurencesMap.count(word)));
    }
    return builder.build();
  }

  /**
   * Returns the cosine similarity between this vector and a different vector.
   * NOTE(tomch): This function normalizes by length of each document, so it's
   * not necessary to normalize by length elsewhere.
   */
  public double getCosineSimilarity(Vector universeVector, Vector v2) {
    Asserts.assertTrue(this.documentCount <= universeVector.documentCount,
        "This Vector is bigger than the universe - This can't be true!",
        IllegalStateException.class);
    Asserts.assertTrue(v2.documentCount <= universeVector.documentCount,
        "Comparison Vector is bigger than the universe - This can't be true!",
        IllegalStateException.class);

    Map<String, Double> tfIdf1 = getTfIdf(universeVector);
    Map<String, Double> tfIdf2 = v2.getTfIdf(universeVector);
    double dotProduct = 0;
    for (String k : Sets.intersection(tfIdf1.keySet(), tfIdf2.keySet())) {
      dotProduct += tfIdf1.get(k) * tfIdf2.get(k);
    }

    double normalizedLength1 = 0;
    for (String k : tfIdf1.keySet()) {
      normalizedLength1 += Math.pow(tfIdf1.get(k), 2);
    }

    double normalizedLength2 = 0;
    for (String k : tfIdf2.keySet()) {
      normalizedLength2 += Math.pow(tfIdf2.get(k), 2);
    }

    return dotProduct / (Math.sqrt(normalizedLength1) * Math.sqrt(normalizedLength2));
  }

  /**
   * For each non-stopword word in this vector, returns a Tf-Idf score for this
   * document versus all documents in the universe.
   * @param universeVector a vector representing word frequencies in the entire
   *     set of documents we've parsed
   */
  public Map<String, Double> getTfIdf(Vector universeVector) {
    if (tfIdfAgainstUniverseMap.containsKey(universeVector)) {
      return tfIdfAgainstUniverseMap.get(universeVector);
    }

    Map<String, Double> tfIdfVector = new HashMap<>();
    for (Multiset.Entry<String> wordFrequency : wordFrequencyMap.entrySet()) {
      String word = wordFrequency.getElement();
      double tf = wordFrequency.getCount();
      double idf = Math.log(universeVector.documentCount /
          (0.000001 + universeVector.numDocumentOccurencesMap.count(word)));
      tfIdfVector.put(word, tf * idf);
    }
    tfIdfAgainstUniverseMap.put(universeVector, tfIdfVector);
    return tfIdfVector;
  }

  public int getDocumentCount() {
    return documentCount;
  }

  public int getWordFrequency(String word) {
    return wordFrequencyMap.count(word);
  }

  public int getDocumentOccurences(String word) {
    return numDocumentOccurencesMap.count(word);
  }
}
