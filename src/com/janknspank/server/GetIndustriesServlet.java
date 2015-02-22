package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.database.DatabaseSchemaException;

@AuthenticationRequired
public class GetIndustriesServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException {
    String searchString = getParameter(req, "contains");

    Iterable<FeatureId> allIndustries = FeatureId.getByType(FeatureType.INDUSTRY);
    JSONArray matchingIndustries = new JSONArray();
    if (searchString != null) {
      searchString = searchString.toLowerCase();
      for (FeatureId industry : allIndustries) {
        if (industry.getTitle().toLowerCase().contains(searchString)) {
          JSONObject industryJSON = new JSONObject();
          industryJSON.put("keyword", industry.getTitle());
          matchingIndustries.put(industryJSON);
        }
      }
    } else {
      // Get all the titles
      for (FeatureId industry : allIndustries) {
        JSONObject industryJSON = new JSONObject();
        industryJSON.put("keyword", industry.getTitle());
        matchingIndustries.put(industryJSON);
      }
    }
    
    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", matchingIndustries);
    return response;
  }
}
