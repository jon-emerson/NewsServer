package com.janknspank.bizness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.api.client.util.Maps;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.ExploreTopic;
import com.janknspank.proto.UserProto.User;
import com.janknspank.server.ArticleSerializer;

public class ExploreTopics {
  /**
   * Returns a set of topics the user might want to explore after viewing the
   * passed set of articles.  We grab the 5 most mentioned entities that the
   * user's not following, and the 3 most applicable industries that the user's
   * not following, randomize them, and return 5 of what's left.
   */
  public static Iterable<ExploreTopic> get(
      Iterable<Article> articles,
      User user,
      @Nullable Entity queriedEntity,
      @Nullable Integer queriedIndustryCode) {
    List<ExploreTopic> exploreTopics = Lists.newArrayList();

    // Get the best entities that the user's not already following and that the
    // user didn't explicitly query for already (e.g. the "Barack Obama" stream
    // shouldn't suggest exploring Barack Obama).
    Set<String> userKeywordSet = ArticleSerializer.getUserKeywordSet(user, false, false);
    Map<String, String> keywordToEntityIdMap = Maps.newHashMap();
    Multiset<String> topKeywords = HashMultiset.create();
    for (Article article : articles) {
      for (ArticleKeyword articleKeyword : article.getKeywordList()) {
        if (articleKeyword.hasEntity()
            && (queriedEntity == null
                || !queriedEntity.getId().equals(articleKeyword.getEntity().getId()))
            && !userKeywordSet.contains(articleKeyword.getKeyword().toLowerCase())) {
          keywordToEntityIdMap.put(articleKeyword.getKeyword(), articleKeyword.getEntity().getId());
          topKeywords.add(articleKeyword.getKeyword());
        }
      }
    }
    for (String keyword :
        Iterables.limit(Multisets.copyHighestCountFirst(topKeywords).elementSet(), 5)) {
      exploreTopics.add(ExploreTopic.newBuilder()
          .setKeyword(keyword)
          .setEntityId(keywordToEntityIdMap.get(keyword))
          .build());
    }

    // Get the best industries that the user's not already following.
    Set<FeatureId> userIndustryFeatureIdSet =
        ImmutableSet.copyOf(UserInterests.getUserIndustryFeatureIds(user));
    Multiset<FeatureId> topIndustries = HashMultiset.create();
    for (Article article : articles) {
      for (ArticleFeature articleFeature : article.getFeatureList()) {
        FeatureId featureId = FeatureId.fromId(articleFeature.getFeatureId());
        if (featureId != null
            && featureId.getFeatureType() == FeatureType.INDUSTRY
            && !new Integer(featureId.getId()).equals(queriedIndustryCode)  // May be null.
            && !userIndustryFeatureIdSet.contains(featureId)) {
          topIndustries.add(featureId);
        }
      }
    }
    for (FeatureId featureId :
        Iterables.limit(Multisets.copyHighestCountFirst(topIndustries).elementSet(), 3)) {
      exploreTopics.add(ExploreTopic.newBuilder()
          .setKeyword(featureId.getTitle())
          .setIndustryCode(featureId.getId())
          .build());
    }

    Collections.shuffle(exploreTopics);
    return Iterables.limit(exploreTopics, 5);
  }

}
