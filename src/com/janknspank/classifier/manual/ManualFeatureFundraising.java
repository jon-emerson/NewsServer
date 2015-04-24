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

public class ManualFeatureFundraising extends ManualHeuristicFeature {
  private static final Map<Pattern, Double> TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("series a "), 1.0)
          .put(Pattern.compile("series a$"), 1.0)
          .put(Pattern.compile("series a,"), 1.0)
          .put(Pattern.compile("series b "), 1.0)
          .put(Pattern.compile("series b$"), 1.0)
          .put(Pattern.compile("series b,"), 1.0)
          .put(Pattern.compile("series c "), 1.0)
          .put(Pattern.compile("series c$"), 1.0)
          .put(Pattern.compile("series c,"), 1.0)
          .put(Pattern.compile("series d "), 1.0)
          .put(Pattern.compile("series d$"), 1.0)
          .put(Pattern.compile("series d,"), 1.0)
          .put(Pattern.compile("series e "), 1.0)
          .put(Pattern.compile("series e$"), 1.0)
          .put(Pattern.compile("series e,"), 1.0)
          .put(Pattern.compile("angel round"), 1.0)
          .put(Pattern.compile("funding round"), 1.0)
          .put(Pattern.compile("raises \\$.+m funding"), 1.0)
          .put(Pattern.compile("raises \\$.+ million"), 1.0)
          .put(Pattern.compile("million of funding"), 1.0)
          .build();
  private static final Iterable<Pattern> TITLE_BLACKLIST =
      Arrays.asList(Pattern.compile("declares .*dividend"));
  private static final Map<Pattern, Double> BODY_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("series a\\."), 0.9)
          .put(Pattern.compile("series a "), 0.9)
          .put(Pattern.compile("series a,"), 0.9)
          .put(Pattern.compile("series b\\."), 0.9)
          .put(Pattern.compile("series b "), 0.9)
          .put(Pattern.compile("series b,"), 0.9)
          .put(Pattern.compile("series c\\."), 0.9)
          .put(Pattern.compile("series c "), 0.9)
          .put(Pattern.compile("series c,"), 0.9)
          .put(Pattern.compile("series d\\."), 0.9)
          .put(Pattern.compile("series d "), 0.9)
          .put(Pattern.compile("series d,"), 0.9)
          .put(Pattern.compile("series e\\."), 0.9)
          .put(Pattern.compile("series e "), 0.9)
          .put(Pattern.compile("series e,"), 0.9)
          .put(Pattern.compile("angel round"), 0.9)
          .build();
  private static final Iterable<Pattern> BODY_BLACKLIST =
      Arrays.asList();
  private static final Set<FeatureId> RELEVANT_TO_INDUSTRIES =
      ImmutableSet.of(
          FeatureId.BIOTECHNOLOGY,
          FeatureId.HARDWARE_AND_ELECTRONICS,
          FeatureId.INTERNET,
          FeatureId.OIL_AND_ENERGY,
          FeatureId.VENTURE_CAPITAL,
          FeatureId.UTILITIES);

  public ManualFeatureFundraising(FeatureId featureId) {
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
