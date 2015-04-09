package com.janknspank.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ServletMapping(urlPattern = "/download")
public class DownloadServlet extends HttpServlet {
  @Override
  public void service(HttpServletRequest request, HttpServletResponse resp) {
    resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    resp.setHeader("Location", arg1);
  }
}
