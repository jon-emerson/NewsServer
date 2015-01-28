package com.janknspank.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Generic utility class for collecting many strings with associated integer
 * values and retaining only the N with the highest values.  Basically, this
 * is an semi-efficient way to implement a "top 10" list.  Usage:
 *
 * <code>
 *   TopList<String, Integer> t = new TopList<>(3);
 *   t.add("ten", 10);
 *   t.add("eleven", 11);
 *   t.add("twelve", 12);
 *   t.add("two", 2);
 *   for (String topThree : t.getKeys()) {
 *     // Print out "twelve", "eleven", "ten".
 *   }
 * </code>
 */
public class TopList<T, U extends Number> implements Iterable<T> {
  private final int maxSize;
  private U smallestValue = null;
  private HashMap<T, U> values = Maps.newHashMap();

  public TopList(int maxSize) {
    this.maxSize = maxSize;
  }

  @SuppressWarnings("unchecked")
  public synchronized void add(T key, U value) {
    if (values.containsKey(key)) {
      values.put(key,  value);
      return;
    }

    // If we're already at capacity...
    if (values.size() == maxSize) {
      if (smallestValue == null ||
          ((Comparable<U>) value).compareTo(smallestValue) <= 0) {
        // Do nothing if the new value isn't bigger than what we've got.
        return;
      }
      // Else, remove the smallest thing we've got.
      for (T existingKey : values.keySet()) {
        if (values.get(existingKey).equals(smallestValue)) {
          values.remove(existingKey);
          break;
        }
      }
      // And update our notion of what the smallest thing is.
      smallestValue = value;
      for (U existingValue : values.values()) {
        if (((Comparable<U>) existingValue).compareTo(smallestValue) < 0) {
          smallestValue = existingValue;
        }
      }
    } else {
      if (values.size() == 0 || ((Comparable<U>) value).compareTo(smallestValue) < 0) {
        smallestValue = value;
      }
    }
    values.put(key, value);
  }

  /**
   * Returns all the keys we're currently tracking, sorted descendingly by
   * their underlying values.
   */
  public synchronized List<T> getKeys() {
    List<T> keyList = Lists.newArrayList(values.keySet());
    Collections.sort(keyList, new Comparator<T>() {
      @SuppressWarnings("unchecked")
      @Override
      public int compare(T t1, T t2) {
        return ((Comparable<U>) values.get(t2)).compareTo(values.get(t1));
      }
    });
    return keyList;
  }

  public U getValue(String key) {
    return values.containsKey(key) ? values.get(key) : null;
  }

  @Override
  public Iterator<T> iterator() {
    return getKeys().iterator();
  }
}
