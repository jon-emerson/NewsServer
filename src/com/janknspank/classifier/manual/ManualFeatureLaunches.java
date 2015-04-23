package com.janknspank.classifier.manual;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.ManualHeuristicFeature;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.UserProto.User;

public class ManualFeatureLaunches extends ManualHeuristicFeature {
  private static final Map<Pattern, Double> TITLE_SCORES =
      ImmutableMap.<Pattern, Double>builder()
          .put(Pattern.compile("launches"), 1.0)
          .put(Pattern.compile("launched"), 0.9)
          .put(Pattern.compile("to launch"), 0.9)
          .put(Pattern.compile("releases"), 0.8)
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
          .put(Pattern.compile("launches"), 0.6)
          .build();
  private static final Iterable<Pattern> BODY_BLACKLIST =
      Arrays.asList(Pattern.compile("manifesto"),
          Pattern.compile("report"),
          Pattern.compile("ago"),
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
  private static final Map<FeatureId, Double> RELEVANT_TO_INDUSTRIES =
      ImmutableMap.<FeatureId, Double>builder()
          .put(FeatureId.APPAREL_AND_FASHION, 1.0)
          .put(FeatureId.AUTOMOTIVE, 1.0)
          .put(FeatureId.AVIATION, 1.0)
          .put(FeatureId.BIOTECHNOLOGY, 1.0)
          .put(FeatureId.COMPUTER_GAMES, 1.0)
          .put(FeatureId.CONSUMER_ELECTRONICS, 1.0)
          .put(FeatureId.HARDWARE_AND_ELECTRONICS, 1.0)
          .put(FeatureId.INTERNET, 1.0)
          .put(FeatureId.SOFTWARE, 1.0)
          .put(FeatureId.USER_EXPERIENCE, 1.0)
          .put(FeatureId.VENTURE_CAPITAL, 1.0)
          .build();

  public ManualFeatureLaunches(FeatureId featureId) {
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
