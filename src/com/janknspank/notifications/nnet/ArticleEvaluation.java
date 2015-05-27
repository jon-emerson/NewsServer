package com.janknspank.notifications.nnet;

import java.util.LinkedHashMap;
import java.util.Set;

import org.neuroph.core.data.DataSetRow;

import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.ClicksPerSites;
import com.janknspank.bizness.EntityType;
import com.janknspank.classifier.FeatureId;
import com.janknspank.crawler.SiteManifests;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.CrawlerProto.SiteManifest;
import com.janknspank.proto.NotificationsProto.Notification;

/**
 * Pulls out fields related to an article so that we know how appropriate
 * an article is for notification purposes.
 */
public class ArticleEvaluation {
  private final boolean isCompany;
  private final boolean isEvent;
  private final boolean isFollowedCompany;
  private final double score;
  private final int hotCount;
  private final double siteCtrRating;

  public ArticleEvaluation(Notification notification) {
    isCompany = notification.getIsCompany();
    isEvent = notification.getIsEvent();
    isFollowedCompany = notification.getIsFollowedCompany();
    score = notification.getScore();
    hotCount = notification.getHotCount();

    SiteManifest site = notification.hasUrl()
        ? SiteManifests.getForUrl(notification.getUrl())
        : SiteManifests.getForShortName(
            notification.getText().substring(0, notification.getText().indexOf(": ")));
    siteCtrRating = ClicksPerSites.getCtrRating(site);
  }

  public ArticleEvaluation(Article article, Set<String> followedEntityIds) {
    isCompany = isArticleAboutCompany(article);
    isEvent = isArticleAboutEvent(article);
    isFollowedCompany = isArticleAboutFollowedCompany(article, followedEntityIds);
    score = article.getScore();
    hotCount = article.getHotCount();

    SiteManifest site = SiteManifests.getForUrl(article.getUrl());
    siteCtrRating = ClicksPerSites.getCtrRating(site);
  }

  private static boolean isArticleAboutFollowedCompany(Article article, Set<String> followedEntityIds) {
    for (ArticleKeyword keyword : article.getKeywordList()) {
      if (keyword.getStrength() >= 100
          && keyword.hasEntity()
          && followedEntityIds.contains(keyword.getEntity().getId())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isArticleAboutCompany(Article article) {
    for (ArticleKeyword keyword : article.getKeywordList()) {
      if (keyword.getStrength() >= 100
          && EntityType.fromValue(keyword.getType()).isA(EntityType.ORGANIZATION)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isArticleAboutEvent(Article article) {
    return ArticleFeatures.getFeatureSimilarity(article,
            FeatureId.MANUAL_HEURISTIC_ACQUISITIONS) >= 0.5
        || ArticleFeatures.getFeatureSimilarity(article,
            FeatureId.MANUAL_HEURISTIC_FUNDRAISING) >= 0.5
        || ArticleFeatures.getFeatureSimilarity(article,
            FeatureId.MANUAL_HEURISTIC_LAUNCHES) >= 0.5
        || ArticleFeatures.getFeatureSimilarity(article,
            FeatureId.MANUAL_HEURISTIC_QUARTERLY_EARNINGS) >= 0.5;
  }

  public boolean isCompany() {
    return isCompany;
  }

  public boolean isEvent() {
    return isEvent;
  }

  public boolean isFollowedCompany() {
    return isFollowedCompany;
  }

  public double getScore() {
    return score;
  }

  public int getHotCount() {
    return hotCount;
  }

  public LinkedHashMap<String, Double> generateInputNodes() {
    LinkedHashMap<String, Double> linkedHashMap = Maps.newLinkedHashMap();
    linkedHashMap.put("is-company", isCompany ? 1.0 : 0.0);
    linkedHashMap.put("is-event", isEvent ? 1.0 : 0.0);
    linkedHashMap.put("is-followed-company", isFollowedCompany ? 1.0 : 0.0);
    linkedHashMap.put("score", score);
    linkedHashMap.put("hot-count", Math.min(1.0, ((double) hotCount) / 10));
    linkedHashMap.put("site-ctr-rating", siteCtrRating);
    return linkedHashMap;
  }

  /**
   * For neural network training.
   */
  public synchronized DataSetRow getDataSetRow(boolean clicked) {
    return new DataSetRow(
        Doubles.toArray(generateInputNodes().values()),
        new double[] { clicked ? 1.0 : 0.0 });
  }
}
