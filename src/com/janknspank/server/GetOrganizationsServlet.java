package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
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

    // DON'T list more than 9 types here!!  MySQL doesn't use indexes anymore
    // for non-unique keys once you go to 10 or more options.
    Iterable<String> organizationTypes = ImmutableList.of(
        EntityType.ORGANIZATION.toString(),
        EntityType.COMPANY.toString(),
        EntityType.SOFTWARE.toString());
    if (searchString != null) {
      orgs = Database.with(Entity.class).get(
          new QueryOption.WhereLike("keyword", searchString + "%"),
          new QueryOption.WhereEquals("type", organizationTypes),
          new QueryOption.WhereNotEqualsNumber("source", Entity.Source.DBPEDIA_LONG_ABSTRACT_VALUE),
          new QueryOption.Limit(50),
          new QueryOption.DescendingSort("importance"));
    } else {
      orgs = Database.with(Entity.class).get(
          new QueryOption.WhereEquals("type", organizationTypes),
          new QueryOption.WhereNotEqualsNumber("source", Entity.Source.DBPEDIA_LONG_ABSTRACT_VALUE),
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
