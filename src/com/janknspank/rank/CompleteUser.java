package com.janknspank.rank;

import java.io.StringReader;
import java.util.List;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.LinkedInProfiles;
import com.janknspank.data.UserInterests;
import com.janknspank.data.UserUrlFavorites;
import com.janknspank.data.UserUrlRatings;
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
import com.janknspank.proto.Core.UserUrlRating;

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
  private List<UserUrlRating> ratings;
  private List<UserInterest> interests;
  private List<UserIndustry> industries;
  private List<UserUrlFavorite> favorites;
  private String currentWorkplace;
  private List<String> skills;
  
  public CompleteUser(String userId) throws DataInternalException, 
      ParserException {
    user = Users.getByUserId(userId);
    ratings = UserUrlRatings.get(userId);
    interests = UserInterests.getInterests(userId);
    favorites = UserUrlFavorites.get(userId);
    
    LinkedInProfile profile = LinkedInProfiles.getByUserId(userId);
    DocumentNode profileDocument = DocumentBuilder.build(null, new StringReader(profile.getData()));
    List<Node> positions = profileDocument.findAll("position");
    for (Node position : positions) {
      // TODO: ask Jon how to do this correctly.
      if (position.getChildNode(4).getFlattenedText().equals("true")) {
        currentWorkplace = position.findFirst("company > name").getFlattenedText();
        break;
      }
    }
  }
  
  public List<UserInterest> getInterests() {
    return interests;
  }
  
  public String getCurrentWorkplace() {
    return currentWorkplace;
  }
}
