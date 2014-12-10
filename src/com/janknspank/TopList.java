package com.janknspank;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Generic utility class for collecting many strings with associated integer
 * values and retaining only the N with the highest values.  Basically, this
 * is an semi-efficient way to implement a "top 10" list.  Usage:
 *
 * <code>
 *   TopList t = new TopList(3);
 *   t.add("ten", 10);
 *   t.add("eleven", 11);
 *   t.add("twelve", 12);
 *   t.add("two", 2);
 *   for (String topThree : t.getKeys()) {
 *     // Print out "twelve", "eleven", "ten".
 *   }
 * </code>
 */
public class TopList {
  private final int maxSize;
  private int smallestValue = Integer.MIN_VALUE;
  private HashMap<String, Integer> values = Maps.newHashMap();

  public TopList(int maxSize) {
    this.maxSize = maxSize;
  }

  public synchronized void add(String key, int value) {
    if (values.containsKey(key)) {
      values.put(key,  value);
      return;
    }

    // If we're already at capacity...
    if (values.size() == maxSize) {
      if (value <= smallestValue) {
        // Do nothing if the new value isn't bigger than what we've got.
        return;
      }
      // Else, remove the smallest thing we've got.
      for (String existingKey : values.keySet()) {
        if (values.get(existingKey).equals(smallestValue)) {
          values.remove(existingKey);
          break;
        }
      }
      // And update our notion of what the smallest thing is.
      smallestValue = value;
      for (Integer existingValue : values.values()) {
        if (existingValue < smallestValue) {
          smallestValue = existingValue;
        }
      }
    } else {
      if (values.size() == 0 || value < smallestValue) {
        smallestValue = value;
      }
    }
    values.put(key, value);
  }

  /**
   * Returns all the keys we're currently tracking, sorted by their underlying
   * values.
   */
  public synchronized List<String> getKeys() {
    List<String> keyList = Lists.newArrayList(values.keySet());
    Collections.sort(keyList, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return values.get(o2) - values.get(o1);
      }
    });
    return keyList;
  }

  public int getValue(String key) {
    return values.containsKey(key) ? values.get(key) : 0;
  }
}
