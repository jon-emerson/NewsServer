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

public class ManualFeatureLaunches extends ManualHeuristicFeature {
  private static final Map<Pattern, Double> TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("launches"), 1.0)
          .put(Pattern.compile(" is launching"), 1.0)
          .put(Pattern.compile("launched"), 0.9)
          .put(Pattern.compile("to launch"), 0.9)
          .put(Pattern.compile("releases"), 0.8)
          .put(Pattern.compile(" will launch "), 0.8)
          .put(Pattern.compile(" launch: "), 0.8)
          .put(Pattern.compile(" on launch day:"), 0.8)
          .put(Pattern.compile(" with launch of "), 0.8)
          .build();
  private static final Iterable<Pattern> TITLE_BLACKLIST =
      Arrays.asList(Pattern.compile("manifesto"),
          Pattern.compile("report"),
          Pattern.compile("ago"),
          Pattern.compile("court"),
          Pattern.compile("lawsuit"),
          Pattern.compile("vinyl"),
          Pattern.compile("investigation"),
          Pattern.compile("election"),
          Pattern.compile("anniversary celebrations"),
          Pattern.compile("awards"),
          Pattern.compile("releases report"),
          Pattern.compile("releases .*white paper"),
          Pattern.compile("launches proxy "),
          Pattern.compile("campaign launches"),
          Pattern.compile("launches audit"),
          Pattern.compile("releases apology"),
          Pattern.compile("releases suggest"),
          Pattern.compile("releases study"),
          Pattern.compile("releases video"),
          Pattern.compile("that launched"),
          Pattern.compile("lawyer"),
          Pattern.compile("USD/"),
          Pattern.compile("criminal"),
          Pattern.compile("afghanistan"),
          Pattern.compile("attack"),
          Pattern.compile("attacks"),
          Pattern.compile("crimea"),
          Pattern.compile("gaza"),
          Pattern.compile("injured"),
          Pattern.compile("iran"),
          Pattern.compile("iraq"),
          Pattern.compile("isil"),
          Pattern.compile("isis"),
          Pattern.compile("killed"),
          Pattern.compile("lebanon"),
          Pattern.compile("militant"),
          Pattern.compile("militants"),
          Pattern.compile("military"),
          Pattern.compile("mortar"),
          Pattern.compile("mortars"),
          Pattern.compile("nasa"),
          Pattern.compile("north korea"),
          Pattern.compile("pakistan"),
          Pattern.compile("paramilitary"),
          Pattern.compile("rocket"),
          Pattern.compile("wounded"));
  private static final Map<Pattern, Double> BODY_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("announced they will launch "), 0.8)
          .put(Pattern.compile(" has launched "), 0.8)
          .put(Pattern.compile("launches"), 0.6)
          .build();
  private static final Iterable<Pattern> BODY_BLACKLIST =
      Arrays.asList(Pattern.compile("manifesto"),
          Pattern.compile("report"),
          Pattern.compile(" ago"),
          Pattern.compile("was launched"),
          Pattern.compile("social media campaign"), 
          Pattern.compile("fell"),
          Pattern.compile("rose"),
          Pattern.compile("as it launches"),
          Pattern.compile("were launched"),
          Pattern.compile("which launches"),
          Pattern.compile("launches into"),
          Pattern.compile("criminal"),
          Pattern.compile("afghanistan"),
          Pattern.compile("attack"),
          Pattern.compile("attacks"),
          Pattern.compile("crimea"),
          Pattern.compile("gaza"),
          Pattern.compile("injured"),
          Pattern.compile("iran"),
          Pattern.compile("iraq"),
          Pattern.compile("isil"),
          Pattern.compile("isis"),
          Pattern.compile("killed"),
          Pattern.compile("lebanon"),
          Pattern.compile("militant"),
          Pattern.compile("militants"),
          Pattern.compile("military"),
          Pattern.compile("mortar"),
          Pattern.compile("mortars"),
          Pattern.compile("nasa"),
          Pattern.compile("north korea"),
          Pattern.compile("pakistan"),
          Pattern.compile("paramilitary"),
          Pattern.compile("rocket"),
          Pattern.compile("wounded"));
  private static final Set<FeatureId> RELEVANT_TO_INDUSTRIES =
      ImmutableSet.of(
          FeatureId.BIOTECHNOLOGY,
          FeatureId.COMPUTER_GAMES,
          FeatureId.CONSUMER_ELECTRONICS,
          FeatureId.HARDWARE_AND_ELECTRONICS,
          FeatureId.INTERNET,
          FeatureId.SOFTWARE,
          FeatureId.USER_EXPERIENCE,
          FeatureId.VENTURE_CAPITAL);

  public ManualFeatureLaunches() {
    super(FeatureId.MANUAL_HEURISTIC_LAUNCHES);
  }

  @Override
  public double score(ArticleOrBuilder article) {
    return relevanceToRegexs(article, TITLE_SCORES, TITLE_BLACKLIST, BODY_SCORES, BODY_BLACKLIST);
  }

  public static boolean isRelevantToUser(Set<FeatureId> userIndustryFeatureIds) {
    return !Collections.disjoint(RELEVANT_TO_INDUSTRIES, userIndustryFeatureIds);
  }
}
