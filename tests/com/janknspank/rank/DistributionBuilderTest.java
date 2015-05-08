package com.janknspank.rank;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DistributionBuilderTest {
  @Test
  public void test() {
    DistributionBuilder distributionBuilder = new DistributionBuilder();
    distributionBuilder.add(-1);
    distributionBuilder.add(0);
    distributionBuilder.add(5);
    distributionBuilder.add(50);
    distributionBuilder.add(51);
    assertEquals(-1.0, distributionBuilder.getPercentileValue(0), 0.000001);
    assertTrue(distributionBuilder.getPercentileValue(0.1) >= -1.0);
    assertTrue(distributionBuilder.getPercentileValue(0.1) <= 0.0);
    assertEquals(0.0, distributionBuilder.getPercentileValue(0.25), 0.000001);
    assertEquals(5.0, distributionBuilder.getPercentileValue(0.5), 0.000001);
    assertTrue(distributionBuilder.getPercentileValue(0.55) >= 5);
    assertTrue(distributionBuilder.getPercentileValue(0.55) <= 50);
    assertEquals(50.0, distributionBuilder.getPercentileValue(0.75), 0.000001);
    assertTrue(distributionBuilder.getPercentileValue(0.9) >= 50);
    assertTrue(distributionBuilder.getPercentileValue(0.9) <= 51);
    assertTrue(distributionBuilder.getPercentileValue(0.95) >= 50);
    assertTrue(distributionBuilder.getPercentileValue(0.95) <= 51);
    assertEquals(51.0, distributionBuilder.getPercentileValue(1.0), 0.000001);
  }
}
