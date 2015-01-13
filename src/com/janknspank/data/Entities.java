package com.janknspank.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.janknspank.proto.Core.Entity;
import com.janknspank.proto.Extensions;

public class Entities extends CacheLoader<String, Entity> {
  private static LoadingCache<String, Entity> ENTITY_CACHE =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterAccess(5, TimeUnit.DAYS)
          .expireAfterWrite(0, TimeUnit.MINUTES)
          .build(new Entities());
  public static final String TYPE_WORK = "w";
  public static final String TYPE_EVENT = "ev";
  public static final String TYPE_LOCATION = "l";
  public static final String TYPE_ORGANIZATION = "o";
  public static final String TYPE_PERSON = "p";

  private static final Map<String, String> ONTOLOGY_TO_NEWS_TYPE =
      ImmutableMap.<String, String>builder()
          .put("http://dbpedia.org/ontology/Work", TYPE_WORK)
          .put("http://dbpedia.org/ontology/Event", TYPE_EVENT)
          .put("http://dbpedia.org/ontology/Place", TYPE_LOCATION)
          .put("http://dbpedia.org/ontology/Organisation", TYPE_ORGANIZATION)
          .put("http://dbpedia.org/ontology/Person", TYPE_PERSON)
          .build();
  private static final String SELECT_BY_KEYWORD_COMMAND =
      "SELECT * FROM " + Database.getTableName(Entity.class) + " WHERE keyword=?";
  public static final int MAX_KEYWORD_LENGTH;
  static {
    int keywordLength = 0;
    for (FieldDescriptor field :
        Entity.getDefaultInstance().getDescriptorForType().getFields()) {
      if (JavaType.STRING == field.getJavaType()) {
        if ("keyword".equals(field.getName())) {
          keywordLength = field.getOptions().getExtension(Extensions.stringLength);
        }
      }
    }
    if (keywordLength == 0) {
      throw new IllegalStateException("Could not find length of keyword field");
    }
    MAX_KEYWORD_LENGTH = keywordLength;
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    BufferedReader reader = null;
    try {
      // We can do n-triples or n-quads here... For some reason I downloaded quads.
      reader = new BufferedReader(new FileReader("dbpedia/instance_types_en.nq"));
      String line = reader.readLine();
      List<Entity> entitiesToUpdate = Lists.newArrayList();
      while (line != null) {
        // Tokenize!
        String[] tokens = line.trim().split(" ");
        if (tokens.length < 3 || tokens.length > 5) {
          System.out.println("Could not parse line: " + line);
          line = reader.readLine();
          continue;
        }

        // Find the topic.
        String topic = tokens[0].substring(tokens[0].lastIndexOf("/") + 1, tokens[0].length() - 1);
        if (topic.matches(".+\\_\\_[0-9]+$")) {
          // Ignore versioned topics.  These are useless to us, they're just
          // older versions of articles that exist in other forms.
          line = reader.readLine();
          continue;
        }
        topic = StringUtils.replace(topic, "\\'", "'");
        topic = StringUtils.replace(topic, "_", " ");
        topic = URLDecoder.decode(topic, "UTF-8");
        String ontology = tokens[2].substring(1, tokens[2].length() - 1);
        if (topic.matches(".+\\([^\\)]+\\)$")) {
          // Ignore classified entities for now, since we have no way to store their
          // classification, and therefore they will only serve to confuse us.
          line = reader.readLine();
          continue;
        }

        String type = ONTOLOGY_TO_NEWS_TYPE.get(ontology);
        if (type != null && topic.length() <= MAX_KEYWORD_LENGTH) {
          Entity entity = Entity.newBuilder()
              .setId(GuidFactory.generate())
              .setKeyword(topic)
              .setType(type)
              .build();
          entitiesToUpdate.add(entity);
        }
        if (entitiesToUpdate.size() == 250) {
          System.out.println(Database.insert(entitiesToUpdate) + " rows inserted");
          entitiesToUpdate.clear();
        }
        line = reader.readLine();
      }
      System.out.println(Database.insert(entitiesToUpdate) + " rows inserted");
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  public static Entity getEntityByKeyword(String keyword) throws DataInternalException {
    try {
      Entity entity = ENTITY_CACHE.get(keyword);
      return (entity == Entity.getDefaultInstance()) ? null : entity;
    } catch (ExecutionException e) {
      if (e.getCause() instanceof DataInternalException) {
        throw (DataInternalException) e.getCause();
      } else {
        throw new DataInternalException("Execution exception: " + e.getMessage(), e);
      }
    }
  }

  /**
   * DO NOT CALL THIS DIRECTLY.
   * @see #getEntityByKeyword(String)
   */
  @Override
  public Entity load(final String keyword) throws Exception {
    try {
      PreparedStatement statement =
          Database.getConnection().prepareStatement(SELECT_BY_KEYWORD_COMMAND);
      statement.setString(1, keyword);
      Entity entity = Database.createFromResultSet(statement.executeQuery(), Entity.class);

      // Unfortunately Guava's cache won't let us return null here, so we do
      // heroics.
      return entity == null ? Entity.getDefaultInstance() : entity;

    } catch (SQLException e) {
      throw new DataInternalException("Could not select url: " + e.getMessage(), e);
    }
  }
}
