package com.janknspank.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.janknspank.classifier.FeatureId;
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
  private static final List<FeatureId> SORTED_INDUSTRY_FEATURE_IDS;
  static {
    List<FeatureId> featureIds = Lists.newArrayList(
        Iterables.filter(ImmutableList.copyOf(FeatureId.values()),
        new Predicate<FeatureId>() {
          @Override
          public boolean apply(FeatureId featureId) {
            return featureId.getFeatureType() == FeatureType.INDUSTRY;
          }
        }));
    featureIds.sort(new Comparator<FeatureId>() {
      @Override
      public int compare(FeatureId featureId1, FeatureId featureId2) {
        return featureId1.getTitle().compareTo(featureId2.getTitle());
      }
    });
    SORTED_INDUSTRY_FEATURE_IDS = ImmutableList.copyOf(featureIds);
  }

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException {
    String searchString = getParameter(req, "contains");

    ArrayList<FeatureId> matchingFeatureIds = Lists.newArrayList();
    searchString = Strings.isNullOrEmpty(searchString) ? null : searchString.toLowerCase();
    for (FeatureId featureId : SORTED_INDUSTRY_FEATURE_IDS) {
      // Put prefix matches at the front, other matches at the back.
      if (!Strings.isNullOrEmpty(searchString)
          && featureId.getTitle().toLowerCase().startsWith(searchString)) {
        matchingFeatureIds.add(0, featureId);
      } else if (Strings.isNullOrEmpty(searchString)
          || featureId.getTitle().toLowerCase().contains(searchString)) {
        matchingFeatureIds.add(featureId);
      }
    }

    JSONArray results = new JSONArray();
    for (FeatureId featureId : matchingFeatureIds) {
      JSONObject industryJSON = new JSONObject();
      industryJSON.put("keyword", featureId.getTitle());
      industryJSON.put("code", featureId.getId());
      industryJSON.put("id", featureId.getId());
      results.put(industryJSON);
    }

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("results", results);
    return response;
  }
}
