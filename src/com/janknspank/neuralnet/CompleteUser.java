package com.janknspank.neuralnet;

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
  private Iterable<UserUrlRating> ratings;
  private Iterable<UserInterest> interests;
  private Iterable<UserIndustry> industries;
  private Iterable<UserUrlFavorite> favorites;
  private String currentWorkplace;
  private Iterable<String> skills;
  
  public CompleteUser(String userId) throws DataInternalException, 
      ParserException {
    user = Users.getByUserId(userId);
    ratings = UserUrlRatings.get(userId);
    interests = UserInterests.getInterests(userId);
    favorites = UserUrlFavorites.get(userId);
    
    LinkedInProfile profile = LinkedInProfiles.getByUserId(userId);
    DocumentNode profileDocument = DocumentBuilder.build(null, new StringReader(profile.getData()));
    List<Node> positions = profileDocument.findAll("positions");
    for (Node position : positions) {
      // TODO: extract current workplace
      System.out.println(position.getFlattenedText());
    }
  }
  
  public Iterable<UserInterest> getInterests() {
    return interests;
  }
}
