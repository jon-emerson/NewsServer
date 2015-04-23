package com.janknspank.rank;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.bizness.BiznessException;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserAction;
import com.janknspank.proto.UserProto.UserAction.ActionType;

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

  /**
   * Returns the users we trust to give us good training data via UserActions.
   */
  private static Map<String, User> getUserActionTrustedUsers() throws DatabaseSchemaException {
    Map<String, User> users = Maps.newHashMap();
    for (User user : Database.with(User.class).get(
        new QueryOption.WhereEquals("email", ImmutableList.of(
            "dvoytenko@yahoo.com",
            "jon@jonemerson.net",
            "panaceaa@gmail.com"
            // "virendesai87@gmail.com"
            )))) {
      users.put(user.getId(), user);
    }
    return users;
  }

  /**
   * Returns a list of TrainingArticles derived from VOTE_UP or X_OUT user
   * actions from users we trust to be of higher quality.
   */
  private static List<TrainingArticle> getUserActionTrainingArticles(ActionType actionType)
      throws DatabaseSchemaException, BiznessException {
    Map<String, User> users = getUserActionTrustedUsers();

    // For ActionType.VOTE_UP, ignore any URLs that were later unvoted up.  For
    // convenience, we have a global blacklist here.  To be 100% correct, we'd
    // have individual backlists per-user... but this is easier and probably 99%
    // accurate.
    Set<String> urlIdsToIgnore = Sets.newHashSet();
    if (actionType == ActionType.VOTE_UP) {
      for (UserAction userAction : Database.with(UserAction.class).get(
          new QueryOption.WhereEquals("user_id", users.keySet()),
          new QueryOption.WhereEqualsEnum("action_type", ActionType.UNVOTE_UP))) {
        urlIdsToIgnore.add(userAction.getUrlId());
      }
    }

    // Figure out training articles for each of the unblacklisted vote up
    // actions.
    List<TrainingArticle> trainingArticles = Lists.newArrayList();
    for (User user : users.values()) {
      Iterable<UserAction> userActions = Database.with(UserAction.class).get(
          new QueryOption.WhereEquals("user_id", user.getId()),
          new QueryOption.WhereEqualsEnum("action_type", actionType));
      System.out.println("For " + user.getEmail() + ", " + Iterables.size(userActions)
          + " " + actionType.name() + " user actions found");

      Set<String> urlsToCrawl = Sets.newHashSet();
      for (UserAction userAction : userActions) {
        if (!urlIdsToIgnore.contains(userAction.getUrlId())) {
          urlsToCrawl.add(userAction.getUrl());
        }
      }

      Map<String, Article> articleMap = getArticles(urlsToCrawl);
      for (UserAction userAction : userActions) {
        if (userAction.hasOnStreamForInterest()) {
          // Ignore these for now.  They're from substreams, e.g. the user is
          // viewing a specific entity or topic, not their main stream.
          continue;
        }
        if (articleMap.containsKey(userAction.getUrl())) {
          User modifiedUser = user.toBuilder()
              .clearInterest()
              .addAllInterest(userAction.getInterestList())
              .build();
          trainingArticles.add(
              new TrainingArticle(articleMap.get(userAction.getUrl()), modifiedUser,
                  actionType == ActionType.VOTE_UP ? 1.0 : 0.0));
        }
      }
    }
    return trainingArticles;
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
      ALL_TRAINING_ARTICLES = Iterables.concat(
          getPersonaTrainingArticles(),
          getUserActionTrainingArticles(ActionType.VOTE_UP));
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
