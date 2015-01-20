package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.data.Articles;
import com.janknspank.data.IndustryCodes;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Database;
import com.janknspank.data.Urls;
import com.janknspank.data.UserUrlFavorites;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.IndustryCode;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.UserUrlFavorite;
import com.janknspank.proto.Serializer;

@AuthenticationRequired(requestMethod = "POST")
public class TrainingServlet extends StandardServlet {

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException {
    Article art = Articles.getRandomUntrainedArticle();
    return new SoyMapData(
        "title", art.getTitle(),
        "paragraphs", new SoyListData(art.getParagraphList()),
        "image_url", art.getImageUrl(),
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
    //TODO
    return null;
  }
}
