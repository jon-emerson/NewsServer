package com.janknspank.rank;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.bizness.BiznessException;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.proto.UserProto.User;

/**
 * Helper class for generating a set of Articles to train the neural network
 * from, as retrieved both from .persona files and from UserActions.
 */
public class TrainingArticles {
  private static final Map<String, Article> ARTICLE_CACHE = Maps.newHashMap();
  private static Iterable<TrainingArticle> ALL_TRAINING_ARTICLES = null;

  /**
   * Returns if an article should be excluded from use in
   * training the neural network. Articles are excluded so
   * they can be used as a benchmark to determine the quality of the scorer.
   * @return true if should be excluded from training
   */
  static boolean isInTrainingHoldback(Article article) {
    if (article.getUrl().hashCode() % 5 == 0) {
      return true;
    }
    return false;
  }

  /**
   * Cached helper method for getting articles by URL.
   */
  private static Map<String, Article> getArticles(Iterable<String> urls) throws BiznessException {
    Map<String, Article> articleMap = Maps.newHashMap();
    List<String> articleUrlsToCrawl = Lists.newArrayList();
    for (String url : urls) {
      if (ARTICLE_CACHE.containsKey(url)) {
        articleMap.put(url, ARTICLE_CACHE.get(url));
      } else {
        articleUrlsToCrawl.add(url);
      }
    }
    for (Map.Entry<String, Article> entry :
        ArticleCrawler.getArticles(articleUrlsToCrawl, true /* retain */).entrySet()) {
      ARTICLE_CACHE.put(entry.getKey(), entry.getValue());
      articleMap.put(entry.getKey(), entry.getValue());
    }
    return articleMap;
  }

  private static List<TrainingArticle> getPersonaTrainingArticles() throws BiznessException {
    List<TrainingArticle> trainingArticles = Lists.newArrayList();
    for (Persona persona : Personas.getPersonaMap().values()) {
      System.out.println("Grabbing articles for " + persona.getEmail() + " ...");
      User user = Personas.convertToUser(persona);

      Map<String, Article> urlArticleMap =
          getArticles(Iterables.concat(persona.getGoodUrlList(), persona.getBadUrlList()));

      // Count good URLs twice, to encourage goodness.
      // Otherwise the neural network could just return all 0s and get a fairly
      // decent error rate.
      for (int i = 0; i < 2; i++) {
        for (String goodUrl : persona.getGoodUrlList()) {
          if (urlArticleMap.containsKey(goodUrl)) {
            trainingArticles.add(new TrainingArticle(urlArticleMap.get(goodUrl), user, 1.0));
          }
        }
      }

      for (String badUrl : persona.getBadUrlList()) {
        if (urlArticleMap.containsKey(badUrl)) {
          trainingArticles.add(new TrainingArticle(urlArticleMap.get(badUrl), user, 0.0));
        }
      }
    }
    return trainingArticles;
  }

  public static synchronized Iterable<TrainingArticle> getTrainingArticles()
      throws DatabaseSchemaException, BiznessException {
    if (ALL_TRAINING_ARTICLES == null) {
      ALL_TRAINING_ARTICLES = getPersonaTrainingArticles();
    }
    return Iterables.filter(ALL_TRAINING_ARTICLES, new Predicate<TrainingArticle>() {
      @Override
      public boolean apply(TrainingArticle trainingArticle) {
        return !isInTrainingHoldback(trainingArticle.getArticle());
      }
    });
  }

  public static synchronized Iterable<TrainingArticle> getHoldbackArticles()
      throws DatabaseSchemaException, BiznessException {
    getTrainingArticles();
    return Iterables.filter(ALL_TRAINING_ARTICLES, new Predicate<TrainingArticle>() {
      @Override
      public boolean apply(TrainingArticle trainingArticle) {
        return isInTrainingHoldback(trainingArticle.getArticle());
      }
    });
  }
}
