package com.janknspank.common;

import java.util.Comparator;
import java.util.List;

import com.google.api.client.util.Lists;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class Version {
  private static final VersionComparator COMPARATOR = new VersionComparator();
  private final List<Integer> partsList = Lists.newArrayList();

  public Version(String versionString) {
    if (versionString != null) {
      for (String partString : Splitter.on(".").split(versionString)) {
        partsList.add(Integer.parseInt(partString));
      }
    }
  }

  /**
   * If this=0.5.4, and version=0.5.3, return true.
   * If this=0.5.4, and version=0.5.4, return true.
   * If this=0.5.4, and version=0.5.5, return false.
   */
  public boolean atLeast(Version version) {
    return COMPARATOR.compare(this, version) >= 0;
  }

  public boolean atLeast(String versionString) {
    return COMPARATOR.compare(this, new Version(versionString)) >= 0;
  }

  public boolean isLessThan(Version version) {
    return !atLeast(version);
  }

  public boolean isLessThan(String versionString) {
    return !atLeast(versionString);
  }

  public static VersionComparator getComparator() {
    return COMPARATOR;
  }

  public static class VersionComparator implements Comparator<Version> {
    private VersionComparator() {}

    @Override
    public int compare(Version version1, Version version2) {
      // Null checks.  Null is considered the lowest version number possible.
      if (version1 == version2) {
        return 0;
      } else if (version1 == null) {
        return -1;
      } else if (version2 == null) {
        return 1;
      }

      int index = 0;
      while (true) {
        if (version1.partsList.size() <= index
            && version2.partsList.size() <= index) {
          return 0;
        }
        int part1 = (version1.partsList.size() > index) ? version1.partsList.get(index) : 0;
        int part2 = (version2.partsList.size() > index) ? version2.partsList.get(index) : 0;
        if (part1 != part2) {
          return Integer.compare(part1, part2);
        }
        index++;
      }
    }
  }

  @Override
  public String toString() {
    return Joiner.on(".").join(partsList);
  }
}
