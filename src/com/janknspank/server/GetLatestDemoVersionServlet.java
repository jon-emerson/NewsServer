package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.BiznessException;
import com.janknspank.common.Version;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.utils.S3DemoHelper;

@ServletMapping(urlPattern = "/v1/get_latest_demo_version")
public class GetLatestDemoVersionServlet extends StandardServlet {
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, BiznessException,
      DatabaseRequestException, RequestException {
    Version latestVersion = S3DemoHelper.findLatestDemoVersion();

    // Create response
    JSONObject response = createSuccessResponse();

    response.put("version", latestVersion.toString());
    response.put("downloadUrl", "http://www.spotternews.com/demo?update=1");
    return response;
  }
}
