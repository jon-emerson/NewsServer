package com.janknspank.server;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.Articles;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.TopList;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.rank.NeuralNetworkScorer;
import com.janknspank.server.soy.ArticleSoy;

public class FeatureArticlesServlet extends StandardServlet {
  private static final int NUM_RESULTS = 50;

  /**
   * Parses the URL for a feature specifier.  The feature specifier is the text
   * after the last forward slash in the path.  It can be a number or a text
   * prefix of a Feature's description.  E.g. "supermarket" will match
   * FeatureId.SUPERMARKETS.
   */
  private FeatureId getFeatureId(HttpServletRequest req) throws RequestException {
    URL url;
    try {
      url = new URL(req.getRequestURI());
    } catch (MalformedURLException e) {
      throw new RequestException("Invalid URL: " + req.getRequestURI());
    }

    String path = url.getPath();
    path = path.substring(path.lastIndexOf("/") + 1);
    int rawFeatureId = NumberUtils.toInt(path, -1);
    if (rawFeatureId > 0) {
      return FeatureId.fromId(rawFeatureId);
    }

    for (FeatureId featureId : FeatureId.values()) {
      String featureTitle = featureId.getTitle()
          .toLowerCase()
          .replaceAll(",", "")
          .replaceAll(" ", "-")
          .replaceAll("&", "and");
      if (featureTitle.startsWith(path)) {
        return featureId;
      }
    }
    return null;
  }

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException {
    FeatureId featureId = getFeatureId(req);
    if (featureId == null) {
      throw new RequestException("Could not determine feature for request");
    }
//
//    final TopList<Article, Double> rankedArticlesAndScores =
//        Articles.getFeatureArticlesAndScores(featureId, NeuralNetworkScorer.getInstance(), NUM_RESULTS);
//    return new SoyMapData(
//        "sessionKey", this.getSession(req).getSessionKey(),
//        "articles", ArticleSoy.toSoyListData(rankedArticlesAndScores));
    throw new RequestException("Not implemented yet, try back later!!");
  }
}
