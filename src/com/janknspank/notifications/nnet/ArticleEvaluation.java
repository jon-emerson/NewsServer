package com.janknspank.notifications.nnet;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.neuroph.core.data.DataSetRow;

import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.Articles;
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
  private final long ageInMillis;

  public ArticleEvaluation(Notification notification) {
    isCompany = notification.getIsCompany();
    isEvent = notification.getIsEvent();
    isFollowedCompany = notification.getIsFollowedCompany();
    score = notification.getScore();
    hotCount = notification.getHotCount();

    if (notification.hasAgeInMillis()) {
      ageInMillis = notification.getAgeInMillis();
    } else {
      // Default to a randomized value that aveages at the historical average
      // for clicked vs. unclicked notifications.
      // This is the distribution we should match:
      //  Age 0 hours: 1.9169329073482428% CTR on 3756 data points
      //  Age 1 hours: 1.8823529411764703% CTR on 2550 data points
      //  Age 2 hours: 1.4609571788413098% CTR on 1985 data points
      //  Age 3 hours: 2.0537124802527646% CTR on 633 data points
      //  Age 4 hours: 0.5607476635514018% CTR on 535 data points
      //  Age 5 hours: 1.5384615384615385% CTR on 455 data points
      //  Age 6 hours: 1.9148936170212765% CTR on 470 data points
      //  Age 7 hours: 0.87527352297593% CTR on 457 data points
      //  Age 8 hours: 1.2919896640826873% CTR on 387 data points
      //  Age 9 hours: 1.13314447592068% CTR on 353 data points
      //  Age 10 hours: 1.03359173126615% CTR on 387 data points
      //  Age 11 hours: 1.3605442176870748% CTR on 294 data points
      //  Age 12 hours: 0.9259259259259258% CTR on 216 data points
      //  Age 13 hours: 0.5291005291005291% CTR on 189 data points
      //  Age 14 hours: 1.477832512315271% CTR on 203 data points
      //  Age 15 hours: 0.847457627118644% CTR on 118 data points
      //  Age 16 hours: 0.8849557522123894% CTR on 113 data points
      double randomSquare = Math.random() * Math.random(); // This should average at 0.25.
      double normalizedSquare = randomSquare * 4; // Average = 1;
      double crawlDelay = (Math.random() * TimeUnit.MINUTES.toMillis(30));
      if (notification.hasClickTime()) {
        ageInMillis = (long) (normalizedSquare * 13777151 + crawlDelay);
      } else {
        ageInMillis = (long) (normalizedSquare * 17363732 + crawlDelay);
      }
    }

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
    ageInMillis = System.currentTimeMillis() - Articles.getPublishedTime(article);

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

  public double getAgeInterpretation() {
    // Reduce resolution on this so that similarly aged articles can be judged
    // together without giving too much data to the neural network that would
    // lead towards overtraining.
    long ageInHalfHours = (long) Math.floor(((double) ageInMillis) / TimeUnit.MINUTES.toMillis(30));
    return Math.max(0, Math.min(1, ((double) ageInHalfHours) / 48));
  }

  public LinkedHashMap<String, Double> generateInputNodes() {
    LinkedHashMap<String, Double> linkedHashMap = Maps.newLinkedHashMap();
    linkedHashMap.put("is-company", isCompany ? 1.0 : 0.0);
    linkedHashMap.put("is-event", isEvent ? 1.0 : 0.0);
    linkedHashMap.put("is-followed-company", isFollowedCompany ? 1.0 : 0.0);
    linkedHashMap.put("score", score);
    linkedHashMap.put("hot-count", Math.min(1.0, ((double) hotCount) / 10));
    linkedHashMap.put("site-ctr-rating", siteCtrRating);
    linkedHashMap.put("age-in-days", getAgeInterpretation());
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
