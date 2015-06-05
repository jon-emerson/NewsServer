package com.janknspank.bizness;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
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
  private static class EntityCallable implements Callable<Void> {
    private final Entity entity;

    public EntityCallable(Entity entity) {
      this.entity = entity;
    }

    @Override
    public Void call() throws Exception {
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
      return null;
    }
  }

  public static void main(String args[]) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(20);
    while (true) {
      Iterable<Entity> entities = Database.with(Entity.class).get(
          new QueryOption.WhereNotTrue("relevance_pass_complete"),
          new QueryOption.AscendingSort("importance"),
          new QueryOption.Limit(200));
      if (Iterables.size(entities) == 0) {
        System.exit(0);
      }
      executor.invokeAll(ImmutableList.copyOf(
          Iterables.transform(entities, new Function<Entity, Callable<Void>>() {
            @Override
            public Callable<Void> apply(Entity entity) {
              return new EntityCallable(entity);
            }
          })));
    }
  }
}
