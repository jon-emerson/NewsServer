package com.janknspank.server;

import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONObject;

import com.google.common.collect.Maps;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.KeywordToEntityId;

@ServletMapping(urlPattern = "/createKeywordToEntityId")
public class CreateKeywordToEntityIdServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException, BiznessException {
    // Use TreeMap to alphabetize.
    TreeMap<String, SoyMapData> industryMap = Maps.newTreeMap();
    for (FeatureId featureId : FeatureId.values()) {
      if (featureId.getFeatureType() == FeatureType.INDUSTRY) {
        industryMap.put(featureId.getTitle(), new SoyMapData(
            "name", featureId.getTitle(),
            "id", featureId.getId()));
      }
    }
    return new SoyMapData("industries", new SoyListData(industryMap.values()));
  }

  private FeatureId getFeatureId(HttpServletRequest req, String parameter) {
    return FeatureId.fromId(NumberUtils.toInt(getParameter(req, parameter)));
  }

  /**
   * Poor man's efficient case-insensitive count for existing articles with
   * the given keyword.  We have to do this since the MongoDB API for Java
   * doesn't support efficient case insensitive searches.
   */
  private int getCount(String keyword) throws DatabaseSchemaException {
    int try1 = (int) Database.with(Article.class).getSize(
        new QueryOption.WhereEquals("keyword.keyword", keyword));
    int try2 = (int) Database.with(Article.class).getSize(
        new QueryOption.WhereEquals("keyword.keyword", keyword.toLowerCase()));
    int try3 = (int) Database.with(Article.class).getSize(
        new QueryOption.WhereEquals("keyword.keyword", keyword.toUpperCase()));
    int try4 = (int) Database.with(Article.class).getSize(
        new QueryOption.WhereEquals("keyword.keyword", WordUtils.capitalizeFully(keyword)));
    int try5 = (int) Database.with(Article.class).getSize(
        new QueryOption.WhereEquals("keyword.keyword", WordUtils.capitalize(keyword)));
    return Math.max(Math.max(try1, try2), Math.max(try3, Math.max(try4, try5)));
  }

  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    String entityId = this.getRequiredParameter(req, "entity_id").trim();
    String keyword = this.getRequiredParameter(req, "keyword").trim();

    Entity entity = Database.with(Entity.class).get(entityId);
    if (entity == null) {
      throw new RequestException("Invalid entity ID: \"" + entityId + "\"");
    }

    KeywordToEntityId.Builder keywordToEntityIdBuilder = KeywordToEntityId.newBuilder()
        .setId(GuidFactory.generate())
        .setEntityId(entityId)
        .setType(entity.getType())
        .setCount(getCount(keyword))
        .setKeyword(keyword.toLowerCase());

    FeatureId industry1 = getFeatureId(req, "top_industry_id_1");
    if (industry1 != null) {
      keywordToEntityIdBuilder.setTopIndustryId1(industry1.getId());
    }

    FeatureId industry2 = getFeatureId(req, "top_industry_id_2");
    if (industry2 != null) {
      keywordToEntityIdBuilder.setTopIndustryId2(industry2.getId());
    }

    FeatureId industry3 = getFeatureId(req, "top_industry_id_3");
    if (industry3 != null) {
      keywordToEntityIdBuilder.setTopIndustryId3(industry3.getId());
    }

    KeywordToEntityId keywordToEntityId = keywordToEntityIdBuilder.build();
    Database.insert(keywordToEntityId);
    JSONObject response = createSuccessResponse();
    response.put("keywordToEntityId", Serializer.toJSON(keywordToEntityId));
    return response;
  }
}
