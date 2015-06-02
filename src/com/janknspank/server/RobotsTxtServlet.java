package com.janknspank.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ServletMapping(urlPattern = "/robots.txt")
public class RobotsTxtServlet extends NewsServlet {
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("User-agent: *\n");
    sb.append("Allow: /\n");
    resp.getOutputStream().write(sb.toString().getBytes());
  }
}
