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

    Iterable<String> organizationTypes =
        Iterables.transform(EntityType.ORGANIZATION.getAllVersions(),
            new Function<EntityType, String>() {
              @Override
              public String apply(EntityType entityType) {
                return entityType.toString();
              }
            });
    if (searchString != null) {
      orgs = Database.with(Entity.class).get(
          new QueryOption.WhereLike("keyword", searchString + "%"),
          new QueryOption.WhereEquals("type", organizationTypes),
          new QueryOption.Limit(50),
          new QueryOption.DescendingSort("importance"));
    } else {
      orgs = Database.with(Entity.class).get(
          new QueryOption.WhereEquals("type", organizationTypes),
          new QueryOption.Limit(100),
          new QueryOption.DescendingSort("importance"));
    }

    JSONArray orgsJson = Serializer.toJSON(orgs);

    // HACK(jonemerson): Return types the client doesn't crash on.
    for (int i = 0; i < orgsJson.length(); i++) {
      JSONObject o = orgsJson.getJSONObject(i);
      o.put("type", "org");
    }

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", orgsJson);
    return response;
  }
}
