package com.janknspank.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PatternCache extends ThreadLocal<LinkedHashMap<String, Pattern>> {
  private static final int CACHE_SIZE_PER_THREAD = 100;

  @Override
  protected LinkedHashMap<String, Pattern> initialValue() {
    return new LinkedHashMap<String, Pattern>() {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Pattern> eldest) {
        return size() > CACHE_SIZE_PER_THREAD;
      }
    };
  }

  public Pattern getPattern(final String regex) {
    if (this.get().containsKey(regex)) {
      return this.get().get(regex);
    }
    Pattern pattern = Pattern.compile(regex);
    this.get().put(regex, pattern);
    return pattern;
  };
}
