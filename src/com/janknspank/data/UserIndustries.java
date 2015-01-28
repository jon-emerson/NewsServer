package com.janknspank.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.proto.Core.IndustryCode;
import com.janknspank.proto.Core.UserIndustry;

public class UserIndustries {
  public static final String SOURCE_LINKEDIN_PROFILE = "lp";
  public static final String SOURCE_EXPLICIT_ADD = "ad";
  public static final String SOURCE_TOMBSTONE = "t";
  
  public static List<UserIndustry> getIndustries(String userId) 
      throws DataInternalException {
    return Database.with(UserIndustry.class).get(
        new QueryOption.WhereEquals("user_id", userId),
        new QueryOption.WhereNotEquals("source", SOURCE_TOMBSTONE));
  }
  
  public static List<UserIndustry> updateIndustries(String userId, DocumentNode profileDocumentNode) 
      throws DataInternalException {
    String industryDescription = profileDocumentNode.findFirst("industry").getFlattenedText();
    IndustryCode industryCode = IndustryCodes.getForDescription(industryDescription);
    UserIndustry userIndustry = UserIndustry.newBuilder()
        .setIndustryCodeId(industryCode.getId())
        .setUserId(userId)
        .setSource(SOURCE_LINKEDIN_PROFILE)
        .setCreateTime(System.currentTimeMillis())
        .build();
    List<UserIndustry> industries = new ArrayList<>();
    industries.add(userIndustry);
    
    // Add any that are not already on the server 
    return updateIndustries(userId, industries);
  }
  
  private static List<UserIndustry> updateIndustries(String userId, List<UserIndustry> industries) 
      throws DataInternalException {
    List<UserIndustry> allIndustries = Lists.newArrayList();
    List<UserIndustry> industriesToInsert = Lists.newArrayList();
    
    List<UserIndustry> industriesAlreadySaved = getIndustries(userId);
    allIndustries.addAll(industriesAlreadySaved);
    Map<Integer, UserIndustry> alreadySavedMap = new HashMap<>();
    for (UserIndustry industry : industriesAlreadySaved) {
      alreadySavedMap.put(industry.getIndustryCodeId(), industry);
    }
    
    for (UserIndustry industryToAdd : industries) {
      if (!alreadySavedMap.containsKey(industryToAdd.getIndustryCodeId())) {
        //Add it
        alreadySavedMap.put(industryToAdd.getIndustryCodeId(), industryToAdd);
        allIndustries.add(industryToAdd);
        industriesToInsert.add(industryToAdd);
      }
    }
    
    try {
      Database.insert(industriesToInsert);
    } catch (ValidationException e) {
      throw new DataInternalException("Error inserting industries: " + e.getMessage(), e);
    }
    
    return allIndustries;
  }
  
  /** Helper method for creating the UserIndustry table. */
  public static void main(String args[]) throws Exception {
    Database.with(UserIndustry.class).createTable();
  }
}
