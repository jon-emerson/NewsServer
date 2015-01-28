package com.janknspank.rank;

import java.io.StringReader;
import java.util.List;

import com.google.common.collect.Iterables;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.LinkedInProfiles;
import com.janknspank.data.UserIndustries;
import com.janknspank.data.UserInterests;
import com.janknspank.data.UserUrlFavorites;
import com.janknspank.data.Users;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.LinkedInProfile;
import com.janknspank.proto.Core.User;
import com.janknspank.proto.Core.UserIndustry;
import com.janknspank.proto.Core.UserInterest;
import com.janknspank.proto.Core.UserUrlFavorite;

/**
 * Convenience class that combines User
 * UserInterests
 * UserUrl
 * and TrainedArticleRelevance
 * @author tomch
 *
 */
public class CompleteUser {
  private User user;
  private Iterable<UserInterest> interests;
  private Iterable<UserIndustry> industries;
  private Iterable<UserUrlFavorite> favorites;
  private String currentWorkplace;
  //private Iterable<UserUrlRating> ratings;
  //private Iterable<String> skills;
  
  public CompleteUser(String userId) throws DataInternalException, 
      ParserException {
    user = Users.getByUserId(userId);
    interests = UserInterests.getInterests(userId);
    favorites = UserUrlFavorites.get(userId);
    industries = UserIndustries.getIndustries(userId);
    //ratings = UserUrlRatings.get(userId);
    
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
