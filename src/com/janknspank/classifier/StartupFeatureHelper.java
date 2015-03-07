package com.janknspank.classifier;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Records the relevance of various startup feature vectors to their underlying
 * industries.  This class is not yet used - We may want to put it in a better
 * place once we decide how to use it.  (e.g. probably in the ranking package,
 * since /classifier/ should be crawl-time concerns.)
 */
public class StartupFeatureHelper {
  private static final Map<FeatureId, Set<FeatureId>> RELATED_INDUSTRIES_MAP =
      ImmutableMap.<FeatureId, Set<FeatureId>>builder()
          .put(FeatureId.STARTUP_TECH, ImmutableSet.of(
              FeatureId.COMPUTER_HARDWARE,
              FeatureId.COMPUTER_SOFTWARE,
              FeatureId.INTERNET,
              FeatureId.BIOTECHNOLOGY,  // Probably can improve this.
              FeatureId.CONSUMER_ELECTRONICS,
              FeatureId.COMPUTER_GAMES,
              FeatureId.COMPUTER_AND_NETWORK_SECURITY))
          .put(FeatureId.STARTUP_TRADITIONAL, ImmutableSet.of(
              FeatureId.CONSUMER_GOODS,
              FeatureId.AUTOMOTIVE))
          .build();

  /**
   * Returns true if this startup feature is particularly relevant to the
   * passed industry code.
   */
  public static boolean isRelatedToIndustry(FeatureId featureId, FeatureId industryFeatureId) {
    return RELATED_INDUSTRIES_MAP.containsKey(featureId)
        && RELATED_INDUSTRIES_MAP.get(featureId).contains(industryFeatureId);
  }

  public static boolean isRelatedToIndustries(
      FeatureId featureId, Iterable<FeatureId> industryFeatureIds) {
    for (FeatureId industryFeatureId : industryFeatureIds) {
      if (isRelatedToIndustry(featureId, industryFeatureId)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isStartupFeature(FeatureId featureId) {
    return featureId == FeatureId.STARTUP_TRADITIONAL
      || featureId == FeatureId.STARTUP_TECH;
  }
}
