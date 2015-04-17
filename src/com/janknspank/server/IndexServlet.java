package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.IPLocationFinder;

@ServletMapping(urlPattern = "/")
public class IndexServlet extends NewsServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (getParameter(req, "ip") != null) {
      try {
        resp.getOutputStream().write(IPLocationFinder.getLocation(req).toString().getBytes());
        return;
      } catch (BiznessException e) {
        throw new ServletException(e);
      }
    }

    resp.getOutputStream().write("Hello World".getBytes());
  }

//  /**
//   * Returns any Soy data necessary for rendering the .main template for this
//   * servlet's Soy page.
//   */
//  @Override
//  protected SoyMapData getSoyMapData(HttpServletRequest req)
//      throws DatabaseSchemaException, RequestException {
//    return new SoyMapData("tab", 0);
//  }
}
