package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.EntityType;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Entity;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_organizations")
public class GetOrganizationsServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException {
    String searchString  = getParameter(req, "contains");
    Iterable<Entity> orgs; 

    if (searchString != null) {
      orgs = Database.with(Entity.class).get(
          new QueryOption.WhereLike("keyword", searchString + "%"),
          new QueryOption.WhereEquals("type", Iterables.transform(EntityType.ORGANIZATION.getAllVersions(),
              new Function<EntityType, String>() {
                @Override
                public String apply(EntityType entityType) {
                  return entityType.toString();
                }
              })),
          new QueryOption.Limit(50));
    } else {
      orgs = Database.with(Entity.class).get(new QueryOption.WhereEquals("type", "org"), 
          new QueryOption.Limit(20));
    }

    JSONArray orgsJson = Serializer.toJSON(orgs);

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", orgsJson);
    return response;
  }
}
