package com.janknspank.bizness;

import java.util.ArrayList;
import java.util.List;

import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry;

public class UserIndustries {
  /**
   * Filters out TOMBSTONEd industries
   */
  public static List<UserIndustry> getCurrentIndustries(User user) {
    List<UserIndustry> allIndustries = user.getIndustryList();
    List<UserIndustry> currentIndustries = new ArrayList<>();
    for (UserIndustry industry : allIndustries) {
      if (industry.getSource() != UserIndustry.Source.TOMBSTONE) {
        currentIndustries.add(industry);
      }
    }
    return currentIndustries;
  }
}