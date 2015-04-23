package com.janknspank.classifier;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.UserIndustries;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.UserProto.User;

public abstract class ManualHeuristicFeature extends Feature {

  public ManualHeuristicFeature(FeatureId featureId) {
    super(featureId);
    if (featureId.getFeatureType() != FeatureType.MANUAL_HEURISTIC) {
      throw new IllegalStateException("The specified feature ID is not a manual heuristic");
    }
  }

  @Override
  public abstract double score(ArticleOrBuilder article);

  /**
   * Returns true if the manual heuristic feature is relevant to the
   * user based on the user's industries and interests. Some of manual
   * heuristics shouldn't be factored into ranking for all industries
   */
  public abstract boolean isRelevantToUser(User user);
  
  /**
   * Given an article and set of regex's and blacklists, computes a relevance score.
   * Any blacklist matches will return 0.
   */
  protected static double relevanceToRegexs(ArticleOrBuilder article, 
      Map<Pattern, Double> titleScores, Iterable<Pattern> titleBlacklist,
      Map<Pattern, Double> bodyScores, Iterable<Pattern> bodyBlacklist) {
    double titleScore = getScore(article.getTitle().toLowerCase(), 
        titleScores, titleBlacklist);
    if (titleScore < 0) {
      // A blacklist word was found
      return 0;
    }

    String firstParagraph = Iterables.getFirst(article.getParagraphList(), "").toLowerCase();
    double firstParagraphScore = getScore(firstParagraph, bodyScores, 
        bodyBlacklist);
    if (firstParagraphScore < 0) {
      // A blacklist word was found
      return 0;
    }

    return Math.max(titleScore, firstParagraphScore);
  }

  @VisibleForTesting
  static double getScore(String text, Map<Pattern, Double> scoreRules) {
    double score = 0;
    for (Pattern pattern : scoreRules.keySet()) {
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) {
        score = Math.max(score, scoreRules.get(pattern));
      }
    }
    return score;
  }

  @VisibleForTesting
  static double getScore(String text, Map<Pattern, Double> scoreRules, 
      Iterable<Pattern> blacklist) {
    if (Iterables.size(blacklist) > 0) {
      for (Pattern blacklistPattern : blacklist) {
        Matcher blacklistMatcher = blacklistPattern.matcher(text);
        if (blacklistMatcher.find()) {
          return -1;
        }
      }
    }

    return getScore(text, scoreRules);
  }

  protected static boolean isRelevantToUser(User user, Set<FeatureId> relevantIndustries) {
    Iterable<FeatureId> userIndustries = UserIndustries.getIndustryFeatureIds(user);
    for (FeatureId feature : userIndustries) {
      if (relevantIndustries.contains(feature)) {
        return true;
      }
    }
    return false;
  }
}
