package com.janknspank.bizness;

import java.util.List;
import java.util.Set;

import com.google.common.base.Predicates;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.Entity.EntityTopic;

public class Entities extends CacheLoader<String, Entity> {
  public static final int MAX_KEYWORD_LENGTH =
      Database.getStringLength(Entity.class, "keyword");
  public static final int MAX_TOPIC_KEYWORD_LENGTH =
      Database.getStringLength(EntityTopic.class, "keyword");

  public static Entity getEntityByKeyword(String keyword) throws DatabaseSchemaException {
    return Iterables.getFirst(getEntitiesByKeyword(ImmutableList.of(keyword)), null);
  }

  public static Entity getEntityById(String id) throws DatabaseSchemaException {
    return Database.with(Entity.class).getFirst(new QueryOption.WhereEquals("id", id));
  }

  public static Iterable<Entity> getEntitiesByKeyword(Iterable<String> keywords)
      throws DatabaseSchemaException {
    List<Entity> entities = Lists.newArrayList();
    Set<String> keywordsToFetch = ImmutableSet.copyOf(keywords);
    for (Entity entity : Database.with(Entity.class).get(
        new QueryOption.WhereEquals("keyword", keywordsToFetch))) {
      // Make sure the keyword we got matches the case of what we're looking
      // for.  This guards against "FloRida" the musical artist being matched
      // when an article is actually about "Florida" the state.
      if (keywordsToFetch.contains(entity.getKeyword())) {
        entities.add(entity);
      }
    }
    return Iterables.filter(entities, Predicates.not(Predicates.isNull()));
  }

  /**
   * DO NOT CALL THIS DIRECTLY.
   * @see #getEntityByKeyword(String)
   */
  @Override
  public Entity load(final String keyword) throws Exception {
    return Database.with(Entity.class).getFirst(
        new QueryOption.WhereEquals("keyword", keyword));
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    // Database.with(Entity.class).createTable();
    Iterable<Entity> staleEntities = Database.with(Entity.class).get(
        new QueryOption.WhereNull("old_id"),
        new QueryOption.Limit(1000));
    int i = 0;
    while (!Iterables.isEmpty(staleEntities)) {
      List<Entity> entitiesToInsert = Lists.newArrayList();
      for (Entity entity : staleEntities) {
        entitiesToInsert.add(entity.toBuilder()
            .setOldId(entity.getId())
            .setId(GuidFactory.generate())
            .build());
      }
      System.out.print(".");
      if (++i % 20000 == 0) {
        System.out.println(i);
      }
      Database.insert(entitiesToInsert);
      Database.delete(staleEntities);
      staleEntities = Database.with(Entity.class).get(
          new QueryOption.WhereNull("old_id"),
          new QueryOption.Limit(1000));
    }
  }
}
