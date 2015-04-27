package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.template.soy.data.SoyMapData;
import com.janknspank.database.DatabaseSchemaException;

@ServletMapping(urlPattern = "/v1/native_privacy")
public class NativePrivacyServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, RequestException {
    return new SoyMapData(
        "title", "Spotter Privacy Policy");
  }
}
