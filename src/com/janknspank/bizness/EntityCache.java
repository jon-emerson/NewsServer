package com.janknspank.bizness;

import java.util.LinkedHashMap;
import java.util.Map;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Entity;

public class EntityCache {
  private static final LinkedHashMap<String, Entity> ENTITY_CACHE_MAP =
      new LinkedHashMap<String, Entity>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entity> eldest) {
          return size() > 2500;
        }
      };
  private static final Object SYNCHRONIZE_HELPER = new Object();

  public static Entity getEntity(String entityId) {
    if (!ENTITY_CACHE_MAP.containsKey(entityId)) {
      synchronized (SYNCHRONIZE_HELPER) {
        if (!ENTITY_CACHE_MAP.containsKey(entityId)) {
          try {
            ENTITY_CACHE_MAP.put(entityId,
                Database.with(Entity.class).getFirst(new QueryOption.WhereEquals("id", entityId)));
          } catch (DatabaseSchemaException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return ENTITY_CACHE_MAP.get(entityId);
  }
}
