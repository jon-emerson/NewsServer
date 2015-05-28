package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.util.Lists;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.EntityType;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_interests")
public class GetInterestsServlet extends StandardServlet {
  // DON'T list more than 9 types here!!  MySQL doesn't use indexes anymore
  // for non-unique keys once you go to 10 or more options.
  public static final Iterable<EntityType> ORGANIZATION_TYPES = ImmutableList.of(
      EntityType.ORGANIZATION,
      EntityType.COMPANY,
      EntityType.SOFTWARE,
      EntityType.WEBSITE,
      EntityType.SPORTS_TEAM);
  public static final Iterable<EntityType> PEOPLE_TYPES = ImmutableList.of(
      EntityType.PERSON,
      EntityType.OFFICE_HOLDER,
      EntityType.ATHLETE,
      EntityType.MUSICAL_ARTIST);

  private static Iterable<String> convertEntityTypesToStrings(Iterable<EntityType> types) {
    return Iterables.transform(types, new Function<EntityType, String>() {
      @Override
      public String apply(EntityType entityType) {
        return entityType.toString();
      }
    });
  }

  public static Iterable<Entity> getEntities(
      String searchString, Iterable<EntityType> types)
      throws DatabaseSchemaException {
    Iterable<String> stringTypes = convertEntityTypesToStrings(types);
    if (searchString != null) {
      return Database.with(Entity.class).get(
          new QueryOption.WhereLike("keyword", searchString + "%"),
          new QueryOption.WhereEquals("type", stringTypes),
          new QueryOption.WhereNotEqualsNumber("source", Entity.Source.DBPEDIA_LONG_ABSTRACT_VALUE),
          new QueryOption.Limit(50),
          new QueryOption.DescendingSort("importance"));
    }
    return Database.with(Entity.class).get(
        new QueryOption.WhereEquals("type", stringTypes),
        new QueryOption.WhereNotEqualsNumber("source", Entity.Source.DBPEDIA_LONG_ABSTRACT_VALUE),
        new QueryOption.Limit(100),
        new QueryOption.DescendingSort("importance"));
  }

  public static Iterable<FeatureId> getIndustryFeatureIds(String searchString) {
    if (searchString == null) {
      return ImmutableList.<FeatureId>of();
    }

    List<FeatureId> featureIds = Lists.newArrayList();
    for (FeatureId featureId : FeatureId.getByType(FeatureType.INDUSTRY)) {
      for (String token : featureId.getTitle().split("[^\\w]")) {
        if (token.startsWith(searchString)) {
          featureIds.add(featureId);
          continue;
        }
      }
    }
    return featureIds;
  }

  private Interest toInterest(Entity entity) {
    return Interest.newBuilder()
        .setType(InterestType.ENTITY)
        .setEntity(entity)
        .build();
  }

  private Interest toInterest(FeatureId featureId) {
    return Interest.newBuilder()
        .setType(InterestType.INDUSTRY)
        .setIndustryCode(featureId.getId())
        .build();
  }

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, BiznessException {
    String searchString = getParameter(req, "query");

    JSONArray interestsJson = new JSONArray();
    for (FeatureId featureId : getIndustryFeatureIds(searchString)) {
      interestsJson.put(Serializer.toJSON(toInterest(featureId)));
    }

    Iterable<Entity> entities = getEntities(searchString,
        Iterables.concat(ORGANIZATION_TYPES, PEOPLE_TYPES));
    for (Entity entity : entities) {
      interestsJson.put(Serializer.toJSON(toInterest(entity)));
    }

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("interests", interestsJson);
    response.put("query", searchString);
    return response;
  }
}
