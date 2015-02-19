package com.janknspank.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.janknspank.bizness.BiznessException;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.classifier.UniverseVector;
import com.janknspank.classifier.Vector;
import com.janknspank.common.TopList;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.crawler.ArticleUrlDetector;
import com.janknspank.crawler.UrlWhitelist;
import com.janknspank.proto.ArticleProto.Article;

public class InspectVector {  
  public static void printTFIDFVectorForUrl(String url) 
      throws BiznessException, ClassifierException {
    Article article = ArticleCrawler.getArticles(ImmutableList.of(url)).get(url);
    Vector vector = Vector.fromArticle(article);
    Map<String, Double> tfIdf = vector.getTfIdf(UniverseVector.getInstance());

    TopList<String, Double> words = new TopList<>(1000);
    for (Map.Entry<String, Double> entry : tfIdf.entrySet()) {
      words.add(entry.getKey(), entry.getValue());
    }

    for (String word : words.getKeys()) {
      System.out.println(word + ": " + words.getValue(word));
    }
  }

  /**
   * Called by bin/inspectvector.sh
   * Generates and article object for the passed in Url, and prints
   * out its TFIDF vector to help debug quality issues.
   * @param args a URL to inspect
   */
  public static void main(String args[]) throws BiznessException, ClassifierException {
    if (args.length == 0) {
      System.out.println("Usage: bin/inspectvector.sh http://path/to/url");
      System.exit(-1);
    }
    String urlString = args[0];

    // Validate the URL.
    try {
      new URL(urlString);
    } catch (MalformedURLException e) {
      System.out.println("Bad URL: " + urlString);
      System.exit(-1);
    }
    System.out.println("Processing URL: " + urlString);

    // Check URL whitelist.
    if (!UrlWhitelist.isOkay(urlString)) {
      System.out.println("URL is blacklisted.");
      return;
    }

    // Check Article regexes.
    if (!ArticleUrlDetector.isArticle(urlString)) {
      System.out.println("URL is not an article.");
      return;
    }

    printTFIDFVectorForUrl(urlString);
  }
}
