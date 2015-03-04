package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.utils.S3DemoHelper;

@ServletMapping(urlPattern = "/demo")
public class DemoServlet extends StandardServlet {
  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  @Override
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DatabaseSchemaException, BiznessException, DatabaseRequestException {
    // TODO: detect the "update" parameter - change the template for updates
    String prompt = "Install Demo";
    String update = getParameter(req, "update");
    if (!Strings.isNullOrEmpty(update)) {
      prompt = "Please update your demo";
    }

    // TODO: fix so the plist URL is dynamic
    int[] latestVersionComponents = S3DemoHelper.findLatestDemoVersion();
    String versionString = latestVersionComponents[0] + "." + latestVersionComponents[1];
    if (latestVersionComponents[2] != 0) {
      versionString += "." + latestVersionComponents[2];
    }

    return new SoyMapData(
        "prompt", prompt, 
        "downloadUrl", "https://murmuring-sands-7215.herokuapp.com/s3Pipe/Spotter-v" + versionString + ".plist");
  }
}
