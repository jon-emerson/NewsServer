package com.janknspank.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.api.client.util.Lists;
import com.google.common.base.Function;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.Articles;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.TopList;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.server.soy.ArticleSoy;

@ServletMapping(urlPattern = "/news/*")
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
      url = new URL(req.getRequestURL().toString());
    } catch (MalformedURLException e) {
      throw new RequestException("Invalid URL: " + req.getRequestURL());
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

  @Override
  protected String getResourceName() {
    return "viewfeed";
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

    final TopList<Article, Double> rankedArticlesAndScores =
        Articles.getArticlesForFeature(featureId, NUM_RESULTS);
    List<Article> articles = Lists.newArrayList();
    articles.addAll(rankedArticlesAndScores.getKeys());
    articles.sort(new Comparator<Article>() {
      @Override
      public int compare(Article o1, Article o2) {
        return -Long.compare(o1.getPublishedTime(), o2.getPublishedTime());
      }
    });
    return new SoyMapData(
        "articles", ArticleSoy.toSoyListData(
            articles,
            new Function<Article, Double>() {
              @Override
              public Double apply(Article article) {
                return rankedArticlesAndScores.getValue(article);
              }
            }));
  }
}
