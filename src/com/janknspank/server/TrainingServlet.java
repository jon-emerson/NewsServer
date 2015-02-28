package com.janknspank.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.ArticleTypeCodes;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.Industry;
import com.janknspank.bizness.Urls;
import com.janknspank.common.Asserts;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.CoreProto.TrainedArticleClassification;
import com.janknspank.proto.CoreProto.TrainedArticleIndustry;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.CoreProto.UrlRating;
import com.janknspank.proto.EnumsProto.ArticleTypeCode;

@AuthenticationRequired
public class TrainingServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   * @throws DatabaseSchemaException
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req) throws DatabaseSchemaException {
    Article article = Articles.getRandomUntrainedArticle();
    return new SoyMapData(
        "sessionKey", this.getSession(req).getSessionKey(),
        "title", article.getTitle(),
        "url", article.getUrl(),
        "urlId", article.getUrlId(),
        "paragraphs", new SoyListData(article.getParagraphList()),
        "image_url", article.getImageUrl(),
        "classifications", Iterables.transform(ArticleTypeCodes.ARTICLE_CLASSIFICATION_CODE_MAP.values(),
            new Function<ArticleTypeCode, SoyMapData>() {
              @Override
              public SoyMapData apply(ArticleTypeCode articleClassification) {
                return new SoyMapData(
                    "code", articleClassification.getCode(),
                    "description", articleClassification.getDescription());
              }
            }),
        "industries", Iterables.transform(Arrays.asList(Industry.values()),
            new Function<Industry, SoyMapData>() {
              @Override
              public SoyMapData apply(Industry industryCode) {
                return new SoyMapData(
                    "code", industryCode.getCode(),
                    "group", industryCode.getGroup(),
                    "description", industryCode.getName());
              }
            }));
  }

  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseRequestException, DatabaseSchemaException {
    Session session = this.getSession(req);

    // Read parameters.
    String urlId = getRequiredParameter(req, "urlId");
    Url articleURL = Urls.getById(urlId);
    if (articleURL == null) {
      throw new RequestException("URL does not exist");
    }

    String[] industryIdsList = req.getParameterValues("industriesCheckboxes");
    //Only returns the selected checkboxes
    String[] articleClassificationCodesList = req.getParameterValues("classifications");
    int rating100scale = Integer.parseInt(req.getParameter("qualityScore"));
    Asserts.assertTrue(rating100scale > 0 && rating100scale < 100, "qualityScore must be between 0 - 100",
        RequestException.class);

    // Business logic.
    // Save the tagged industries
    if (industryIdsList != null) {
      List<TrainedArticleIndustry> articleIndustries = new ArrayList<TrainedArticleIndustry>();
      for (String industryId: industryIdsList) {
        articleIndustries.add(TrainedArticleIndustry.newBuilder()
            .setTrainerUserId(session.getUserId())
            .setUrlId(urlId)
            .setIndustryCodeId(Integer.parseInt(industryId))
            .build());
      }
      Database.insert(articleIndustries);
    }

    // Save the user relevance rating
    UrlRating userRating = UrlRating.newBuilder()
        .setUrl(articleURL.getUrl())
        .setRating(rating100scale)
        .setCreateTime(System.currentTimeMillis())
        .build();
    Database.insert(userRating);

    // Collect all checked and unchecked classification states
    Map<String, Boolean> classificationsHelper = new HashMap<String, Boolean>();
    for (String code : ArticleTypeCodes.ARTICLE_CLASSIFICATION_CODE_MAP.keySet()) {
      classificationsHelper.put(code, false);
    }
    if (articleClassificationCodesList != null) {
      for (String taggedCodes : articleClassificationCodesList) {
        classificationsHelper.put(taggedCodes, true);
      }
    }

    // Save the article classifications
    List<TrainedArticleClassification> articleClassifications = new ArrayList<TrainedArticleClassification>();
    for (Map.Entry<String, Boolean> entry : classificationsHelper.entrySet()) {
      String code = entry.getKey();
      Boolean state = entry.getValue();
      articleClassifications.add(TrainedArticleClassification.newBuilder()
          .setTrainerUserId(session.getUserId())
          .setUrlId(urlId)
          .setArticleClassificationCode(code)
          .setChecked(state)
          .build());
    }
    Database.insert(articleClassifications);

    return this.createSuccessResponse();
  }
}
