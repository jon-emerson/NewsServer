package com.janknspank.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.UniverseVector;
import com.janknspank.classifier.Vector;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.rank.Deduper;

public class GetSimilarityThresholdForDupes {
  private static final Map<String, String> dupes;
  static {
    dupes = new HashMap<>();
    dupes.put("http://techcrunch.com/2015/02/10/yelp-gulps-eat24/",
        "http://bits.blogs.nytimes.com/2015/02/10/yelp-buys-eat24-an-online-food-ordering-service-for-134-million/");
    dupes.put("https://recode.net/2015/02/13/obama-says-europes-aggressiveness-towards-google-comes-from-protecting-lesser-competitors/",
        "http://www.theverge.com/2015/2/17/8050691/obama-our-companies-created-the-internet");
    dupes.put("http://bits.blogs.nytimes.com/2015/02/11/microsoft-continues-mobile-push-with-sunrise-acquisition/",
        "http://techcrunch.com/2015/02/04/microsoft-sunrise/");
    dupes.put("http://techcrunch.com/2015/02/18/ubers-series-e-round-surges-to-2-2-billion/",
        "http://dealbook.nytimes.com/2015/02/18/uber-expands-funding-round-by-1-billion/");
    dupes.put("http://www.wired.com/2015/02/facebook-unveils-tool-sharing-data-malicious-botnets/",
        "http://www.slate.com/blogs/future_tense/2015/02/16/facebook_threatexchange_the_tool_for_sharing_data_on_malicious_botnets.single.html");
    dupes.put("http://thenextweb.com/apps/2015/02/20/youtube-kids-launching-february-23/",
        "http://money.cnn.com/2015/02/20/technology/mobile/youtube-for-kids/");
  }

  /**
   * This prints the similarities for articles that are confirmed to be about
   * the same basic event and largely the same.
   */
  public static void printConfirmedDupeSimilarities() throws ClassifierException, BiznessException {
    for (Map.Entry<String, String> entry : dupes.entrySet()) {
      Map<String, Article> articles = ArticleCrawler.getArticles(ImmutableList.of(entry.getKey()));
      Article article1 = articles.get(entry.getKey());
      articles = ArticleCrawler.getArticles(ImmutableList.of(entry.getValue()));
      Article article2 = articles.get(entry.getValue());
      Vector vector1 = Vector.fromArticle(article1);
      Vector vector2 = Vector.fromArticle(article2);
      System.out.println("Similarity: " + vector1.getCosineSimilarity(
          UniverseVector.getInstance(),  vector2));
    }
  }

  public static void printArticlePairsThatHaveSimilaritiesAbove(double thresholdSimilarity) 
      throws DatabaseSchemaException {
    ArrayList<Article> articles = Lists.newArrayList(
        Articles.getArticlesByFeatures(ImmutableList.of(FeatureId.INTERNET), 1000));
    int size = articles.size();
    int duplicatesCount = 0;
    System.out.println("Deduping " + size + " articles");
    for (int i = 0; i < size - 1; i++) {
      for (int j = i + 1; j < size; j++) {
        Article article1 = articles.get(i);
        Article article2 = articles.get(j);
        double similarity = Deduper.similarity(article1, article2);
        if (similarity > thresholdSimilarity) {
          System.out.println("similarity between " + article1.getTitle() 
            + "\n\t& " + article2.getTitle() + "\n\t\t ->" + similarity);
          articles.remove(j);
          j--;
          size--;
          duplicatesCount++;
        }
      }
    }
    System.out.println("Total duplicates found: " + duplicatesCount);
  }

  public static void main(String args[]) throws ClassifierException, 
      BiznessException, DatabaseSchemaException {
    printConfirmedDupeSimilarities();
    printArticlePairsThatHaveSimilaritiesAbove(0.25);
  }
}
