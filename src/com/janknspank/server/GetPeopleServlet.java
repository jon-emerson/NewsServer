package com.janknspank.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Entity;

@AuthenticationRequired
public class GetPeopleServlet extends StandardServlet {

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

    Iterable<Entity> people; 

    if (searchString != null) {
      String whereCondition = "%" + StringUtils.capitalize(searchString) + "%";
      people = Database.with(Entity.class).get(
          new QueryOption.WhereLike("keyword", whereCondition),
          new QueryOption.WhereEquals("type", "p"),
          new QueryOption.Limit(20));
    } else {
      people = Database.with(Entity.class).get(new QueryOption.WhereEquals("type", "p"), 
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
