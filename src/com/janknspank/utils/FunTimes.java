package com.janknspank.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.janknspank.bizness.Entities;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.KeywordToEntityId;

public class FunTimes {
  private static final Set<String> KEYWORD_BLACKLIST = ImmutableSet.of(
      "us", "bbc", "read more", "reuters", "the associated press", "uk", "new york times",
      "associated press", "it", "washington post", "american", "the new york times",
      "the washington post", "office", "los angeles times", "eu", "the huffington post",
      "ap", "legislature", "defense", "superior court", "search", "city hall", "bush", "mr",
      "sorry", "brent", "mac", "ph. d.", "alice", "march", "joe",
      "homepage tested reviews cameras lenses accessories", "david", "clintons", "fox", "bill",
      "by", "george", "ms", "steve", "honestly", "paul");

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
      for (Entry<String> keywordEntry : keywordCounts.entrySet()) {
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
      for (Entry<FeatureId> featureIdEntry : featureIdSet.entrySet()) {
        topList.add(featureIdEntry.getElement(), featureIdEntry.getCount());
      }
      return topList.getKeys();
    }
  }

  public static void main(String args[]) throws Exception {
    System.out.println("Reading articles...");
    Iterable<Article> articles = Database.with(Article.class).get();
    System.out.println(Iterables.size(articles) + " articles found.");

    // Here's where we'll keep all the companies, people, places that we find in
    // articles.
    Map<EntityType, KeywordSet> masterMap = Maps.newHashMap();
    masterMap.put(EntityType.ORGANIZATION, new KeywordSet());
    masterMap.put(EntityType.PERSON, new KeywordSet());
    masterMap.put(EntityType.PLACE, new KeywordSet());

    for (Article article : articles) {
      for (ArticleKeyword keyword : article.getKeywordList()) {
        if (KEYWORD_BLACKLIST.contains(keyword.getKeyword().toLowerCase())) {
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

    System.out.println("Top people:");
    KeywordSet peopleKeywordSet = masterMap.get(EntityType.PERSON);
    TopList<String, Integer> topPeople = peopleKeywordSet.getTopList(50000);
    List<KeywordToEntityId> keywordToEntityIdsToInsert = Lists.newArrayList();
    int i = 0;
    for (String person : topPeople) {
      if (topPeople.getValue(person) == 1) {
        break;
      }
      i++;
      KeywordToEntityId.Builder keywordToEntityIdBuilder = KeywordToEntityId.newBuilder()
          .setId(GuidFactory.generate())
          .setType(EntityType.PERSON.toString())
          .setKeyword(person)
          .setCount(topPeople.getValue(person));
      List<FeatureId> topIndustryFeatureIds = peopleKeywordSet.getTopFeatures(person);
      if (topIndustryFeatureIds.size() >= 1) {
        keywordToEntityIdBuilder.setTopIndustryId1(topIndustryFeatureIds.get(0).getId());
      }
      if (topIndustryFeatureIds.size() >= 2) {
        keywordToEntityIdBuilder.setTopIndustryId2(topIndustryFeatureIds.get(1).getId());
      }
      if (topIndustryFeatureIds.size() >= 3) {
        keywordToEntityIdBuilder.setTopIndustryId3(topIndustryFeatureIds.get(2).getId());
      }

      Entity entity = Entities.getEntityByKeyword(person);
      if (entity != null) {
        if (EntityType.fromValue(entity.getType()).isA(EntityType.PERSON)) {
          keywordToEntityIdBuilder.setEntityId(entity.getId());
        }
      }
      if (keywordToEntityIdBuilder.getCount() >= 3
          || keywordToEntityIdBuilder.hasEntityId()) {
        keywordToEntityIdsToInsert.add(keywordToEntityIdBuilder.build());
      }

      if (keywordToEntityIdsToInsert.size() >= 1000) {
        Database.insert(keywordToEntityIdsToInsert);
        keywordToEntityIdsToInsert.clear();
        System.out.println(i);
      }
    }
    Database.insert(keywordToEntityIdsToInsert);
    System.out.println(i);

//    System.out.println("Top people:");
//    TopList<String, Integer> topPeople =
//        masterMap.get(EntityType.PERSON).getTopList(100);
//    for (String person : topPeople) {
//      System.out.println("  " + person + " -> " + topPeople.getValue(person));
//    }
//
//    System.out.println("Top places:");
//    TopList<String, Integer> topPlaces =
//        masterMap.get(EntityType.PLACE).getTopList(100);
//    for (String place : topPlaces) {
//      System.out.println("  " + place + " -> " + topPlaces.getValue(place));
//    }
  }
}
