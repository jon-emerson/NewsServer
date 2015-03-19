package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.Iterables;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Entity;

/**
 * Returns entities that match a given search query, as specified by the
 * parameter "query".
 */
@ServletMapping(urlPattern = "/v1/get_entities")
public class GetEntitiesServlet extends StandardServlet {
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, BiznessException,
      DatabaseRequestException, RequestException {
    String query = getParameter(req, "query");
    if (query == null) {
      return createSuccessResponse();
    }

    // Business logic.
    Iterable<Entity> entities = Iterables.concat(
        Database.with(Entity.class).get(
            new QueryOption.WhereEquals("keyword", query),
            new QueryOption.AscendingSort("source"),
            new QueryOption.WhereNotEqualsNumber("source",
                Entity.Source.DBPEDIA_LONG_ABSTRACT.getNumber()),
            new QueryOption.DescendingSort("importance"),
            new QueryOption.Limit(10)),
        Database.with(Entity.class).get(
            new QueryOption.WhereLike("keyword", query + "%"),
            new QueryOption.AscendingSort("source"),
            new QueryOption.WhereNotEqualsNumber("source",
                Entity.Source.DBPEDIA_LONG_ABSTRACT.getNumber()),
            new QueryOption.DescendingSort("importance"),
            new QueryOption.Limit(10)));

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("entities", Serializer.toJSON(entities));
    return response;
  }
}
