package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;

public class DemoServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, BiznessException, DatabaseRequestException {
    // TODO: fix so the plist URL is dynamic
    return new SoyMapData(
          "downloadUrl", "https://murmuring-sands-7215.herokuapp.com/s3Pipe/Spotter-v0.1.plist");
  }
}
