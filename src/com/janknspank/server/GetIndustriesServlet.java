package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.classifier.IndustryCode;
import com.janknspank.database.DatabaseSchemaException;

@AuthenticationRequired
public class GetIndustriesServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException {
    String searchString = getParameter(req, "contains");

    JSONArray matchingIndustries = new JSONArray();
    if (searchString != null) {
      searchString = searchString.toLowerCase();
      for (IndustryCode industry : IndustryCode.values()) {
        if (industry.getDescription().toLowerCase().contains(searchString)) {
          JSONObject industryJSON = new JSONObject();
          industryJSON.put("keyword", industry.getDescription());
          industryJSON.put("id", industry.getId());
          matchingIndustries.put(industryJSON);
        }
      }
    } else {
      // Get all the titles
      for (IndustryCode industry : IndustryCode.values()) {
        JSONObject industryJSON = new JSONObject();
        industryJSON.put("keyword", industry.getDescription());
        industryJSON.put("id", industry.getId());
        matchingIndustries.put(industryJSON);
      }
    }

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", matchingIndustries);
    return response;
  }
}
