package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_organizations")
public class GetOrganizationsServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, BiznessException {
    String searchString = getParameter(req, "query");
    JSONArray orgsJson = Serializer.toJSON(GetInterestsServlet.getEntities(
        searchString, GetInterestsServlet.ORGANIZATION_TYPES));

    // HACK(jonemerson): Return types the client doesn't crash on.
    for (int i = 0; i < orgsJson.length(); i++) {
      JSONObject o = orgsJson.getJSONObject(i);
      o.put("type", "org");
    }

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", orgsJson);
    response.put("query", searchString);
    return response;
  }
}
