package com.janknspank.server;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.google.template.soy.tofu.SoyTofu.Renderer;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Session;

public class NewsServlet extends HttpServlet {
  protected static final String SESSION_ID_PARAM = "sessionKey";
  private static final String PARAMS_ATTRIBUTE_KEY = "__params";
  private static final String SESSION_ATTRIBUTE_KEY = "__session";

  /**
   * This is a highly robust way of looking for parameters either in post body,
   * multipart body, or query parameters.  We probably don't need to be so
   * robust, but I don't want to break the app, so I keep it this way.
   * TODO(jonemerson): Just use request.getParameter() going forward.
   */
  @SuppressWarnings("unchecked")
  public String getParameter(HttpServletRequest request, String key) {
    String value = request.getParameter(key);
    if (value != null) {
      return value;
    } else {
      return ((MultiMap<String>) request.getAttribute(PARAMS_ATTRIBUTE_KEY)).
          getString(key);
    }
  }

  public Session getSession(HttpServletRequest request) {
    return (Session) request.getAttribute(SESSION_ATTRIBUTE_KEY);
  }

  /**
   * Enforces servlet annotations, e.g. authentication.
   */
  @Override
  final public void service(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    // Parse the parameters.
    MultiMap<String> params = new MultiMap<String>();
    String queryString = getQueryString(request);
    UrlEncoded.decodeTo(queryString, params, "UTF-8", 100);
    request.setAttribute(PARAMS_ATTRIBUTE_KEY, params);

    // Handle authentication.  Authentication is required on servlets that
    // have annotated themselves with @AuthenticationRequired.  These
    // servlets may additionally restrict authentication required to only
    // certain methods (e.g. only POST).  If authentication is required
    // and we have no sessionKey or the sessionKey is invalid, this method
    // sends the user to an error response.  Otherwise, authentication is
    // handled on a best-effort basis.
    AuthenticationRequired authRequired =
        this.getClass().getAnnotation(AuthenticationRequired.class);
    boolean isAuthRequired = (authRequired != null) &&
        (authRequired.requestMethod().equals("ALL") ||
            authRequired.requestMethod().equals(request.getMethod()));
    Session session;
    try {
      String sessionKey = getParameter(request, "sessionKey");
      if (sessionKey == null) {
        session = getSessionFromCookies(request);
      } else {
        session = Session.get(sessionKey);
      }
    } catch (DataRequestException e) {
      // This only happens for illegal session IDs that don't represent
      // real people.  Be harsh here: Reject any such requests.
      handleAuthenticationError(request, response, e.getMessage());
      return;
    }
    if (isAuthRequired && session == null) {
      sendToLogin(request, response);
      return;
    }
    request.setAttribute(SESSION_ATTRIBUTE_KEY, session);

    // Onwards!
    super.service(request, response);
  }

  /**
   * Checks to see if we set a cookie containing the user's session ID.  If
   * so, create a Session from it and return it!
   */
  public static Session getSessionFromCookies(HttpServletRequest req) {
    String cookieName = "NewsSessionKey";
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (int i = 0; i < cookies.length; i++) {
        Cookie cookie = cookies[i];
        if (cookieName.equals(cookie.getName())) {
          try {
            return Session.get(cookie.getValue());
          } catch (DataRequestException e) {
            System.err.println("Bad cookie found, ignoring: " + e.getMessage());
          }
        }
      }
    }
    return null;
  }

  private void handleAuthenticationError(HttpServletRequest request,
      HttpServletResponse response, String message)
      throws ServletException, IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setHeader("Content-type", "application/json; charset=UTF-8");
    response.getWriter().print(getErrorJson(message).toString());
  }

  /**
   * Sends the user to a login page, with a redirect back to this
   * page when the user's finished authenticating.
   */
  private void sendToLogin(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // Reconstruct original requesting URL (path only - no scheme/server/port).
    StringBuffer url =  new StringBuffer();
    url.append(request.getContextPath()).append(request.getServletPath());
    String pathInfo = request.getPathInfo();
    if (pathInfo != null) {
      url.append(pathInfo);
    }
    String queryString = request.getQueryString();
    if (queryString != null) {
      url.append("?").append(queryString);
    }

    // Redirect the user to the login page with a next URL to this
    // page.
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    response.setHeader("Location",
        "/login?nextUrl=" + URLEncoder.encode(url.toString(), "UTF-8")); 
  }

  /**
   * Returns the request body, whether it came from query parameters or POST
   * body.
   */
  private String getQueryString(HttpServletRequest req) throws IOException {
    if (req.getParameterNames().hasMoreElements() ||
        Strings.nullToEmpty(req.getContentType()).startsWith("multipart")) {
      // Return nothing - we can just get parameters from the request.
      return "";
    } else if (req.getQueryString() != null) {
      return req.getQueryString();
    } else {
      return CharStreams.toString(req.getReader());
    }
  }

  protected JSONObject getErrorJson(String errorMessage) {
    JSONObject json = new JSONObject();
    json.put("success", false);
    json.put("errorMessage", errorMessage);
    return json;
  }

  /**
   * Returns the base filename for resources related to this servlet.  E.g.
   * if this servlet is named HappyServlet, this method would return "happy".
   */
  protected String getImpliedResourceName() {
    String name = this.getClass().getSimpleName();
    if (name.endsWith("Servlet")) {
      return name.substring(0, name.length() - "Servlet".length()).toLowerCase();
    }
    throw new IllegalStateException("Servlet class names should end with Servlet.");
  }

  /**
   * Returns a Tofu renderer for soy related to this Servlet class, preconfigured
   * to use namespace "cookout." plus the servlet's name.  E.g. "cookout.index".
   */
  protected SoyTofu getTofu() {
    SoyFileSet.Builder sfsBuilder = new SoyFileSet.Builder();
    for (File soyFile : new File("templates/").listFiles()) {
      if (soyFile.getName().endsWith(".soy")) {
        sfsBuilder.add(soyFile);
      }
    }
    SoyFileSet sfs = sfsBuilder.build();
    SoyTofu tofu = sfs.compileToTofu().forNamespace("news." + getImpliedResourceName());
    return tofu;
  }

  /**
   * Writes the output of a Soy template to the HttpResponse.  The template
   * to write is specified by "template", which should be a string starting
   * with a dot if the template is in the servlet's native package.
   * (Otherwise the template can be fully qualified, e.g.
   * "cookout.index.main").
   */
  protected void writeSoyTemplate(HttpServletResponse resp, String template, SoyMapData data)
      throws IOException {
    resp.setContentType("text/html");

    Renderer renderer = getTofu().newRenderer(template);
    if (data != null) {
      renderer.setData(data);
    }
    resp.getWriter().print(renderer.render());
  }

  /**
   * TODO(jonemerson): There's probably a better way to write a JSONObject
   * to a OutputStream besides converting the whole thing to a String first.
   */
  protected void writeJson(HttpServletResponse resp, JSONObject o) throws IOException {
    resp.setContentType("application/json");
    resp.getOutputStream().write(o.toString().getBytes());
  }
}
