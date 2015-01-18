package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.Entities;
import com.janknspank.data.EntityType;
import com.janknspank.data.GuidFactory;
import com.janknspank.data.LocalDatabase;
import com.janknspank.data.ValidationException;
import com.janknspank.interpreter.KeywordFinder;
import com.janknspank.interpreter.KeywordUtils;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Core.Entity;
import com.janknspank.proto.Core.Entity.EntityTopic;
import com.janknspank.proto.Core.Entity.EntityTopic.Context;
import com.janknspank.proto.Core.Entity.Source;
import com.janknspank.proto.Local.LongAbstract;
import com.janknspank.proto.Local.TokenToEntity;
import com.janknspank.proto.Validator;

public class GetKeywordsFromDbpediaAbstracts {
  private static LoadingCache<String, Iterable<Entity>> LOCAL_ENTITY_CACHE =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterAccess(5, TimeUnit.DAYS)
          .expireAfterWrite(0, TimeUnit.MINUTES)
          .build(new CacheLoader<String, Iterable<Entity>>() {
             public Iterable<Entity> load(String key) throws Exception {
               throw new Error("This cache does not support loads.");
             }
           });


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

  /**
   * Uses the local database to find a list of Entities that may match the
   * passed text.
   */
  private static Iterable<Entity> getPartialMatches(String text) throws DataInternalException {
    Set<String> tokens = Sets.newHashSet();
    for (String sentence : KeywordFinder.getSentences(text)) {
      for (String token : KeywordFinder.getTokens(sentence)) {
        tokens.add(KeywordUtils.cleanKeyword(token));
      }
    }
    List<Entity> entities = Lists.newArrayList();
    Set<String> tokensToFetch = Sets.newHashSet();
    for (String keyword : tokens) {
      Iterable<Entity> tokenToEntities = LOCAL_ENTITY_CACHE.getIfPresent(keyword);
      if (tokenToEntities != null) {
        Iterables.addAll(entities, tokenToEntities);
      } else {
        tokensToFetch.add(keyword);
      }
    }
    Database localDatabase = LocalDatabase.getInstance();
    Multimap<String, Entity> newTokenToEntityMap = HashMultimap.create();
    for (TokenToEntity tokenToEntity :
        localDatabase.get("token", tokensToFetch, TokenToEntity.class)) {
      if (!tokensToFetch.contains(tokenToEntity.getToken())) {
        System.out.println("GOT ENTITY W/ UNREQUESTED KEYWORD!! " + tokenToEntity.getToken());
        continue;
      }
      Entity entity = Entity.newBuilder()
          .setKeyword(tokenToEntity.getEntityKeyword())
          .setType(tokenToEntity.getEntityType())
          .build();
      entities.add(entity);
      newTokenToEntityMap.put(tokenToEntity.getToken(), entity);
    }
    for (String token : newTokenToEntityMap.keySet()) {
      LOCAL_ENTITY_CACHE.put(token, newTokenToEntityMap.get(token));
    }
    return entities;
  }

  private static Iterable<Entity> getEntities(LongAbstract longAbstract, String text)
          throws DataInternalException {
    // Find words that look like keywords, using NLP.  This helps find entities
    // that might not be in Wikipedia - Which is probably quite a few!
    List<Entity> entities = Lists.newArrayList();
    for (ArticleKeyword articleKeyword :
        KeywordFinder.findKeywords("" /* urlId */, ImmutableList.of(text))) {
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
    for (Entity entity : getPartialMatches(text)) {
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
    Map<String, Entity> entityMap = Maps.newHashMap();
    for (Entity entity : entities) {
      Entity existingEntity = entityMap.get(entity.getKeyword());
      if (existingEntity == null || existingEntity.getId() == null) {
        entityMap.put(entity.getKeyword(), entity);
      }
    }
    for (Entity entity : Entities.getEntitiesByKeyword(entityMap.keySet())) {
      if (isRelevantEntityType(EntityType.fromValue(entity.getType()))) {
        entityMap.put(entity.getKeyword(), entity);
      }
    }
    entityMap.remove(longAbstract.getTopic());
    return entityMap.values();
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
    try {
      int randomOffset = (int) (10000 * Math.random());
      PreparedStatement stmt = LocalDatabase.getInstance().prepareStatement(
          "SELECT * FROM " + Database.getTableName(LongAbstract.class) + " "
          + "LIMIT " + randomOffset + ",1");
      return LocalDatabase.createFromResultSet(stmt.executeQuery(), LongAbstract.class);
    } catch (SQLException e) {
      throw new DataInternalException("Could not read from database", e);
    }
  }

  public static void main(String args[]) {
    BufferedReader reader = null;
    try {
      // We can do n-triples or n-quads here... For some reason I downloaded quads.
      Database database = Database.getInstance();
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
          for (Entity relatedEntity : getEntities(abs, abs.getText())) {
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
          database.insert(entitiesToInsert);
          entitiesToInsert.clear();
          database.update(entitiesToUpdate);
          entitiesToUpdate.clear();
          LocalDatabase.getInstance().delete(longAbstractsToDelete);
          longAbstractsToDelete.clear();
        }

        line = reader.readLine();
      }

      // Insert the remaining stragglers.
      database.insert(entitiesToInsert);
      database.insert(entitiesToUpdate);

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
