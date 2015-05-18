package com.janknspank.bizness;

import java.util.List;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;
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

  public static Iterable<Entity> getEntitiesByKeyword(Iterable<String> keywords)
      throws DatabaseSchemaException {
    return Database.with(Entity.class).get(
        new QueryOption.WhereEquals("keyword", keywords),
        new QueryOption.WhereNotEqualsNumber("source",
            Entity.Source.DBPEDIA_LONG_ABSTRACT.getNumber()),
        new QueryOption.DescendingSort("importance"),
        new QueryOption.AscendingSort("source"));
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
