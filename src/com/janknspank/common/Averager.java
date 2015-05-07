package com.janknspank.common;

/**
 * Helper class for collecting the average of a progressively received set of
 * numbers.
 */
public class Averager {
  private int count = 0;
  private double sum = 0;

  public void add(Number number) {
    count++;
    sum += number.doubleValue();
  }

  public int getCount() {
    return count;
  }

  public double get() {
    return sum / count;
  }
}