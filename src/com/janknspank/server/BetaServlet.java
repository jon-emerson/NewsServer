package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.BiznessException;
import com.janknspank.common.Version;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.utils.S3DemoHelper;

@ServletMapping(urlPattern = "/beta")
public class BetaServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, BiznessException, DatabaseRequestException {
    // TODO: detect the "update" parameter - change the template for updates
    String prompt = "Install Beta";
    String update = getParameter(req, "update");
    if (!Strings.isNullOrEmpty(update)) {
      prompt = "Please update Spotter to the latest beta";
    }

    // TODO: fix so the plist URL is dynamic
    Version latestVersion = S3DemoHelper.findLatestDemoVersion();
    return new SoyMapData(
        "prompt", prompt, 
        "downloadUrl", "https://murmuring-sands-7215.herokuapp.com/s3Pipe/Spotter-v"
            + latestVersion + ".plist");
  }
}
