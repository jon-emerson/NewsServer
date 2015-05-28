package com.janknspank.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.janknspank.bizness.Entities;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.KeywordToEntityId;

/**
 * Finds all ArticleKeywords that haven't yet been associated with EntityIds
 * in the KeywordToEntityId table, and either increases their count by the
 * number of articles we currently have, or inserts a new KeywordToEntity with
 * just the existing count.
 *
 * We should run this somewhat regularly so that we're sure that keywords that
 * are referenced frequently have entities associated with them.
 */
public class PopulateKeywordToEntityIdTable {
  public static class KeywordSet {
    private final Multiset<String> keywordCounts = HashMultiset.create();
    private final Map<String, Multiset<FeatureId>> keywordIndustryFeatures = Maps.newHashMap();

    private Set<FeatureId> getIndustryFeatureIds(Article article) {
      ImmutableSet.Builder<FeatureId> setBuilder = ImmutableSet.builder();
      for (ArticleFeature feature : article.getFeatureList()) {
        FeatureId featureId = FeatureId.fromId(feature.getFeatureId());
        if (featureId.getFeatureType() == FeatureType.INDUSTRY) {
          setBuilder.add(featureId);
        }
      }
      return setBuilder.build();
    }

    public void add(ArticleKeyword keyword, Article article) {
      String keywordStr = keyword.getKeyword().toLowerCase();
      keywordCounts.add(keywordStr);
      Multiset<FeatureId> keywordFeatureIds = keywordIndustryFeatures.get(keywordStr);
      if (keywordFeatureIds == null) {
        keywordFeatureIds = HashMultiset.create();
        keywordIndustryFeatures.put(keywordStr, keywordFeatureIds);
      }
      keywordFeatureIds.addAll(getIndustryFeatureIds(article));
    }

    public TopList<String, Integer> getTopList(int size) {
      TopList<String, Integer> topList = new TopList<>(size);
      for (Multiset.Entry<String> keywordEntry : keywordCounts.entrySet()) {
        topList.add(keywordEntry.getElement(), keywordEntry.getCount());
      }
      return topList;
    }

    public List<FeatureId> getTopFeatures(String keyword) {
      TopList<FeatureId, Integer> topList = new TopList<>(3);
      Multiset<FeatureId> featureIdSet = keywordIndustryFeatures.get(keyword);
      if (featureIdSet == null) {
        return Collections.emptyList();
      }
      for (Multiset.Entry<FeatureId> featureIdEntry : featureIdSet.entrySet()) {
        topList.add(featureIdEntry.getElement(), featureIdEntry.getCount());
      }
      return topList.getKeys();
    }
  }

  private static class ExistingKeywordToEntityIds {
    private final Map<String, KeywordToEntityId> keywordMap;

    public ExistingKeywordToEntityIds(EntityType entityType) throws DatabaseSchemaException {
      ImmutableMap.Builder<String, KeywordToEntityId> keywordMapBuilder = ImmutableMap.builder();
      for (KeywordToEntityId keywordToEntityId : Database.with(KeywordToEntityId.class).get(
          new QueryOption.WhereEquals("type", entityType.toString()))) {
        keywordMapBuilder.put(keywordToEntityId.getKeyword(), keywordToEntityId);
      }
      keywordMap = keywordMapBuilder.build();
    }

    public boolean contains(String keyword) {
      return keywordMap.containsKey(keyword);
    }

    public KeywordToEntityId get(String keyword) {
      return keywordMap.get(keyword);
    }
  }

  public static void main(String args[]) throws Exception {
    System.out.println("Reading articles...");
    Iterable<Article> articles = Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(60000));
    System.out.println(Iterables.size(articles) + " articles retrieved.");

    // Here's where we'll keep all the companies, people, places that we find in
    // articles.
    Map<EntityType, KeywordSet> masterMap = Maps.newHashMap();
    masterMap.put(EntityType.ORGANIZATION, new KeywordSet());
    masterMap.put(EntityType.PERSON, new KeywordSet());
    masterMap.put(EntityType.PLACE, new KeywordSet());

    for (Article article : articles) {
      for (ArticleKeyword keyword : article.getKeywordList()) {
        if (keyword.hasEntity()) {
          // Already associated!!  Great!
          continue;
        }

        EntityType type = EntityType.fromValue(keyword.getType());
        if (type.isA(EntityType.ORGANIZATION)) {
          type = EntityType.ORGANIZATION;
        } else if (type.isA(EntityType.PERSON)) {
          type = EntityType.PERSON;
        } else if (type.isA(EntityType.PLACE)) {
          type = EntityType.PLACE;
        } else {
          continue;
        }
        KeywordSet keywordSet = masterMap.get(type);
        keywordSet.add(keyword, article);
      }
    }

