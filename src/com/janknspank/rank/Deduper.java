package com.janknspank.rank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.classifier.UniverseVector;
import com.janknspank.classifier.Vector;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.ArticleProto.DuplicateArticle;
import com.janknspank.proto.ArticleProto.SocialEngagement;

public class Deduper {
  public static final double DUPLICATE_SIMILARITY_THRESHOLD = 0.5;
  
  /**
   * This method filters out duplicates based on the DuplicateArticles
   * that are written on Article objects at crawl time. This is more efficient
   * than computing duplicates from scratch using cosine similarity. This
   * method should be used during getArticles, not the "dedupe" method below.
   */
  public static Iterable<Article> filterOutDupes(Iterable<Article> articles) {
    HashMap<String, Article> urlIdsMap = new HashMap<>();
    Set<Article> dedupedArticles = new HashSet<>();

    for (Article article : articles) {
      urlIdsMap.put(article.getUrlId(), article);
    }

    for (Article article : articles) {
      List<DuplicateArticle> dupes = article.getDuplicateList();
      SocialEngagement articleEngagement = article.getSocialEngagement(
          article.getSocialEngagementCount() - 1);
      double highestSocialScore = articleEngagement.getShareScore();
      Article mostValuableDupe = article;
      for (DuplicateArticle dupe : dupes) {
        Article dupeArticle = urlIdsMap.get(dupe.getUrlId());
        if (dupeArticle != null) {
          // There's a dupe! Take the one with the largest social score
          SocialEngagement dupeEngagement = dupeArticle.getSocialEngagement(
              dupeArticle.getSocialEngagementCount() - 1);
          if (dupeEngagement.getShareScore() > highestSocialScore) {
            highestSocialScore = dupeEngagement.getShareScore();
            mostValuableDupe = dupeArticle;
          }
          else {
            // Given that dupes are not bidirectional (ie. one article may have
            // a number of DuplicateArticles, but the dupes may not), we need
            // to ensure that dupes that were added earlier in the loop are
            // not kept around.
            dedupedArticles.remove(dupeArticle);
          }
        }
      }

      dedupedArticles.add(mostValuableDupe);
    }

    return dedupedArticles;
  }
  
  /**
   * NOTE: computationally expensive
   * Computes which articles are duplicates of each other and removes
   * them from the list of articles that is returned. Dupes are detected
   * based on their cosine similarity.
   */
  public static Iterable<Article> dedupe(Iterable<Article> articles) {
    ArrayList<Article> dedupedArticles = Lists.newArrayList(articles);
    int size = dedupedArticles.size();
    HashMap<Article, Article> duplicatesMap = new HashMap<>();
    System.out.println("Deduping " + size + " articles");
    for (int i = 0; i < size - 1; i++) {
      for (int j = i + 1; j < size; j++) {
        if (areDupes(dedupedArticles.get(i), dedupedArticles.get(j))) {
          duplicatesMap.put(dedupedArticles.get(i), dedupedArticles.get(j));
          dedupedArticles.remove(j);
          j--;
          size--;
        }
      }
    }

    System.out.println("found " + duplicatesMap.size() + " duplicate articles");
    for (Map.Entry<Article, Article> entry: duplicatesMap.entrySet()) {
      Article dupe1 = entry.getKey();
      Article dupe2 = entry.getValue();
      System.out.println("\t" + dupe1.getTitle() + "\t\t" + dupe1.getUrl() 
          + "\n\t& " + dupe2.getTitle() + "\t\t" + dupe2.getUrl());
    }

    return dedupedArticles;
  }

  /**
   * Test whether two articles are duplicates based on their cosine similarity
   */
  public static boolean areDupes(ArticleOrBuilder article1, ArticleOrBuilder article2) {
    double similarity = similarity(article1, article2);
    if (similarity > DUPLICATE_SIMILARITY_THRESHOLD) {
      return true;
    }
    return false;
  }

  public static double similarity(ArticleOrBuilder article1, ArticleOrBuilder article2) {
    Vector vector1 = Vector.fromArticle(article1);
    Vector vector2 = Vector.fromArticle(article2);
    try {
      return vector1.getCosineSimilarity(UniverseVector.getInstance(), vector2);
    } catch (ClassifierException e) {
      e.printStackTrace();
      return 0;
    }
  }
}
