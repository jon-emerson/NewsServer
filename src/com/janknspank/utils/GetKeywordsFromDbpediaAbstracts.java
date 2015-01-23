package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.Entities;
import com.janknspank.data.EntityType;
import com.janknspank.data.GuidFactory;
import com.janknspank.data.QueryOption;
import com.janknspank.data.ValidationException;
import com.janknspank.interpreter.KeywordFinder;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Core.Entity;
import com.janknspank.proto.Core.Entity.EntityTopic;
import com.janknspank.proto.Core.Entity.EntityTopic.Context;
import com.janknspank.proto.Core.Entity.Source;
import com.janknspank.proto.Local.LongAbstract;
import com.janknspank.proto.Validator;

public class GetKeywordsFromDbpediaAbstracts {
  public static boolean isRelevantEntityType(EntityType type) {
    if (type == null) {
      return false;
    }
    if (type.isA(EntityType.POPULATED_PLACE)) {
      return type != EntityType.SETTLEMENT;
    }
    if (type.isA(EntityType.BOOK) ||
        type.isA(EntityType.WRITER) ||
        type.isA(EntityType.SOFTWARE) ||
        type.isA(EntityType.WEBSITE) ||
        type.isA(EntityType.PERIODICAL_LITERATURE)) { // Magazines, newspapers, etc.
      return true;
    }
    return !type.isA(EntityType.ARTIST) &&
        !type.isA(EntityType.BAND) &&
        !type.isA(EntityType.WORK) &&
        !type.isA(EntityType.POPULATED_PLACE);
  }

  private static Iterable<Entity> getPartialMatches(Multimap<String, Entity> entityMap, String text)
      throws DataInternalException {
    Set<Entity> entities = Sets.newHashSet();
    for (String sentence : KeywordFinder.getSentences(text)) {
      for (String token : KeywordFinder.getTokens(sentence)) {
        entities.addAll(entityMap.get(token));
      }
    }
    return entities;
  }

  private static Iterable<Entity> getEntities(
      Multimap<String, Entity> entityMap, LongAbstract longAbstract, String text)
      throws DataInternalException {
    // Find words that look like keywords, using NLP.  This helps find entities
    // that might not be in Wikipedia - Which is probably quite a few!
    List<Entity> entities = Lists.newArrayList();
    for (ArticleKeyword articleKeyword :
        KeywordFinder.findParagraphKeywords("" /* urlId */, ImmutableList.of(text))) {
      if (!articleKeyword.getKeyword().contains(".")) {
        entities.add(Entity.newBuilder()
            .setKeyword(articleKeyword.getKeyword())
            .setType(articleKeyword.getType())
            .setSource(Source.DBPEDIA_LONG_ABSTRACT)
            .build());
      }
    }

    // Find Wikipedia keyword matches against the actual text.
    Set<Entity> wikipediaMatches = Sets.newHashSet();
    for (Entity entity : getPartialMatches(entityMap, text)) {
      if (text.contains(entity.getKeyword())) {
        for (EntityTopic entityTopic : entity.getTopicList()) {
          if (entityTopic.getContext() == Context.WIKIPEDIA_SUBTOPIC) {
            // Make sure the article / text is actually about this subtopic,
            // and doesn't just contain this text with no other relationship.
            if (!text.contains(entityTopic.getKeyword())) {
              continue;
            }
          }
        }
        wikipediaMatches.add(entity);
      }
    }

    // Make sure no Wikipedia keyword matches are word subsets of each other.
    // (E.g. for Christmas Eve, only take Christmas Eve, not Eve too.)
    Set<Entity> smallWikipediaMatches = Sets.newHashSet();
    for (Entity bigEntity : wikipediaMatches) {
      for (Entity littleEntity : wikipediaMatches) {
        if (bigEntity.getKeyword().length() > littleEntity.getKeyword().length() &&
            bigEntity.getKeyword().contains(littleEntity.getKeyword())) {
          smallWikipediaMatches.add(littleEntity);
        }
      }
    }
    wikipediaMatches.removeAll(smallWikipediaMatches);

    entities.addAll(wikipediaMatches);
    return canonicalize(longAbstract, entities);
  }

  /**
   * De-dupes and finds entity_ids for each given Entity, so that we can write
   * fully-qualified data to the DB.
   */
  private static Iterable<Entity> canonicalize(LongAbstract longAbstract, List<Entity> entities)
      throws DataInternalException {
    Map<String, Entity> topicToEntityMap = Maps.newHashMap();
    for (Entity entity : entities) {
      Entity existingEntity = topicToEntityMap.get(entity.getKeyword());
      if (existingEntity == null || existingEntity.getId() == null) {
        topicToEntityMap.put(entity.getKeyword(), entity);
      }
    }
    topicToEntityMap.remove(longAbstract.getTopic());
    for (Entity entity : Entities.getEntitiesByKeyword(topicToEntityMap.keySet())) {
      if (isRelevantEntityType(EntityType.fromValue(entity.getType()))) {
        topicToEntityMap.put(entity.getKeyword(), entity);
      }
    }
    return topicToEntityMap.values();
  }

  /**
   * Have we already NLP'ed topics for this Entity already?
   */
  private static boolean entityHasTopicsAlready(Entity entity) {
    for (EntityTopic topic : entity.getTopicList()) {
      if (!topic.hasContext() || topic.getContext() != Context.WIKIPEDIA_SUBTOPIC) {
        return true;
      }
    }
    return false;
  }

