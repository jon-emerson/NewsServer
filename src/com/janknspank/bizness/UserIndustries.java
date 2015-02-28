package com.janknspank.bizness;

import java.util.Set;

import com.google.api.client.util.Sets;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.UserOrBuilder;

public class UserIndustries {
  /**
   * Returns a user's industry interests, accounting for tombstones and dupes.
   */
  public static Iterable<Industry> getIndustries(UserOrBuilder user) {
    Set<Integer> tombstonedIndustryCodes = Sets.newHashSet();
    Set<Integer> industries = Sets.newHashSet();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == InterestType.INDUSTRY) {
        if (interest.getSource() == InterestSource.TOMBSTONE) {
          tombstonedIndustryCodes.add(interest.getIndustryCode());
        } else {
          industries.add(interest.getIndustryCode());
        }
      }
    }
    industries.removeAll(tombstonedIndustryCodes);
    return Iterables.transform(industries, new Function<Integer, Industry>() {
      @Override
      public Industry apply(Integer industryCode) {
        return Industry.fromCode(industryCode);
      }
    });
  }
}