package com.janknspank.classifier.manual;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.ManualHeuristicFeature;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;

public class ManualFeatureIsList extends ManualHeuristicFeature {
  private static final Map<Pattern, Double> TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("[0-9]{1,2} ways"), 1.0)
          .put(Pattern.compile("[0-9]{1,2} tips"), 1.0)
          .put(Pattern.compile("[0-9]{1,2} things"), 1.0)
          .put(Pattern.compile("[0-9]{1,2} numbers"), 1.0)
          .put(Pattern.compile("[0-9]{1,2} reasons"), 1.0)
          .put(Pattern.compile("^(two|three|four|five|six|seven|eight|nine|ten|eleven|twelve) "), 0.9)
          .put(Pattern.compile("^[0-9]{1,2} "), 0.9)
          .put(Pattern.compile("this week:"), 0.9)
          .build();
  private static final Iterable<Pattern> TITLE_BLACKLIST =
      Arrays.asList();
  private static final Map<Pattern, Double> BODY_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("the (two|three|four|five|six|seven|eight|nine|ten|eleven|twelve) best .* of the week"), 0.9)
          .put(Pattern.compile("the [0-9]+ best .* of the week"), 0.9)
          .build();
  private static final Iterable<Pattern> BODY_BLACKLIST = Arrays.asList();

  public ManualFeatureIsList(FeatureId featureId) {
    super(featureId);
  }

  @Override
  public double score(ArticleOrBuilder article) {
    return relevanceToRegexs(article, TITLE_SCORES, TITLE_BLACKLIST, BODY_SCORES, BODY_BLACKLIST);
  }

  public static boolean isRelevantToUser(Set<FeatureId> userIndustryFeatureIds) {
    return true;
  }
}
