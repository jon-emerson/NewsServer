package com.janknspank.classifier;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.janknspank.bizness.Industry;

/**
 * Records the relevance of various startup feature vectors to their underlying
 * industries.  This class is not yet used - We may want to put it in a better
 * place once we decide how to use it.  (e.g. probably in the ranking package,
 * since /classifier/ should be crawl-time concerns.)
 */
public class StartupFeatureHelper {
  private static final Map<FeatureId, Set<Industry>> RELATED_INDUSTRIES_MAP =
      ImmutableMap.<FeatureId, Set<Industry>>builder()
          .put(FeatureId.STARTUP_TECH, ImmutableSet.of(
              Industry.COMPUTER_HARDWARE,
              Industry.COMPUTER_SOFTWARE,
              Industry.COMPUTER_NETWORKING,
              Industry.INTERNET,
              Industry.BIOTECHNOLOGY,  // Probably can improve this.
              Industry.CONSUMER_ELECTRONICS,
              Industry.COMPUTER_GAMES,
              Industry.COMPUTER_AND_NETWORK_SECURITY))
          .put(FeatureId.STARTUP_TRADITIONAL, ImmutableSet.of(
              Industry.CONSUMER_GOODS,
              Industry.AUTOMOTIVE))
          .build();

  /**
   * Returns true if this startup feature is particularly relevant to the
   * passed industry code.
   */
  public static boolean isRelatedToIndustry(FeatureId featureId, Industry industryCode) {
    return RELATED_INDUSTRIES_MAP.containsKey(featureId)
        && RELATED_INDUSTRIES_MAP.get(featureId).contains(industryCode);
  }

  public static boolean isRelatedToIndustries(
      FeatureId featureId, Iterable<Industry> userIndustries) {
    for (Industry userIndustry : userIndustries) {
      if (isRelatedToIndustry(featureId, userIndustry)) {
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
