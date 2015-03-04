package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.bizness.Industry;
import com.janknspank.database.DatabaseSchemaException;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_industries")
public class GetIndustriesServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException {
    String searchString = getParameter(req, "contains");

    JSONArray matchingIndustries = new JSONArray();
    if (searchString != null) {
      searchString = searchString.toLowerCase();
      for (Industry industry : Industry.values()) {
        if (industry.getName().toLowerCase().contains(searchString)) {
          JSONObject industryJSON = new JSONObject();
          industryJSON.put("keyword", industry.getName());
          industryJSON.put("code", industry.getCode());
          matchingIndustries.put(industryJSON);
        }
      }
    } else {
      // Get all the titles
      for (Industry industry : Industry.values()) {
        JSONObject industryJSON = new JSONObject();
        industryJSON.put("keyword", industry.getName());
        industryJSON.put("code", industry.getCode());
        matchingIndustries.put(industryJSON);
      }
    }

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", matchingIndustries);
    return response;
  }
}
