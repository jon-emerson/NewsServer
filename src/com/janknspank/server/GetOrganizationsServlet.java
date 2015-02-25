package com.janknspank.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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
public class GetOrganizationsServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, RequestException {
    String searchString;
    try {
      searchString = URLDecoder.decode(getParameter(req, "contains"), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new RequestException("Unable to decode 'contains' parameter: " + e.getMessage(), e);
    }

    Iterable<Entity> orgs; 

    if (searchString != null) {
      orgs = Database.with(Entity.class).get(
          new QueryOption.WhereLikeIgnoreCase("keyword", searchString + "%"),
          new QueryOption.WhereEquals("type", "org"),
          new QueryOption.Limit(20));
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
