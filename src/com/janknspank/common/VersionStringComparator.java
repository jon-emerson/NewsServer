package com.janknspank.common;

import java.util.Comparator;
import java.util.List;

import com.google.api.client.util.Lists;
import com.google.common.base.Splitter;

public class VersionStringComparator implements Comparator<String> {
  private static class Version {
    private final List<Integer> partsList = Lists.newArrayList();

    private Version(String versionString) {
      if (versionString != null) {
        for (String partString : Splitter.on(".").split(versionString)) {
          partsList.add(Integer.parseInt(partString));
        }
      }
    }
  }

  @Override
  public int compare(String versionString1, String versionString2) {
    Version version1 = new Version(versionString1);
    Version version2 = new Version(versionString2);
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
