package com.janknspank.bizness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;

public class UserInterests {
  /**
   * Returns only valid, currently followed user interests. 
   * Filters out TOMBSTONEd interests
   */
  public static List<Interest> getInterests(User user) {
    Map<InterestType, List<Interest>> tombstoneInterestMap = Maps.asMap(
        ImmutableSet.copyOf(InterestType.values()),
        new Function<InterestType, List<Interest>>() {
          @Override
          public List<Interest> apply(InterestType interestType) {
            return Lists.newArrayList();
          }
        });
    for (Interest interest : user.getInterestList()) {
      if (interest.getSource() == InterestSource.TOMBSTONE) {
        tombstoneInterestMap.get(interest.getType()).add(interest);
      }
    }

    List<Interest> cleanInterests = Lists.newArrayList();
    for (Interest interest : user.getInterestList()) {
      if (interest.getSource() == InterestSource.TOMBSTONE) {
        continue;
      }
      switch (interest.getType()) {
        case ENTITY:
          for (Interest tombstoneInterest : tombstoneInterestMap.get(interest.getType())) {
            // NOTE(jonemerson): Maybe we want to check entity type too?
            // I'm being conservative here and killing any string matches.
            if (tombstoneInterest.getEntity().getKeyword().equals(interest.getEntity().getKeyword())) {
              break;
            }
          }
          cleanInterests.add(interest);
          break;

        case INDUSTRY:
          for (Interest tombstoneInterest : tombstoneInterestMap.get(interest.getType())) {
            if (tombstoneInterest.getIndustryCode() == interest.getIndustryCode()) {
              break;
            }
          }
          cleanInterests.add(interest);
          break;

        default:
          cleanInterests.add(interest);
          break;
      }
    }
    return cleanInterests;
  }

  /**
   * Returns only interests from a specific source. 
   */
  public static List<Interest> getInterestsBySource(User user, InterestSource source) {
    List<Interest> matchingInterests = new ArrayList<>();
    for (Interest interest : user.getInterestList()) {
      if (interest.getSource() == source) {
        matchingInterests.add(interest);
      }
    }
    return matchingInterests;
  }
}
