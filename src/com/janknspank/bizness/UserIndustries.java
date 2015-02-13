package com.janknspank.bizness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.classifier.IndustryCode;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry;
import com.janknspank.proto.UserProto.UserIndustry.Relationship;
import com.janknspank.proto.UserProto.UserIndustry.Source;

public class UserIndustries {
  /**
   * Returns a list of industries derived from the user's passed in
   * LinkedIn profile + other industries the user explicitly added
   * @param userId
   * @param profileDocumentNode
   * @return
   * @throws DataInternalException
   */
  public static User updateIndustries(User user, DocumentNode profileDocumentNode) 
      throws BiznessException, DatabaseSchemaException {
    String industryDescription = profileDocumentNode.findFirst("industry").getFlattenedText();
    IndustryCode industryCode = IndustryCode.fromDescription(industryDescription);
    UserIndustry userIndustry = UserIndustry.newBuilder()
        .setIndustryCodeId(industryCode.getId())
        .setSource(Source.LINKED_IN_PROFILE)
        .setRelationship(Relationship.CURRENT_INDUSTRY)
        .setCreateTime(System.currentTimeMillis())
        .build();
    List<UserIndustry> industries = new ArrayList<>();
    industries.add(userIndustry);

    // Add any that are not already on the server 
    return updateIndustries(user, industries, Source.LINKED_IN_PROFILE);
  }

  /**
   * Converts an industry to a String that can be used for uniqueness.
   */
  private static String hashUserIndustry(UserIndustry industry) {
    List<String> tokens = Lists.newArrayList();
    if (industry.hasIndustryCodeId()) {
      tokens.add(Integer.toString(industry.getIndustryCodeId()));
    }
    if (industry.hasSource()) {
      tokens.add(industry.getSource().name());
    }
    return Joiner.on(":").join(tokens);
  }

  /**
   * Updates the industries for a given source (like LinkedIn).  Includes
   * removing old industries and inserting new ones.  Used to keep the DB in
   * sync with LinkedIn profile changes.  This should not be use for explicit
   * industry adds.  For that just generate a new UserIndustry object and push
   * it.
   * @return all industries the user is industryed in, irrespective of source
   */
  private static User updateIndustries(User user,
      List<UserIndustry> industries, Source source)
      throws BiznessException, DatabaseSchemaException {
    List<UserIndustry> finalUserIndustrys = Lists.newArrayList();
    Map<String, UserIndustry> existingUserIndustryMap = Maps.newHashMap();
    for (UserIndustry industry : user.getIndustryList()) {
      if (industry.getSource() == source) {
        existingUserIndustryMap.put(hashUserIndustry(industry), industry);
      } else {
        finalUserIndustrys.add(industry);
      }
    }
    for (UserIndustry industry : industries) {
      String hash = hashUserIndustry(industry);
      if (existingUserIndustryMap.containsKey(hash)) {
        finalUserIndustrys.add(existingUserIndustryMap.get(hash));
      } else {
        finalUserIndustrys.add(industry);
      }
    }
    try {
      return Database.set(user, "industry", finalUserIndustrys);
    } catch (DatabaseRequestException e) {
      throw new BiznessException("Could not update industries: " + e.getMessage(), e);
    }
  }
}
