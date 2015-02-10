package com.janknspank.rank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import com.google.api.client.util.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Doubles;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.proto.CoreProto.Distribution;

public class DistributionBuilder {
  private final Percentile percentileInstance = new Percentile();
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
   * this with 0.8, this method would return 4.
   */
  @VisibleForTesting
  double getPercentileValue(double percentile) {
    if (doubleArrayCache == null) {
      doubleArrayCache = Doubles.toArray(values);
    }
    if (percentile <= 0.0000001) {
      return doubleArrayCache.length == 0 ? 0 : Doubles.min(doubleArrayCache);
    }
    return percentileInstance.evaluate(doubleArrayCache, percentile);
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
   * Returns what value exists at the given percentile.
   * E.g. getValueAtPercentile(distribution, 50) would give the median value.
   * @param percentile value between 0 and 100, inclusive
   */
  public static double getValueAtPercentile(Distribution distribution, double percentile) {
    // This will become the biggest percentile that's lower than the quantile.
    Distribution.Percentile bottomPercentileObj = null;

    // This will become the littlist percentile that's biggest than the quantile.
    Distribution.Percentile topPercentileObj = null;

    for (Distribution.Percentile percentileObj : distribution.getPercentileList()) {
      if (percentileObj.getPercentile() <= percentile
          && (bottomPercentileObj == null
              || percentileObj.getPercentile() > bottomPercentileObj.getPercentile())) {
        bottomPercentileObj = percentileObj;
      }
      if (percentileObj.getPercentile() >= percentile
          && (topPercentileObj == null
              || percentileObj.getPercentile() < topPercentileObj.getPercentile())) {
        topPercentileObj = percentileObj;
      }
    }

    double range = topPercentileObj.getPercentile() - bottomPercentileObj.getPercentile();
    double ratioTowardsTop =
        (range == 0) ? 1 : (percentile - bottomPercentileObj.getPercentile()) / range;
    return (topPercentileObj.getValue() * ratioTowardsTop +
        bottomPercentileObj.getValue() * (1 - ratioTowardsTop));
  }

  /**
   * Returns a projected normalized value for the given value based on the
   * historical distribution of that value, as provided by {@code distribution}.
   */
  public static double projectQuantile(Distribution distribution, double value) {
    if (distribution.getPercentileCount() == 0) {
      throw new IllegalStateException("Distribution has no percentiles");
    }
    value = Math.max(0, Math.min(1, value));

    // This will become the biggest percentile that's lower than the value.
    Distribution.Percentile bottomPercentile = null;

    // This will become the littlist percentile that's biggest than the value.
    Distribution.Percentile topPercentile = null;

    for (Distribution.Percentile percentile : distribution.getPercentileList()) {
      if (percentile.getValue() <= value
          && (bottomPercentile == null
              || percentile.getPercentile() > bottomPercentile.getPercentile())) {
        bottomPercentile = percentile;
      }
      if (percentile.getValue() >= value
          && (topPercentile == null
              || percentile.getPercentile() < topPercentile.getPercentile())) {
        topPercentile = percentile;
      }
    }

    double range = topPercentile.getValue() - bottomPercentile.getValue();
    double ratioTowardsTop =
        (range == 0) ? 1 : (value - bottomPercentile.getValue()) / range;
    return (topPercentile.getPercentile() * ratioTowardsTop +
        bottomPercentile.getPercentile() * (1 - ratioTowardsTop)) / 100;
  }

  public Distribution build() {
    Distribution.Builder builder = Distribution.newBuilder();
    for (double percentile : new double[] { 0, 1, 5, 10, 25, 37, 50, 63, 75, 90, 95, 99, 100 }) {
      double value = getPercentileValue(percentile);
      builder.addPercentile(Distribution.Percentile.newBuilder()
          .setPercentile((int) percentile)
          .setValue(value)
          .setDataPointCount(getCountOfValuesAtMost(value))
          .build());
    }
    return builder.build();
  }

  public void writeToFile(File distributionFile) throws ClassifierException {
    OutputStream outputStream = null;
    try {
      outputStream = new GZIPOutputStream(new FileOutputStream(distributionFile));
      build().writeTo(outputStream);
    } catch (IOException e) {
      throw new ClassifierException("Could not write file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }

  public static Distribution fromFile(File distributionFile) throws ClassifierException {
    InputStream inputStream = null;
    try {
      inputStream = new GZIPInputStream(new FileInputStream(distributionFile));
      return Distribution.parseFrom(inputStream);
    } catch (IOException e) {
      throw new ClassifierException("Could not read file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }
}
