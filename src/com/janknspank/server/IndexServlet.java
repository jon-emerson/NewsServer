package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ServletMapping(urlPattern = "/")
public class IndexServlet extends HttpServlet { // StandardServlet
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.getOutputStream().write("Hello world".getBytes());
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
