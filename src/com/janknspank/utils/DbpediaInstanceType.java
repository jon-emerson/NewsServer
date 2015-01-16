package com.janknspank.utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.janknspank.data.Entities;
import com.janknspank.data.EntityType;
import com.janknspank.data.GuidFactory;
import com.janknspank.proto.Core.Entity;
import com.janknspank.proto.Core.Entity.EntityTopic;
import com.janknspank.proto.Core.Entity.Source;
import com.janknspank.proto.Core.Entity.EntityTopic.Context;

public class DbpediaInstanceType {
  private List<DbpediaInstanceTypeLine> lines = Lists.newArrayList();
  private EntityType entityType = null;
  private String topic = null;
  private String subtopic = null;

  public DbpediaInstanceType() {
  }

  public void addLine(DbpediaInstanceTypeLine line) {
    lines.add(line);
    if (topic == null) {
      topic = line.getTopic();
      subtopic = line.getSubtopic();
    }

    // Collect the most specific type we can get.
    EntityType lineEntityType = line.getEntityType();
    if (lineEntityType != null &&
        (entityType == null || entityType.getDepth() < lineEntityType.getDepth())) {
      entityType = lineEntityType;
    }
  }

  public boolean isLineRelevant(DbpediaInstanceTypeLine line) {
    return (topic == null) ||
        (StringUtils.equals(topic, line.getTopic()) &&
         StringUtils.equals(subtopic, line.getSubtopic()));
  }

  public String getTopic() {
    return topic;
  }

  public String getSubtopic() {
    return subtopic;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public Entity createEntity() {
    Entity.Builder entityBuilder = Entity.newBuilder()
        .setId(GuidFactory.generate())
        .setKeyword(topic)
        .setType(entityType.toString())
        .setSource(Source.DBPEDIA_INSTANCE_TYPE);
    if (subtopic != null && subtopic.length() < Entities.MAX_TOPIC_KEYWORD_LENGTH) {
      entityBuilder.addTopic(EntityTopic.newBuilder()
          .setKeyword(subtopic)
          .setContext(Context.WIKIPEDIA_SUBTOPIC)
          .setStrength(100));
    }
    return entityBuilder.build();
  }

  public boolean isValuableEntity() {
    // We can't store items whose titles / subtitles are too long.
    if (topic.length() > Entities.MAX_KEYWORD_LENGTH ||
        (subtopic != null && subtopic.length() < Entities.MAX_TOPIC_KEYWORD_LENGTH)) {
      return false;
    }

    // We only care about people, companies, events, holidays, and creative
    // works. (E.g. not atomic elements, time periods, mathematical theorems,
    // etc.)
    return (entityType != null) &&
        (entityType.isA(EntityType.ORGANIZATION) ||
         entityType.isA(EntityType.PERSON) ||
         entityType.isA(EntityType.EVENT) ||
         entityType.isA(EntityType.PLACE) ||
         entityType.isA(EntityType.WORK) ||
         entityType.isA(EntityType.HOLIDAY));
  }
}
