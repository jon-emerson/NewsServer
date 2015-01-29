package com.janknspank.rank;

import java.io.StringReader;
import java.util.List;

import com.google.common.collect.Iterables;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.LinkedInProfiles;
import com.janknspank.bizness.UserIndustries;
import com.janknspank.bizness.UserInterests;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.LinkedInProfile;
import com.janknspank.proto.Core.UserIndustry;
import com.janknspank.proto.Core.UserInterest;

/**
 * Convenience class that combines interests and industries.
 */
public class CompleteUser {
  private Iterable<UserInterest> interests;
  private Iterable<UserIndustry> industries;
  private String currentWorkplace;
  //private Iterable<UserUrlRating> ratings;
  //private Iterable<String> skills;

  public CompleteUser(String userId) throws DatabaseSchemaException, ParserException, BiznessException {
    interests = UserInterests.getInterests(userId);
    industries = UserIndustries.getIndustries(userId);

    LinkedInProfile profile = LinkedInProfiles.getByUserId(userId);
    DocumentNode profileDocument = DocumentBuilder.build(null, new StringReader(profile.getData()));
    List<Node> positions = profileDocument.findAll("position");
    for (Node position : positions) {
      if (position.findFirst("is-current").getFlattenedText().equals("true")) {
        currentWorkplace = position.findFirst("company > name").getFlattenedText();
        break;
      }
    }

    if (industries == null || Iterables.size(industries) == 0) {
      // Try to generate from linkedIn profile
      industries = UserIndustries.updateIndustries(userId, profileDocument);
    }
  }

  public Iterable<UserInterest> getInterests() {
    return interests;
  }

  public String getCurrentWorkplace() {
    return currentWorkplace;
  }

  public Iterable<UserIndustry> getIndustries() {
    return industries;
  }
}