    for (EntityType type : new EntityType[] { EntityType.PERSON, EntityType.ORGANIZATION }) {
      ExistingKeywordToEntityIds existingKeywordToEntityIds = new ExistingKeywordToEntityIds(type);

      System.out.println("TOP " + type.name() + ":");
      KeywordSet keywordSet = masterMap.get(type);
      TopList<String, Integer> topKeywords = keywordSet.getTopList(10000);
      List<KeywordToEntityId> keywordToEntityIdsToInsert = Lists.newArrayList();
      List<KeywordToEntityId> keywordToEntityIdsToUpdate = Lists.newArrayList();
      int i = 0;
      for (String keyword : topKeywords) {
        if (topKeywords.getValue(keyword) == 1) {
          break;
        }
        i++;

        // If we already know about this keyword, increase its count, now that
        // we've seen it more and it's still not handled.
        KeywordToEntityId.Builder keywordToEntityIdBuilder;
        boolean existing = existingKeywordToEntityIds.contains(keyword);
        if (existing) {
          KeywordToEntityId existingKeywordToEntityId = existingKeywordToEntityIds.get(keyword);
          if (existingKeywordToEntityId.hasEntityId()
              || (existingKeywordToEntityId.hasRemoved()
                  && existingKeywordToEntityId.getRemoved())) {
            // Actually, it's already been tagged with an entity, or removed,
            // so we can skip it.
            continue;
          }

          keywordToEntityIdBuilder = existingKeywordToEntityId.toBuilder()
              .setCount(existingKeywordToEntityId.getCount() + topKeywords.getValue(keyword));
        } else {
          keywordToEntityIdBuilder = KeywordToEntityId.newBuilder()
              .setId(GuidFactory.generate())
              .setType(type.toString())
              .setKeyword(keyword)
              .setCount(topKeywords.getValue(keyword));
        }

        // Update the feature IDs.
        List<FeatureId> topIndustryFeatureIds = keywordSet.getTopFeatures(keyword);
        if (topIndustryFeatureIds.size() >= 1) {
          keywordToEntityIdBuilder.setTopIndustryId1(topIndustryFeatureIds.get(0).getId());
        } else {
          keywordToEntityIdBuilder.clearTopIndustryId1();
        }
        if (topIndustryFeatureIds.size() >= 2) {
          keywordToEntityIdBuilder.setTopIndustryId2(topIndustryFeatureIds.get(1).getId());
        } else {
          keywordToEntityIdBuilder.clearTopIndustryId2();
        }
        if (topIndustryFeatureIds.size() >= 3) {
          keywordToEntityIdBuilder.setTopIndustryId3(topIndustryFeatureIds.get(2).getId());
        } else {
          keywordToEntityIdBuilder.clearTopIndustryId3();
        }

        // See if we can just trivially assign this keyword to an entity based
        // on an exact Keyword string match.
        Entity entity = Entities.getEntityByKeyword(keyword);
        if (entity != null) {
          if (EntityType.fromValue(entity.getType()).isA(EntityType.PERSON)) {
            keywordToEntityIdBuilder.setEntityId(entity.getId());
          }
        }

        // If it's an existing keyword, make sure we update it.  Else, insert
        // it, as long as we've seen it 3 or more times.
        if (existing) {
          keywordToEntityIdsToUpdate.add(keywordToEntityIdBuilder.build());
        } else if (keywordToEntityIdBuilder.getCount() >= 3
            || keywordToEntityIdBuilder.hasEntityId()) {
          keywordToEntityIdsToInsert.add(keywordToEntityIdBuilder.build());
        }

        if ((keywordToEntityIdsToInsert.size() + keywordToEntityIdsToUpdate.size()) >= 1000) {
          Database.insert(keywordToEntityIdsToInsert);
          keywordToEntityIdsToInsert.clear();
          Database.update(keywordToEntityIdsToUpdate);
          keywordToEntityIdsToUpdate.clear();
          System.out.println(i);
        }
      }
      Database.insert(keywordToEntityIdsToInsert);
      Database.update(keywordToEntityIdsToUpdate);
      System.out.println(i);
    }
  }
}
