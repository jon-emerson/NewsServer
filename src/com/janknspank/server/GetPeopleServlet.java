package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.EntityType;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Entity;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_people")
public class GetPeopleServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, BiznessException {
    String searchString = getParameter(req, "query");
    Iterable<Entity> people = GetInterestsServlet.getEntities(
        searchString, GetInterestsServlet.PEOPLE_TYPES);
    JSONArray peopleJson = Serializer.toJSON(people);

    // HACK(jonemerson): Tell the client these are all of type "p" so that it
    // doesn't get confused if we have office holders, etc.
    for (int i = 0; i < peopleJson.length(); i++) {
      JSONObject o = peopleJson.getJSONObject(i);
      o.put("type", EntityType.PERSON.toString());
    }

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", peopleJson);
    response.put("query", searchString);
    return response;
  }
}
