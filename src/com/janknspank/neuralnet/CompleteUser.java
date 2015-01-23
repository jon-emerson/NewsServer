package com.janknspank.neuralnet;

import java.io.StringReader;
import java.util.List;

import com.janknspank.data.*;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.*;

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
    List<Node> positions = profileDocument.findAll("positions");
    for (Node position : positions) {
      // TODO: extract current workplace
      System.out.println(position.getFlattenedText());
    }
  }
  
  public List<UserInterest> getInterests() {
    return interests;
  }
}
