package com.janknspank.bizness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.janknspank.classifier.Deduper;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.TrainedArticleIndustry;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.Scorer;

/**
 * Helper class that manages storing and retrieving Article objects from the
 * database.
 */
public class Articles {
  /**
   * Gets articles that contain a set of keywords.
   */
  public static Iterable<Article> getArticlesForKeywords(Iterable<String> keywords, int limit)
      throws DatabaseSchemaException {
    return Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.WhereEquals("keyword.keyword", keywords),
        new QueryOption.Limit(limit));
  }

  public static Iterable<Article> getRankedArticles(User user, Scorer scorer, int limit)
      throws DatabaseSchemaException {
    Map<Article, Double> ranks = getArticlesAndScores(user, scorer, limit * 5);

    // Sort the articles
    TopList<Article, Double> articles = new TopList<>(ranks.size());
    for (Map.Entry<Article, Double> entry : ranks.entrySet()) {
      articles.add(entry.getKey(), entry.getValue());
    }

    List<Article> topArticles = articles.getKeys();
    return topArticles.subList(0, Math.min(topArticles.size(), limit));
  }

  /**
   * Used by ViewFeedServlet to show a set of Articles and their corresponding
   * scores.
   */
  public static Map<Article, Double> getArticlesAndScores(User user, Scorer scorer, int limit)
      throws DatabaseSchemaException {
    // TODO: replace this with getArticles(UserIndustries.getIndustries(userId))
    Iterable<Article> articles = getArticlesByInterest(user.getInterestList(), limit);
    Iterable<Article> dedupedArticles = Deduper.filterOutDupes(articles);
    Map<Article, Double> ranks = new HashMap<>();
    for (Article article : dedupedArticles) {
      ranks.put(article, scorer.getScore(user, article));
    }
    return ranks;
  }
  
  /**
   * Used for dupe detection threshold testing
   * @param features
   * @return
   */
  public static Iterable<Article> getArticlesByFeatures(Iterable<FeatureId> featureIds)
      throws DatabaseSchemaException{
    List<Number> featureNumbers = new ArrayList<>();
    for (FeatureId featureId : featureIds) {
      featureNumbers.add((Number)featureId.getId());
    }

    return Database.with(Article.class).get(
        new QueryOption.WhereEqualsNumber("feature.feature_id", featureNumbers),
        new QueryOption.Limit(1000));
  }

  /**
   * Gets a list of articles tailored specifically to the current user's
   * interests.
   */
  public static Iterable<Article> getArticlesByInterest(Iterable<Interest> interests, int limit)
      throws DatabaseSchemaException {
    return getArticlesForKeywords(
        Iterables.transform(interests, new Function<Interest, String>() {
          @Override
          public String apply(Interest interest) {
            return interest.getKeyword();
          }
        }),
        limit);
  }

  /**
   * Returns a random article.
   */
  public static Article getRandomArticle() throws DatabaseSchemaException {
    return Database.with(Article.class).getFirst(new QueryOption.AscendingSort("rand()"));
  }

  /**
   * Returns a random untrained article
   */
  public static Article getRandomUntrainedArticle() throws DatabaseSchemaException {
    Article article;
    Iterable<TrainedArticleIndustry> taggedIndustries;
    do {
      article = Articles.getRandomArticle();
      taggedIndustries = TrainedArticleIndustries.getFromArticle(article.getUrlId());
    } while (!Iterables.isEmpty(taggedIndustries));
    return article;
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(Article.class).createTable();
  }
}
