package com.janknspank.notifications;

import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.TimeRankingStrategy.MainStreamStrategy;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.rank.DiversificationPass;
import com.janknspank.rank.NeuralNetworkScorer;

public class FollowedEntitiesOnlyScorer extends BlendScorer {
  @Override
  public Iterable<Article> getArticles(User user) throws DatabaseSchemaException, BiznessException {
    List<Interest> entityInterests = Lists.newArrayList();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == InterestType.ENTITY) {
        entityInterests.add(interest);
      }
    }
    return Articles.getRankedArticles(
        user.toBuilder()
            .clearInterest()
            .addAllInterest(entityInterests)
            .build(),
        NeuralNetworkScorer.getInstance(),
        new MainStreamStrategy(),
        new DiversificationPass.MainStreamPass(),
        25 /* results */,
        ImmutableSet.<String>of(),
        false /* videoOnly */);
  }
}
