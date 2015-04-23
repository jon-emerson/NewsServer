package com.janknspank.classifier.manual;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.ManualHeuristicFeature;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;

public class ManualFeatureAcquisitions extends ManualHeuristicFeature {
  // Acquisition keywords.
  // NOTE: Do not use the word "Acquisition".  It matches articles like this:
  // http://www.channelnewsasia.com/news/singapore/parliament-passes-changes/1714406.html
  private static final Map<Pattern, Double> TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("acquires"), 1.0)
          .put(Pattern.compile("is acquiring"), 1.0)
          .put(Pattern.compile("buys"), 0.9)
          .put(Pattern.compile("is buying"), 0.8)
          .put(Pattern.compile("buying .+ for"), 1.0)
          .put(Pattern.compile("to acquire .+illion"), 1.0)
          .build();
  private static final Iterable<Pattern> TITLE_BLACKLIST =
      Arrays.asList(Pattern.compile("for buying"), 
          Pattern.compile("home-buying"), 
          Pattern.compile("-buying"), 
          Pattern.compile("buying for"), 
          Pattern.compile("buying experience"),
          Pattern.compile("worth buying"),
          Pattern.compile("buying\\?"),
          Pattern.compile("buying opportunity"),
          Pattern.compile("biggest buys"),
          Pattern.compile("who buys"),
          Pattern.compile("is buying what's"),
          Pattern.compile(" buying of "),
          Pattern.compile("buys.*apartment"),
          Pattern.compile("it buys "),
          Pattern.compile(" bond buys"),
          Pattern.compile("acquires.*for 20[0-9]{2}"));
  private static final Map<Pattern, Double> BODY_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("acquires"), 0.8)
          .put(Pattern.compile("is acquiring"), 0.8)
          .build();
  private static final Iterable<Pattern> BODY_BLACKLIST = Arrays.asList();

  public ManualFeatureAcquisitions(FeatureId featureId) {
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
