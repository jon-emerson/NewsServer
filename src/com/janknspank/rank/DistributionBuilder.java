package com.janknspank.rank;

import java.util.Collections;
import java.util.List;

import com.google.api.client.util.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Doubles;
import com.janknspank.proto.CoreProto.Distribution;

public class DistributionBuilder {
  private List<Double> values = Lists.newArrayList();
  private double[] doubleArrayCache = null;

  public DistributionBuilder() {
  }

  public void add(double value) {
    values.add(value);
    doubleArrayCache = null;
  }

  /**
   * Returns what value in the distribution would have the specified {@code
   * percentile}.  E.g. if the distribution was [1, 2, 3, 4, 5], and you called
   * this with 0.75, this method would return 4.
   */
  @VisibleForTesting
  double getPercentileValue(double percentile) {
    if (percentile < 0) {
      throw new IllegalArgumentException("Illegal percentile: " + percentile
          + ". Must greater than or equal to 0.0");
    }
    if (percentile > 1) {
      throw new IllegalArgumentException("Illegal percentile: " + percentile
          + ". Must less than or equal to 1.0");
    }
    if (doubleArrayCache == null) {
      Collections.sort(values);
      doubleArrayCache = Doubles.toArray(values);
    }
    if (percentile <= 0.0000001) {
      return doubleArrayCache.length == 0 ? 0 : Doubles.min(doubleArrayCache);
    }
    // For 0.0 with size = 5, we want bottomIndex = 0, topIndex = DNC, bottomWeight = 1.
    // For 0.05 with size = 5, we want bottomIndex = 0, topIndex = 1, bottomWeight = 0.2.
    // For 0.5 with size = 5, we want bottomIndex = 2, topIndex = DNC, bottomWeight = 1.
    // For 0.75 with size = 5, we want bottomIndex = 3, topIndex = DNC, bottomWeight = 1.
    // For 0.9 with size = 5, we want bottomIndex = 3, topIndex = 4, bottomWeight = 0.4.
    // For 0.95 with size = 5, we want bottomIndex = 3, topIndex = 4, bottomWeight = 0.2.
    // For 0.99 with size = 5, we want bottomIndex = 3, topIndex = 4, bottomWeight = 0.04.
    // For 1.0 with size = 5, we want bottomIndex = 4, topIndex = DNC, bottomWeight = 1.
    int bottomIndex = (int) Math.floor((values.size() - 1) * percentile);
    int topIndex = (int) Math.ceil((values.size() - 1) * percentile);
    double bottomIndexPercentile = bottomIndex / ((double) values.size() - 1);
    double topIndexPercentile = topIndex / ((double) values.size() - 1);
    double bottomWeight = (topIndexPercentile - bottomIndexPercentile < 0.00000001)
        ? 1 // It really doesn't matter what this is...
        : (topIndexPercentile - percentile) / (topIndexPercentile - bottomIndexPercentile);
    return values.get(bottomIndex) * bottomWeight + values.get(topIndex) * (1 - bottomWeight);
  }

  private long getCountOfValuesAtMost(double value) {
    int count = 0;
    for (Double d : values) {
      if (d <= value) {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns a projected normalized value for the given value based on the
   * historical distribution of that value, as provided by {@code distribution}.
   * The returned value will be between 0 and 1.
   */
  public static double projectQuantile(Distribution distribution, double value) {
    if (distribution.getPercentileCount() == 0) {
      throw new IllegalStateException("Distribution has no percentiles");
    }

    // This will become the biggest percentile that's lower than the value.
    Distribution.Percentile bottomPercentile = null;

    // This will become the littlist percentile that's biggest than the value.
    Distribution.Percentile topPercentile = null;

    for (Distribution.Percentile percentile : distribution.getPercentileList()) {
      if (percentile.getValue() <= value
          && (bottomPercentile == null
              || percentile.getPercentileDouble() > bottomPercentile.getPercentileDouble())) {
        bottomPercentile = percentile;
      }
      if (percentile.getValue() >= value
          && (topPercentile == null
              || percentile.getPercentileDouble() < topPercentile.getPercentileDouble())) {
        topPercentile = percentile;
      }
    }

    if (topPercentile == null
        || distribution.getPercentile(distribution.getPercentileCount() - 1).getValue() <= value) {
      return 1.0;
    } else if (bottomPercentile == null) {
      return 0.0;
    }

    double range = topPercentile.getValue() - bottomPercentile.getValue();
    double ratioTowardsTop =
        (range == 0) ? 1 : (value - bottomPercentile.getValue()) / range;
    return topPercentile.getPercentileDouble() * ratioTowardsTop +
        bottomPercentile.getPercentileDouble() * (1 - ratioTowardsTop);
  }

  public Distribution build() {
    if (values.size() == 0) {
      throw new IllegalStateException("Distribution cannot be built: No values set.");
    }
    Distribution.Builder builder = Distribution.newBuilder();
    System.out.println("Building distribution...");
    for (double percentile : new double[] {
        0, 0.01, 0.03, 0.05, 0.10, 0.25, 0.37, 0.50, 0.63, 0.75, 0.80, 0.85, 0.90, 0.93, 0.95, 0.97,
        0.99, 0.995, 0.999, 1.0
    }) {
      double value = getPercentileValue(percentile);
      builder.addPercentile(Distribution.Percentile.newBuilder()
          .setPercentileDouble(percentile)
          .setValue(value)
          .setDataPointCount(getCountOfValuesAtMost(value))
          .build());
      System.out.println("Value at " + percentile + "%: " + value);
    }
    return builder.build();
  }
}
