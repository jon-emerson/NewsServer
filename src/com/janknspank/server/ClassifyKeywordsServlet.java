package com.janknspank.server;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.BiznessException;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.KeywordToEntityId;

@ServletMapping(urlPattern = "/classifyKeywords")
public class ClassifyKeywordsServlet extends StandardServlet {
  private String getIndustry(KeywordToEntityId keywordToEntityId) {
    List<String> industries = Lists.newArrayList();
    for (int topIndustryId : new int[] {
        keywordToEntityId.getTopIndustryId1(),
        keywordToEntityId.getTopIndustryId2(),
        keywordToEntityId.getTopIndustryId3() }) {
      FeatureId maybeFeatureId = FeatureId.fromId(topIndustryId);
      if (maybeFeatureId != null) {
        industries.add(maybeFeatureId.getTitle());
      }
    }
    return Joiner.on(", ").join(industries);
  }

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException, BiznessException {
    int offset = NumberUtils.toInt(getParameter(req, "offset"));
    String query = getParameter(req, "query");

    List<SoyMapData> articleSoyMapDataList = Lists.newArrayList();
    for (KeywordToEntityId keywordToEntityId : Database.with(KeywordToEntityId.class).get(
        new QueryOption.WhereNull("entity_id"),
        new QueryOption.WhereNotTrue("removed"),
        new QueryOption.DescendingSort("count"),
        query == null
            ? new QueryOption.WhereNotNull("id") // No-op.
            : new QueryOption.WhereLike("keyword", "%" + query + "%"),
        new QueryOption.LimitWithOffset(30, offset))) {
      SoyMapData articleSoyMapData = new SoyMapData(
          "id", keywordToEntityId.getId(),
          "count", keywordToEntityId.getCount(),
          "keyword", keywordToEntityId.getKeyword(),
          "industry", getIndustry(keywordToEntityId));
      articleSoyMapDataList.add(articleSoyMapData);
    }
    return new SoyMapData(
        "keywordToEntityIds", new SoyListData(articleSoyMapDataList));
  }

  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, RequestException, DatabaseRequestException {
    for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("delete-")) {
        String keywordToEntityIdId = key.substring("delete-".length());
        Database.update(
            Database.with(KeywordToEntityId.class)
                .getFirst(new QueryOption.WhereEquals("id", keywordToEntityIdId))
                .toBuilder()
                .setRemoved(true)
                .build());
      } else {
        String keywordToEntityIdId = key;
        String entityId = entry.getValue().length > 0 ? entry.getValue()[0] : "";
        if (!entityId.trim().isEmpty()) {
          if (Database.with(Entity.class).get(entityId) != null) {
            Database.update(
                Database.with(KeywordToEntityId.class).getFirst(
                    new QueryOption.WhereEquals("id", keywordToEntityIdId)).toBuilder()
                        .setEntityId(entityId)
                        .build());
          } else {
            throw new RequestException("Entity ID does not exist: " + entityId);
          }
        }
      }
    }
    return this.createSuccessResponse();
  }
}
