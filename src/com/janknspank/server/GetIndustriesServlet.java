package com.janknspank.server;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.janknspank.classifier.Feature;
import com.janknspank.classifier.FeatureType;
import com.janknspank.database.DatabaseSchemaException;

/**
 * Returns a complete list of industry features that we currently support.  This
 * is not a list of LinkedIn industries - that's too big for users to wrap their
 * heads around!
 */
@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_industries")
public class GetIndustriesServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException {
    String searchString = getParameter(req, "contains");

    ArrayList<Feature> matchingFeatures = Lists.newArrayList();
    searchString = searchString.toLowerCase();
    for (Feature feature : Feature.getAllFeatures()) {
      if (feature.getFeatureId().getFeatureType() == FeatureType.INDUSTRY) {
        // Put prefix matches at the front, other matches at the back.
        if (!Strings.isNullOrEmpty(searchString)
            && feature.getDescription().toLowerCase().startsWith(searchString)) {
          matchingFeatures.add(0, feature);
        } else if (Strings.isNullOrEmpty(searchString)
            || feature.getDescription().toLowerCase().contains(searchString)) {
          matchingFeatures.add(feature);
        }
      }
    }

    JSONArray results = new JSONArray();
    for (Feature feature : matchingFeatures) {
      JSONObject industryJSON = new JSONObject();
      industryJSON.put("keyword", feature.getDescription());
      industryJSON.put("code", feature.getFeatureId().getId());
      industryJSON.put("id", feature.getFeatureId().getId());
      results.put(industryJSON);
    }

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", results);
    return response;
  }
}
