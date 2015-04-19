package com.janknspank.classifier;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.janknspank.bizness.BiznessException;
import com.janknspank.proto.ArticleProto.Article;

public class ManualHeuristicFeatureTest {
  @Test
  public void testGetScore() {
    assertEquals(50.0,
        ManualHeuristicFeature.getScore("Moose drool 500",
            ImmutableMap.<String, Double>builder()
                .put("hello", 200.0)
                .put("drool", 50.0)
                .build()),
        0.000001 /* epsilon */);
    assertEquals(200.0,
        ManualHeuristicFeature.getScore("Moose drool hello 500",
            ImmutableMap.<String, Double>builder()
                .put("hello", 200.0)
                .put("drool", 50.0)
                .build()),
        0.000001 /* epsilon */);
    assertEquals(0.0,
        ManualHeuristicFeature.getScore("Moose drool 500",
            ImmutableMap.<String, Double>builder()
                .put("jorge", 200.0)
                .put("pasilda", 50.0)
                .build()),
        0.000001 /* epsilon */);
  }

  @Test
  public void testScore() throws BiznessException {
    ManualHeuristicFeatureBenchmarks.benchmark(30001);

    ManualHeuristicFeature feature =
        new ManualHeuristicFeature(FeatureId.MANUAL_HEURISTIC_LAUNCHES);
    assertEquals(1.0, feature.score(Article.newBuilder()
        .setTitle("Google launches amazing new product")),
        0.000001 /* epsilon */);
    assertEquals(0.0, feature.score(Article.newBuilder()
        .setTitle("ISIS launches missiles at Israel")),
        0.000001 /* epsilon */);
  }
}
