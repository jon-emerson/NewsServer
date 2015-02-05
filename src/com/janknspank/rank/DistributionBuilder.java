package com.janknspank.rank;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import com.google.api.client.util.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Doubles;
import com.janknspank.proto.CoreProto.Distribution;

public class DistributionBuilder {
  private final Percentile percentileInstance = new Percentile();
  private List<Double> values = Lists.newArrayList();
  private double[] doubleArrayCache = null;

  public DistributionBuilder() {
  }

  public void add(long value) {
    values.add((double) value);
    doubleArrayCache = null;
  }

  /**
   * Returns what value in the distribution would have the specified {@code
   * percentile}.  E.g. if the distribution was [1, 2, 3, 4, 5], and you called
   * this with 0.8, this method would return 4.
   */
  @VisibleForTesting
  long getPercentileValue(double percentile) {
    if (doubleArrayCache == null) {
      doubleArrayCache = Doubles.toArray(values);
    }
    if (percentile <= 0.0000001) {
      return (long) Doubles.min(doubleArrayCache);
    }
    return (long) percentileInstance.evaluate(doubleArrayCache, percentile);
  }

  private long getCountOfValuesAtMost(long value) {
    int count = 0;
    for (Double d : values) {
      if (d <= value) {
        count++;
      }
    }
    return count;
  }

  public Distribution build() {
    Distribution.Builder builder = Distribution.newBuilder();
    for (double percentile : new double[] { 5, 10, 25, 50, 75, 90, 95, 100 }) {
      long value = getPercentileValue(percentile);
      builder.addPercentile(Distribution.Percentile.newBuilder()
          .setPercentile((int) percentile)
          .setValue(value)
          .setDataPointCount(getCountOfValuesAtMost(value))
          .build());
    }
    return builder.build();
  }
}
