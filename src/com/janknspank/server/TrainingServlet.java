package com.janknspank.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.data.ArticleClassifications;
import com.janknspank.data.Articles;
import com.janknspank.data.IndustryCodes;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Database;
import com.janknspank.data.TrainedArticleClassifications;
import com.janknspank.data.TrainedArticleIndustries;
import com.janknspank.data.Urls;
import com.janknspank.data.UserUrlFavorites;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleClassification;
import com.janknspank.proto.Core.IndustryCode;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.TrainedArticleClassification;
import com.janknspank.proto.Core.TrainedArticleIndustry;
import com.janknspank.proto.Core.UserUrlFavorite;
import com.janknspank.proto.Serializer;

@AuthenticationRequired
public class TrainingServlet extends StandardServlet {

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException {
    Article article = Articles.getRandomUntrainedArticle();
    return new SoyMapData(
        "sessionKey", this.getSession(req).getSessionKey(),
        "title", article.getTitle(),
        "url", article.getUrl(),
        "urlId", article.getUrlId(),
        "paragraphs", new SoyListData(article.getParagraphList()),
        "image_url", article.getImageUrl(),
        "classifications", Iterables.transform(ArticleClassifications.ARTICLE_CLASSIFICATION_CODE_MAP.values(),
            new Function<ArticleClassification, SoyMapData>() {
              @Override
              public SoyMapData apply(ArticleClassification articleClassification) {
                return new SoyMapData(
                    "code", articleClassification.getCode(),
                    "description", articleClassification.getDescription());
              }
            }),
        "industries", Iterables.transform(IndustryCodes.INDUSTRY_CODE_MAP.values(),
            new Function<IndustryCode, SoyMapData>() {
              @Override
              public SoyMapData apply(IndustryCode industryCode) {
                return new SoyMapData(
                    "id", industryCode.getId(),
                    "group", industryCode.getGroup(),
                    "description", industryCode.getDescription());
              }
            }));
  }
  
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException {
    Session session = this.getSession(req);
    
    // Read parameters.
    String urlId = getRequiredParameter(req, "urlId");
    String[] industryIdsList = req.getParameterValues("industriesCheckboxes");
    //Only returns the selected checkboxes
    String[] articleClassificationCodesList = req.getParameterValues("classifications");

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
      Database.getInstance().insert(articleIndustries);      
    }
    
    // Collect all checked and unchecked classification states
    Map<String, Boolean> classificationsHelper = new HashMap<String, Boolean>();
    for (String code : ArticleClassifications.ARTICLE_CLASSIFICATION_CODE_MAP.keySet()) {
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
    Database.getInstance().insert(articleClassifications);
    
    return this.createSuccessResponse();
  }
}
