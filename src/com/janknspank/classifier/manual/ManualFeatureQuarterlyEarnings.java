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

public class ManualFeatureQuarterlyEarnings extends ManualHeuristicFeature {
  private static final Map<Pattern, Double> TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("\\sq[1-4]"), 0.9)
          .put(Pattern.compile("\\s[1-4]q"), 0.9)
          .put(Pattern.compile("\\s(first|second|third|fourth) quarter"), 0.9)
          .put(Pattern.compile("quarterly earnings"), 0.9)
          .put(Pattern.compile("earnings.*quarterly"), 0.9)
          .put(Pattern.compile("smashes earnings estimates"), 0.9)
          .put(Pattern.compile(" earnings beat "), 0.9)
          .build();
  private static final Iterable<Pattern> TITLE_BLACKLIST =
      Arrays.asList(Pattern.compile("earning preview"),
          Pattern.compile("q[1-4] lending"),
          Pattern.compile(" of q[1-4]"), // Best Performing No-Load Mutual Funds Of Q1 2015
          Pattern.compile(" for q[1-4]"), // BoT: Brace for Q1 contraction
          Pattern.compile("q[1-4] economy"), // Thai Q1 economy likely contracted quarter-on-quarter
          Pattern.compile("yields"), // Q1 2015: Global Stimulus Drives Yields Lower
          Pattern.compile("trendforce"), // TrendForce's Reports Q1 Smartphone Shipments Totaled 291.2M Units with Huawei Becoming Top Chinese Brand
          Pattern.compile("spending"), // TrendForce's Reports Q1 Smartphone Shipments Totaled 291.2M Units with Huawei Becoming Top Chinese Brand
          Pattern.compile("industrial reports"), // Greater Montréal Office & Industrial ReportsShow Positive and Steady Activity in Q1-2015
          Pattern.compile("vc"), // London’s Startups Hit A High Of $682M In VC Funding In Q1 2015
          Pattern.compile("\\?"), // Would you rather be a real millionaire or a paper billionaire? The psychology of unicorns and the toll on Q1 returns
          Pattern.compile("funding report"), // Indian startup funding report for Q1 2015
          Pattern.compile("figures"), // Greater Toronto REALTORS(R) Report Q1 Rental Market Figures
          Pattern.compile("installed"), // China Installed 5.04 GW Of New Solar In Q1’15
          Pattern.compile("earnings season") // Enjoy this earnings season because the first quarter looks ugly
          );
  private static final Map<Pattern, Double> BODY_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("announced its (first|second|third|fourth)-quarter numbers"), 0.9)
          .put(Pattern.compile("quarterly revenue"), 0.6)
          .put(Pattern.compile("beat analysts' estimates"), 0.8)
          .build();
  private static final Iterable<Pattern> BODY_BLACKLIST =
      Arrays.asList(Pattern.compile("federal reserve"));
  private static final Set<FeatureId> RELEVANT_TO_INDUSTRIES =
      ImmutableSet.of(
          FeatureId.EQUITY_INVESTING,
          FeatureId.AVIATION,
          FeatureId.BIOTECHNOLOGY,
          FeatureId.INTERNET,
          FeatureId.MINING_AND_METALS,
          FeatureId.OIL_AND_ENERGY,
          FeatureId.UTILITIES);

  public ManualFeatureQuarterlyEarnings(FeatureId featureId) {
    super(featureId);
  }

  @Override
  public double score(ArticleOrBuilder article) {
    return relevanceToRegexs(article, TITLE_SCORES, TITLE_BLACKLIST, BODY_SCORES, BODY_BLACKLIST);
  }

  public static boolean isRelevantToUser(Set<FeatureId> userIndustryFeatureIds) {
    return !Collections.disjoint(RELEVANT_TO_INDUSTRIES, userIndustryFeatureIds);
  }
}
