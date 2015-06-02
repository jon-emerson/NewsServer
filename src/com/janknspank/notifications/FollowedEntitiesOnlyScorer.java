package com.janknspank.notifications;

import java.util.List;

import com.google.common.collect.Lists;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.Interest.InterestType;

public class FollowedEntitiesOnlyScorer extends BlendScorer {
  @Override
  public Iterable<Article> getArticles(User user) throws DatabaseSchemaException, BiznessException {
    List<Interest> entityInterests = Lists.newArrayList();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == InterestType.ENTITY) {
        entityInterests.add(interest);
      }
    }
    return Articles.getMainStream(user.toBuilder()
        .clearInterest()
        .addAllInterest(entityInterests)
        .build());
  }
}
