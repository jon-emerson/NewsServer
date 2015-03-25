package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.template.soy.data.SoyMapData;
import com.janknspank.database.DatabaseSchemaException;

@ServletMapping(urlPattern = "/v1/terms")
public class TermsServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException {
    return new SoyMapData(
        "title", "Spotter Terms of Use");
  }
}