  public static LongAbstract getNextLongAbstract() throws DataInternalException {
    return Database.with(LongAbstract.class).getFirst(
        new QueryOption.LimitWithOffset(1, (int) (10000 * Math.random())));
  }

  /**
   * This is a map from every word in relevant entities to the Entities
   * themselves.  This means that a consume can tokenize all their text into
   * tokens, then use this map to find if there's any potential maps
   * efficiently, before doing the less efficient verification of each token
   * (but on a much smaller data set).
   */
  public static Multimap<String, Entity> getEntityMap() throws IOException {
    Multimap<String, Entity> entityMap = HashMultimap.create();
    BufferedReader reader = null;
    try {
      // We can do n-triples or n-quads here... For some reason I downloaded
      // quads.
      reader = new BufferedReader(new FileReader("dbpedia/instance_types_en.nt"));
      String line = reader.readLine();
      DbpediaInstanceType currentInstanceType = new DbpediaInstanceType();
      while (line != null) {
        if (line.startsWith("#")) {
          line = reader.readLine();
          continue;
        }
        DbpediaInstanceTypeLine instanceTypeLine = new DbpediaInstanceTypeLine(line);
        if (instanceTypeLine.isValid()) {
          if (!currentInstanceType.isLineRelevant(instanceTypeLine)) {
            if (currentInstanceType.isValuableEntity()) {
              String topicStr = currentInstanceType.getTopic();
              EntityType type = currentInstanceType.getEntityType();
              if (isRelevantEntityType(type) &&
                  topicStr.length() <= Entities.MAX_KEYWORD_LENGTH) {
                Entity entity = Entity.newBuilder()
                    .setKeyword(topicStr)
                    .setType(type.toString())
                    .build();
                for (String keywordToken : topicStr.split("\\s+")) {
                  entityMap.put(keywordToken, entity);
                }
              }
            }
            currentInstanceType = new DbpediaInstanceType();
          }
          currentInstanceType.addLine(instanceTypeLine);
        }
        line = reader.readLine();
      }
      // There's an off-by-1 error here: We lose the last entity.
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
    return entityMap;
  }

  public static void main(String args[]) {
    System.out.println("Reading entities...");
    Multimap<String, Entity> entityMap;
    try {
      entityMap = getEntityMap();
    } catch (IOException e) {
      throw new Error(e);
    }
    System.out.println("Starting process...");

    BufferedReader reader = null;
    try {
      // We can do n-triples or n-quads here... For some reason I downloaded quads.
      List<Entity> entitiesToUpdate = Lists.newArrayList();
      List<Entity> entitiesToInsert = Lists.newArrayList();
      List<LongAbstract> longAbstractsToDelete = Lists.newArrayList();

      reader = new BufferedReader(new FileReader("dbpedia/long_abstracts_en.nq"));
      String line = reader.readLine();
      while (line != null) {
        if (line.startsWith("#")) {
          line = reader.readLine();
          continue;
        }

        try {
          LongAbstract abs = getNextLongAbstract();
          Entity entity = Entities.getEntityByKeyword(abs.getTopic());
          if (entity == null) {
            longAbstractsToDelete.add(abs);
            line = reader.readLine();
            continue;
          } else if (entityHasTopicsAlready(entity)) {
            System.out.println("Skipping already-topiced entity: " + entity.getKeyword());
            longAbstractsToDelete.add(abs);
            line = reader.readLine();
            continue;
          }

          Entity.Builder entityBuilder = entity.toBuilder();
          for (Entity relatedEntity : getEntities(entityMap, abs, abs.getText())) {
            // Give entities without an ID an ID, add them to entities to insert
            if (!relatedEntity.hasId()) {
              relatedEntity = relatedEntity.toBuilder()
                  .setId(GuidFactory.generate())
                  .build();
              Validator.assertValid(relatedEntity);
              entitiesToInsert.add(relatedEntity);
            }

            // Construct a EntityTopic from this entity.
            entityBuilder.addTopic(EntityTopic.newBuilder()
                .setEntityId(relatedEntity.getId())
                .setKeyword(relatedEntity.getKeyword())
                .setStrength(1)
                .setType(relatedEntity.getType()));
          }

          Entity updatedEntity = entityBuilder.build();
          Validator.assertValid(updatedEntity);
          entitiesToUpdate.add(updatedEntity);
          longAbstractsToDelete.add(abs);

        } catch (DataInternalException | ValidationException e) {
          e.printStackTrace();
        }

        // If we have enough entities to insert or update, do the deed.
        if (entitiesToInsert.size() > 100 || entitiesToUpdate.size() > 100) {
          System.out.println("Writing up through keyword: " + entitiesToUpdate.get(0).getKeyword());
          Database.insert(entitiesToInsert);
          entitiesToInsert.clear();
          Database.update(entitiesToUpdate);
          entitiesToUpdate.clear();
          Database.delete(longAbstractsToDelete);
          longAbstractsToDelete.clear();
        }

        line = reader.readLine();
      }

      // Insert the remaining stragglers.
      Database.insert(entitiesToInsert);
      Database.insert(entitiesToUpdate);

    } catch (IOException | ValidationException | DataInternalException e) {
      e.printStackTrace();

    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
