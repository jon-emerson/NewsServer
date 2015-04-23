package com.janknspank.classifier.manual;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.ManualHeuristicFeature;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;

public class ManualFeatureBigMoney extends ManualHeuristicFeature {
  private static final Map<Pattern, Double> TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          // Trillion
          .put(Pattern.compile("[\\$€][0-9]+(\\.\\d+)?t"), 1.0)
          .put(Pattern.compile("[\\$€][0-9]+(\\.\\d+)? trillion"), 1.0)
          .put(Pattern.compile("[0-9]+(\\.\\d+)?t (dollars|euros|swiss franc)"), 1.0)
          .put(Pattern.compile("[0-9]+(\\.\\d+)? trillion (dollars|euros|swiss franc)"), 1.0)
          // Billion
          .put(Pattern.compile("[\\$€][0-9]+(\\.\\d+)?b"), 1.0)
          .put(Pattern.compile("[\\$€][0-9]+(\\.\\d+)? billion"), 1.0)
          .put(Pattern.compile("[0-9]+(\\.\\d+)?b (dollars|euros|swiss franc)"), 1.0)
          .put(Pattern.compile("[0-9]+(\\.\\d+)? billion (dollars|euros|swiss franc)"), 1.0)
          // Tens-hundreds of millions
          .put(Pattern.compile("[\\$€][0-9]{2,}+(\\.\\d+)?m"), 0.9)
          .put(Pattern.compile("[\\$€][0-9]+(\\.\\d+)? million"), 0.9)
          .put(Pattern.compile("[0-9]{2,}+(\\.\\d+)?m (dollars|euros|swiss franc)"), 0.9)
          .put(Pattern.compile("[0-9]{2,}+(\\.\\d+)? million (dollars|euros|swiss franc)"), 0.9)
          // Single digit millions
          .put(Pattern.compile("[\\$€][0-9]+(\\.\\d+)?m"), 0.8)
          .put(Pattern.compile("[\\$€][0-9]+(\\.\\d+)? million"), 0.8)
          .put(Pattern.compile("[0-9]+(\\.\\d+)?m (dollars|euros|swiss franc)"), 0.8)
          .put(Pattern.compile("[0-9]+(\\.\\d+)? million (dollars|euros|swiss franc)"), 0.8)
          // Hundreds of thousands
          .put(Pattern.compile("[\\$€][0-9]{3}(\\.\\d+)?k"), 0.6)
          .put(Pattern.compile("[\\$€][0-9]{3}(\\.\\d+)? thousand"), 0.6)
          .put(Pattern.compile("[0-9]{3}(\\.\\d+)?k (dollars|euros|swiss franc)"), 0.6)
          .put(Pattern.compile("[0-9]{3}(\\.\\d+)? thousand (dollars|euros|swiss franc)"), 0.6)
          // Precise numbers > $100k
          .put(Pattern.compile("[\\$€][1-9]\\d{2}(k|(,\\d{3})+)"), 0.6)
          .build();
  private static final Iterable<Pattern> TITLE_BLACKLIST =
      Arrays.asList();
  private static final Map<Pattern, Double> BODY_SCORES = TITLE_SCORES;
  private static final Iterable<Pattern> BODY_BLACKLIST =
      Arrays.asList();
  private static final Set<FeatureId> RELEVANT_TO_INDUSTRIES =
      ImmutableSet.of(
          FeatureId.EQUITY_INVESTING,
          FeatureId.AUTOMOTIVE,
          FeatureId.AVIATION,
          FeatureId.BIOTECHNOLOGY,
          FeatureId.INTERNET,
          FeatureId.MERGERS_AND_ACQUISITIONS,
          FeatureId.MINING_AND_METALS,
          FeatureId.OIL_AND_ENERGY,
          FeatureId.UTILITIES,
          FeatureId.VENTURE_CAPITAL);

  public ManualFeatureBigMoney(FeatureId featureId) {
    super(featureId);
  }

  @Override
  public double score(ArticleOrBuilder article) {
    return relevanceToRegexs(article, TITLE_SCORES, TITLE_BLACKLIST,
        BODY_SCORES, BODY_BLACKLIST);
  }

  public static boolean isRelevantToUser(Set<FeatureId> userIndustryFeatureIds) {
    return !Collections.disjoint(RELEVANT_TO_INDUSTRIES, userIndustryFeatureIds);
  }
}
