package com.janknspank.bizness;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.database.Database;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.EntityIdToIndustryRelevance;

public class EntityIdToIndustryRelevances {
  public static void main(String args[]) throws Exception {
    while (true) {
      Entity entity = Database.with(Entity.class).getFirst(
          new QueryOption.WhereNotTrue("relevance_pass_complete"),
          new QueryOption.DescendingSort("importance"),
          new QueryOption.AscendingSort("source"),
          new QueryOption.Limit(20));
      if (entity == null) {
        break;
      }
      Map<Integer, EntityIdToIndustryRelevance.Builder> builderMap = Maps.newHashMap();
      for (Article article : Database.with(Article.class).get(
          new QueryOption.WhereEquals("keyword.entity.id", entity.getId()))) {
        for (ArticleFeature feature : article.getFeatureList()) {
          FeatureId featureId = FeatureId.fromId(feature.getFeatureId());
          if (featureId != null && featureId.getFeatureType() == FeatureType.INDUSTRY) {
            if (!builderMap.containsKey(featureId.getId())) {
              builderMap.put(featureId.getId(), EntityIdToIndustryRelevance.newBuilder()
                  .setId(GuidFactory.generate())
                  .setEntityId(entity.getId())
                  .setIndustryId(featureId.getId())
                  .setSum(0)
                  .setCount(0)
                  .setUpdatedThroughTime(0));
            }
            EntityIdToIndustryRelevance.Builder builder = builderMap.get(featureId.getId());
            builder
                .setSum(builder.getSum() + feature.getSimilarity())
                .setCount(builder.getCount() + 1)
                .setUpdatedThroughTime(
                    Math.max(article.getPublishedTime(), builder.getUpdatedThroughTime()));
          }
        }
      }
      Database.insert(Iterables.transform(builderMap.values(),
          new Function<EntityIdToIndustryRelevance.Builder, EntityIdToIndustryRelevance>() {
            @Override
            public EntityIdToIndustryRelevance apply(
                EntityIdToIndustryRelevance.Builder builder) {
              return builder.build();
            }
          }));
      Database.update(entity.toBuilder()
          .setRelevancePassComplete(true)
          .build());
      System.out.print(".");
    }
  }
}
