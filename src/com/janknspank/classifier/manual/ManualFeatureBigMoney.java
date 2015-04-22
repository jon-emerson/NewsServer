package com.janknspank.classifier.manual;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.janknspank.bizness.UserIndustries;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.ManualHeuristicFeature;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.UserProto.User;

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
  private static final Map<FeatureId, Double> RELEVANT_TO_INDUSTRIES =
      ImmutableMap.<FeatureId, Double>builder()
          .put(FeatureId.EQUITY_INVESTING, 1.0)
          .put(FeatureId.AUTOMOTIVE, 1.0)
          .put(FeatureId.AVIATION, 1.0)
          .put(FeatureId.BIOTECHNOLOGY, 1.0)
          .put(FeatureId.INTERNET, 1.0)
          .put(FeatureId.MERGERS_AND_ACQUISITIONS, 1.0)
          .put(FeatureId.MINING_AND_METALS, 1.0)
          .put(FeatureId.OIL_AND_ENERGY, 1.0)
          .put(FeatureId.UTILITIES, 1.0)
          .put(FeatureId.VENTURE_CAPITAL, 1.0)
          .build();

  public ManualFeatureBigMoney(FeatureId featureId) {
    super(featureId);
  }

  @Override
  public double score(ArticleOrBuilder article) {
    return relevanceToRegexs(article, TITLE_SCORES, TITLE_BLACKLIST,
        BODY_SCORES, BODY_BLACKLIST);
  }

  public boolean isRelevantToUser(User user) {
    return isRelevantToUser(user, RELEVANT_TO_INDUSTRIES);
  }
}
