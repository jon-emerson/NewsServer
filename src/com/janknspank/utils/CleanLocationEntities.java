package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import com.google.api.client.util.Lists;
import com.janknspank.bizness.EntityType;
import com.janknspank.database.Database;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Entity;

/**
 * One-time utility for detecting location entities that are mistyped as
 * organizations, people, companies, etc.
 */
public class CleanLocationEntities {
  private static class KeywordCallable implements Callable<Void> {
    private final String keyword;

    public KeywordCallable(String keyword) {
      this.keyword = keyword;
    }

    @Override
    public Void call() throws Exception {
      Entity bestEntity = null;
      List<Entity> entitiesToDelete = Lists.newArrayList();
      for (Entity entity : Database.with(Entity.class).get(
          new QueryOption.WhereEquals("keyword", keyword))) {
        boolean isBest = false;
        if (bestEntity == null) {
          isBest = true;
        } else if (entity.getImportance() != bestEntity.getImportance()) {
          isBest = entity.getImportance() > bestEntity.getImportance();
        } else if (entity.getTopicCount() != bestEntity.getTopicCount()) {
          isBest = entity.getTopicCount() > bestEntity.getTopicCount();
        } else {
          EntityType entityType = EntityType.fromValue(entity.getType());
          EntityType bestEntityType = EntityType.fromValue(bestEntity.getType());
          if (entityType != null && bestEntityType != null) {
            isBest = entityType.getDepth() > bestEntityType.getDepth();
          }
        }
        if (isBest) {
          if (bestEntity != null) {
            // Delete the old best, before reassinging best to the current.
            entitiesToDelete.add(bestEntity);
          }
          bestEntity = entity;
        } else {
          entitiesToDelete.add(entity);
        }
      }
      Database.delete(entitiesToDelete);
      System.out.print(entitiesToDelete.isEmpty() ? "x" : ".");
      return null;
    }
  }

  public static void main(String args[]) throws Exception {
    List<KeywordCallable> callables = Lists.newArrayList();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader("out.txt"));
      String line;
      while ((line = reader.readLine()) != null) {
        callables.add(new KeywordCallable(line.split("\t")[0]));
      }
    } finally {
      IOUtils.closeQuietly(reader);
    }

    ExecutorService executor = Executors.newFixedThreadPool(100);
    executor.invokeAll(callables);
    executor.shutdown();
    executor.awaitTermination(600, TimeUnit.MINUTES);
  }
}
