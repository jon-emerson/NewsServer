package com.janknspank.classifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.janknspank.proto.UserProto.UserIndustry;

/**
 * Records the relevance of various startup feature vectors to their underlying
 * industries.  This class is not yet used - We may want to put it in a better
 * place once we decide how to use it.  (e.g. probably in the ranking package,
 * since /classifier/ should be crawl-time concerns.)
 */
public class StartupFeatureHelper {
  private static final Map<FeatureId, Set<IndustryCode>> RELATED_INDUSTRIES_MAP =
      ImmutableMap.<FeatureId, Set<IndustryCode>>builder()
          .put(FeatureId.STARTUP_TECH, ImmutableSet.of(
              IndustryCode.COMPUTER_HARDWARE,
              IndustryCode.COMPUTER_SOFTWARE,
              IndustryCode.COMPUTER_NETWORKING,
              IndustryCode.INTERNET,
              IndustryCode.BIOTECHNOLOGY,  // Probably can improve this.
              IndustryCode.CONSUMER_ELECTRONICS,
              IndustryCode.COMPUTER_GAMES,
              IndustryCode.COMPUTER_AND_NETWORK_SECURITY))
          .put(FeatureId.STARTUP_TRADITIONAL, ImmutableSet.of(
              IndustryCode.CONSUMER_GOODS,
              IndustryCode.AUTOMOTIVE))
          .build();

  /**
   * Returns true if this startup feature is particularly relevant to the
   * passed industry code.
   */
  public static boolean isRelatedToIndustry(FeatureId featureId, IndustryCode industryCode) {
    return RELATED_INDUSTRIES_MAP.containsKey(featureId)
        && RELATED_INDUSTRIES_MAP.get(featureId).contains(industryCode);
  }

  public static boolean isRelatedToIndustries(
      FeatureId featureId, List<UserIndustry> userIndustries) {
    for (UserIndustry userIndustry : userIndustries) {
      if (isRelatedToIndustry(
          featureId, IndustryCode.findFromId(userIndustry.getIndustryCodeId()))) {
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
