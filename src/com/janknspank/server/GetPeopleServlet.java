package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Entity;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_people")
public class GetPeopleServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException {
    String searchString = getParameter(req, "contains");
    Iterable<Entity> people; 

    if (searchString != null) {
      people = Database.with(Entity.class).get(
          new QueryOption.WhereLike("keyword", searchString + "%"),
          new QueryOption.WhereEquals("type", "p"),
          new QueryOption.DescendingSort("importance"),
          new QueryOption.Limit(20));
    } else {
      people = Database.with(Entity.class).get(
          new QueryOption.WhereEquals("type", "p"),
          new QueryOption.DescendingSort("importance"),
          new QueryOption.Limit(20));
    }

    // TODO: add some kind of sorting for relevance

    JSONArray peopleJson = Serializer.toJSON(people);

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", peopleJson);
    return response;
  }
}
