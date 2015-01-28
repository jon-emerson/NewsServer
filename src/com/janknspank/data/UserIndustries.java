package com.janknspank.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.proto.Core.IndustryCode;
import com.janknspank.proto.Core.UserIndustry;
import com.janknspank.proto.Core.UserInterest;

public class UserIndustries {
  public static final String SOURCE_LINKEDIN_PROFILE = "lp";
  public static final String SOURCE_EXPLICIT_ADD = "ad";
  public static final String SOURCE_TOMBSTONE = "t";
  
  public static Iterable<UserIndustry> getIndustries(String userId) 
      throws DataInternalException {
    return Database.with(UserIndustry.class).get(
        new QueryOption.WhereEquals("user_id", userId),
        new QueryOption.WhereNotEquals("source", SOURCE_TOMBSTONE));
  }
  
  /**
   * Returns a list of industries derived from the user's passed in
   * LinkedIn profile + other industries the user explicitly added
   * @param userId
   * @param profileDocumentNode
   * @return
   * @throws DataInternalException
   */
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
    return updateIndustries(userId, industries, SOURCE_LINKEDIN_PROFILE);
  }
  
  // Used to keep UserIndustry in sync with user's LinkedIn profile
  // Industry. Should not be used for explicit industry adds.
  // For that use just generate a new UserIndustry object and insert it.
  /**
   * Updates the industries for a given source (like LinkedIn)
   * Includes removing old industries and inserting new ones.
   * @param userId
   * @param newIndustries
   * @param source
   * @return all industries the user is interested in, irrespective of source
   * @throws DataInternalException
   */
  private static List<UserIndustry> updateIndustries(String userId, List<UserIndustry> newIndustries,
      String source) throws DataInternalException {
    List<UserIndustry> allIndustries = Lists.newArrayList();
    
    // Find the Industries from source (ex. LinkedIn) the user already has. 
    Map<Integer, UserIndustry> industryByCode = Maps.newHashMap();
    for (UserIndustry industry : getIndustries(userId)) {
      if (source.equals(industry.getSource())) {
        industryByCode.put(industry.getIndustryCodeId(), industry);
      }
      else {
        allIndustries.add(industry);
      }
    }
    
    Set<UserIndustry> industriesToDelete = Sets.newHashSet();
    List<UserIndustry> industriesToInsert = Lists.newArrayList();
    for (UserIndustry industry : industryByCode.values()) {
      industriesToDelete.add(industry);
    }
    
    for (UserIndustry newIndustry : newIndustries) {
      if (!source.equals(newIndustry.getSource())) {
        // This method only supports updating one type at a time.
        throw new DataInternalException("Cannot add UserIndustry of source " + newIndustry.getSource()
            + " inside a call to update UserIndustries of source " + source + ": "
            + newIndustry.toString());
      }
      UserIndustry existingIndustry = industryByCode.get(newIndustry.getIndustryCodeId());
      if (existingIndustry != null) {
        industriesToDelete.remove(existingIndustry);
      } else {
        industriesToInsert.add(newIndustry);
        allIndustries.add(newIndustry);
      }
    }
    
    try {
      Database.insert(industriesToInsert);
    } catch (ValidationException e) {
      throw new DataInternalException("Error inserting industries: " + e.getMessage(), e);
    }
    Database.delete(industriesToDelete);
    
    // Return the latest and greatest industries, regardless of source.
    return allIndustries;
  }
  
  /** Helper method for creating the UserIndustry table. */
  public static void main(String args[]) throws Exception {
    Database.with(UserIndustry.class).createTable();
  }
}
