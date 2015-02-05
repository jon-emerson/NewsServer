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
    assertEquals(-1, distributionBuilder.getPercentileValue(0));
    assertEquals(-1, distributionBuilder.getPercentileValue(10));
    assertEquals(0, distributionBuilder.getPercentileValue(25));
    assertEquals(5, distributionBuilder.getPercentileValue(50));
    assertTrue(distributionBuilder.getPercentileValue(55) >= 5);
    assertTrue(distributionBuilder.getPercentileValue(55) <= 50);
    assertEquals(50, distributionBuilder.getPercentileValue(75));
    assertTrue(distributionBuilder.getPercentileValue(90) >= 50);
    assertTrue(distributionBuilder.getPercentileValue(90) <= 51);
    assertTrue(distributionBuilder.getPercentileValue(95) >= 50);
    assertTrue(distributionBuilder.getPercentileValue(95) <= 51);
    assertEquals(51, distributionBuilder.getPercentileValue(100));
  }
}
