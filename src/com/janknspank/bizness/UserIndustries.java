package com.janknspank.bizness;

import java.util.Set;

import com.google.api.client.util.Sets;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.janknspank.classifier.FeatureId;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.UserOrBuilder;

public class UserIndustries {
  /**
   * Returns a user's industry interests, accounting for tombstones and dupes.
   */
  public static Iterable<FeatureId> getIndustryFeatureIds(UserOrBuilder user) {
    Set<Integer> tombstonedFeatureIds = Sets.newHashSet();
    Set<Integer> featureIds = Sets.newHashSet();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == InterestType.INDUSTRY) {
        if (interest.getSource() == InterestSource.TOMBSTONE) {
          tombstonedFeatureIds.add(interest.getIndustryCode());
        } else {
          featureIds.add(interest.getIndustryCode());
        }
      }
    }
    featureIds.removeAll(tombstonedFeatureIds);
    return Iterables.transform(featureIds, new Function<Integer, FeatureId>() {
      @Override
      public FeatureId apply(Integer industryFeatureId) {
        return FeatureId.fromId(industryFeatureId);
      }
    });
  }
}
