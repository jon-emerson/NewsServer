package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

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
import com.janknspank.interpreter.KeywordFinder;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Core.Entity;
import com.janknspank.proto.Core.Entity.EntityTopic;
import com.janknspank.proto.Core.Entity.EntityTopic.Context;
import com.janknspank.proto.Core.Entity.Source;

public class GetKeywordsFromDbpediaAbstracts {
  public static class Abstract {
    private final List<String> tokens = Lists.newArrayList();

    public Abstract(String nqLine) {
      StringBuffer currentToken = new StringBuffer();
      boolean inBlock = false;
      boolean inQuote = false;
      for (int i = 0; i < nqLine.length(); i++) {
        char c = nqLine.charAt(i);
        if (inBlock && c == '>') {
          inBlock = false;
          tokens.add(currentToken.toString());
          currentToken.setLength(0);
          continue;
        }
        if (inQuote && c == '"') {
          inQuote = false;
          tokens.add(StringEscapeUtils.unescapeJava(currentToken.toString()));
          currentToken.setLength(0);
          while (i < nqLine.length() && nqLine.charAt(i) != ' ') {
            i++; // Skip the @en part.
          }
          continue;
        }
        if (!inBlock && !inQuote && c == '<') {
          inBlock = true;
          continue;
        }
        if (!inBlock && !inQuote && c == '"') {
          inQuote = true;
          continue;
        }
        if (inBlock || inQuote) {
          currentToken.append(c);
        }
      }
      if (currentToken.length() > 0) {
        tokens.add(currentToken.toString());
      }
    }

    public String getArticleName() {
      if (tokens.size() == 0 || tokens.get(0).length() == 0) {
        return null;
      }
      String token = tokens.get(0);
      return token.substring(token.lastIndexOf("/") + 1);
    }

    public String getTopic() {
      try {
        String rawTopic = StringUtils.replace(getArticleName(), "\\'", "'");
        rawTopic = StringUtils.replace(rawTopic, "_", " ");
        rawTopic = URLDecoder.decode(rawTopic, "UTF-8");
        Matcher subtopicMatcher = DbpediaInstanceTypeLine.SUBTOPIC_PATTERN.matcher(rawTopic);
        return (subtopicMatcher.matches())
            ? subtopicMatcher.group(1).trim()
            : rawTopic.trim();
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
   }

    public String getText() {
      return (tokens.size() < 3) ? null : tokens.get(2);
    }
  }

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
   * This is a map from every word in relevant entities to the Entities
   * themselves.  This means that a consume can tokenize all their text into
   * tokens, then use this map to find if there's any potential maps
   * efficiently, before doing the less efficient verification of each token
   * (but on a much smaller data set).
   */
  public static Multimap<String, Entity> getEntityMap() throws Exception {
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

  private static Iterable<Entity> getEntities(
      Abstract abs, Multimap<String, Entity> entityMap, String text)
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
    Set<Entity> partialMatches = Sets.newHashSet();
    Set<Entity> wikipediaMatches = Sets.newHashSet();
    for (String sentence : KeywordFinder.getSentences(text)) {
      for (String token : KeywordFinder.getTokens(sentence)) {
        partialMatches.addAll(entityMap.get(token));
      }
    }
    for (Entity entity : partialMatches) {
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
    return canonicalize(abs, entities);
  }

  /**
   * De-dupes and finds entity_ids for each given Entity, so that we can write
   * fully-qualified data to the DB.
   */
  private static Iterable<Entity> canonicalize(Abstract abs, List<Entity> entities)
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
    entityMap.remove(abs.getTopic());
    return entityMap.values();
  }

  public static void main(String args[]) throws Exception {
    System.out.println("Reading entities...");
    Multimap<String, Entity> entityMap = getEntityMap();
    System.out.println("Starting process...");

    BufferedReader reader = null;
    try {
      // We can do n-triples or n-quads here... For some reason I downloaded quads.
      reader = new BufferedReader(new FileReader("dbpedia/long_abstracts_en.nq"));
      String line = reader.readLine();
      List<Entity> entitiesToUpdate = Lists.newArrayList();
      List<Entity> entitiesToInsert = Lists.newArrayList();
      while (line != null) {
        if (line.startsWith("#")) {
          line = reader.readLine();
          continue;
        }
        Abstract abs = new Abstract(line);
        Entity entity = Entities.getEntityByKeyword(abs.getTopic());
        if (entity == null) {
          line = reader.readLine();
          continue;
        }

        Entity.Builder entityBuilder = entity.toBuilder();
        for (Entity relatedEntity : getEntities(abs, entityMap, abs.getText())) {
          // Give entities without an ID an ID, add them to entities to insert
          if (!relatedEntity.hasId()) {
            relatedEntity = relatedEntity.toBuilder()
                .setId(GuidFactory.generate())
                .build();
            entitiesToInsert.add(relatedEntity);
          }

          // Construct a EntityTopic from this entity.
          entityBuilder.addTopic(EntityTopic.newBuilder()
              .setEntityId(relatedEntity.getId())
              .setKeyword(relatedEntity.getKeyword())
              .setStrength(1)
              .setType(relatedEntity.getType()));
        }
        entitiesToUpdate.add(entityBuilder.build());

        // If we have enough entities to insert or update, do the deed.
        if (entitiesToInsert.size() > 100 || entitiesToUpdate.size() > 100) {
          System.out.println("Writing up through topic: " + abs.getTopic());
          Database.insert(entitiesToInsert);
          entitiesToInsert.clear();
          Database.update(entitiesToUpdate);
          entitiesToUpdate.clear();
        }

        line = reader.readLine();
      }
      Database.insert(entitiesToUpdate);

    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }
}
