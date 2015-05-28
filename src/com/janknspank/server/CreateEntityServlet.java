package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.Entity.Source;

@ServletMapping(urlPattern = "/createEntity")
public class CreateEntityServlet extends StandardServlet {

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException, BiznessException {
    List<SoyMapData> typeList = Lists.newArrayList();
    for (EntityType entityType : Iterables.concat(
        GetInterestsServlet.ORGANIZATION_TYPES, GetInterestsServlet.PEOPLE_TYPES)) {
      SoyMapData soyMapData = new SoyMapData(
          "name", entityType.name(),
          "string", entityType.toString());
      typeList.add(soyMapData);
    }
    return new SoyMapData("types", new SoyListData(typeList));
  }

  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    String keyword = this.getRequiredParameter(req, "keyword").trim();
    String shortName = Strings.nullToEmpty(getParameter(req, "short_name")).trim();
    String typeStr = this.getRequiredParameter(req, "type");

    EntityType type = EntityType.fromValue(typeStr);
    if (type == null) {
      throw new RequestException("Invalid type: \"" + typeStr + "\"");
    }

    Entity.Builder entityBuilder = Entity.newBuilder()
        .setId(GuidFactory.generate())
        .setKeyword(keyword)
        .setType(type.toString())
        .setSource(Source.MANUAL);
    if (!Strings.isNullOrEmpty(shortName)) {
      entityBuilder.setShortName(shortName);
    }
    Entity entity = entityBuilder.build();
    Database.insert(entity);
    JSONObject response = createSuccessResponse();
    response.put("entity", Serializer.toJSON(entity));
    return response;
  }
}
