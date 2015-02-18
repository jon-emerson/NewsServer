package com.janknspank.classifier;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ManualHeuristicFeatureTest {
  @Test
  public void test() {
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
}
