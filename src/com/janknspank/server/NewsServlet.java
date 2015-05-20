package com.janknspank.server;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.google.template.soy.tofu.SoyTofu.Renderer;
import com.janknspank.bizness.Sessions;
import com.janknspank.common.Asserts;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.UserProto.User;

public class NewsServlet extends HttpServlet {
  protected static final String SESSION_ID_PARAM = "session_key";
  private static final String PARAMS_ATTRIBUTE_KEY = "__params";
  private static final String SESSION_ATTRIBUTE_KEY = "__session";
  private static final String USER_ATTRIBUTE_KEY = "__user";

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
      for (NameValuePair pair : (List<NameValuePair>) request.getAttribute(PARAMS_ATTRIBUTE_KEY)) {
        if (key.equals(pair.getName())) {
          return pair.getValue();
        }
      }
    }
    return null;
  }

  public boolean hasParameter(HttpServletRequest request, String key) {
    return request.getParameterMap().containsKey(key);
  }

  /**
   * Gets a parameter, or throws a DatabaseRequestException.
   */
  public String getRequiredParameter(HttpServletRequest request, String key)
      throws RequestException {
    return Asserts.assertNonEmpty(getParameter(request, key), key, RequestException.class);
  }

  public Session getSession(HttpServletRequest request) {
    return (Session) request.getAttribute(SESSION_ATTRIBUTE_KEY);
  }

  public User getUser(HttpServletRequest request)
      throws DatabaseSchemaException, AuthenticationRequiredException {
    User user = (User) request.getAttribute(USER_ATTRIBUTE_KEY);
    if (user == null) {
      synchronized (this) {
        user = (User) request.getAttribute(USER_ATTRIBUTE_KEY);
        if (user == null) {
          user = Database.with(User.class).get(getSession(request).getUserId());
          if (user == null) {
            throw new AuthenticationRequiredException("No user found");
          }
          request.setAttribute(USER_ATTRIBUTE_KEY, user);
        }
      }
    }
    return user;
  }

  /**
   * Enforces servlet annotations, e.g. authentication.
   */
  @Override
  final public void service(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    // Parse the parameters.
    request.setAttribute(PARAMS_ATTRIBUTE_KEY,
        URLEncodedUtils.parse(getQueryString(request), Charsets.UTF_8));

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
      String sessionKey = getParameter(request, "session_key");
      if (Strings.isNullOrEmpty(sessionKey)) {
        session = getSessionFromCookies(request);
      } else {
        session = Sessions.getBySessionKey(sessionKey);
      }
    } catch (RequestException e) {
      if (isAuthRequired) {
        handleAuthenticationError(request, response,
            "Invalid authentication token: " + e.getMessage());
        return;
      }
      session = null;
    } catch (DatabaseSchemaException e) {
      e.printStackTrace();
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      writeJson(request, response, getErrorJson(e.getMessage()));
      return;
    }
    if (isAuthRequired && session == null) {
      handleAuthenticationError(request, response, "Authentication required");
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
            return Sessions.getBySessionKey(cookie.getValue());
          } catch (RequestException | DatabaseSchemaException e) {
            System.err.println("Bad cookie found, ignoring: " + e.getMessage());
          }
        }
      }
    }
    return null;
  }

  protected void handleAuthenticationError(HttpServletRequest request,
      HttpServletResponse response, String message)
      throws ServletException, IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setHeader("Content-type", "application/json; charset=UTF-8");
    response.getWriter().print(getErrorJson(message).toString());
  }

//  /**
//   * Sends the user to a login page, with a redirect back to this
//   * page when the user's finished authenticating.
//   */
//  private void sendToLogin(HttpServletRequest request, HttpServletResponse response)
//      throws ServletException, IOException {
//    // Reconstruct original requesting URL (path only - no scheme/server/port).
//    StringBuffer url =  new StringBuffer();
//    url.append(request.getContextPath()).append(request.getServletPath());
//    String pathInfo = request.getPathInfo();
//    if (pathInfo != null) {
//      url.append(pathInfo);
//    }
//    String queryString = request.getQueryString();
//    if (queryString != null) {
//      url.append("?").append(queryString);
//    }
//
//    // Redirect the user to the login page with a next URL to this
//    // page.
//    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
//    response.setHeader("Location",
//        "/login?nextUrl=" + URLEncoder.encode(url.toString(), "UTF-8")); 
//  }

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
  protected String getResourceName() {
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
  public static SoyTofu getTofu(String resourceName) {
    SoyFileSet.Builder sfsBuilder = new SoyFileSet.Builder();
    for (File soyFile : new File("templates/").listFiles()) {
      if (soyFile.getName().endsWith(".soy")) {
        sfsBuilder.add(soyFile);
      }
    }
    SoyFileSet sfs = sfsBuilder.build();
    SoyTofu tofu = sfs.compileToTofu().forNamespace("news." + resourceName);
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
    resp.setContentType("text/html; charset=utf-8");

    Renderer renderer = getTofu(getResourceName()).newRenderer(template);
    if (data != null) {
      renderer.setData(data);
    }
    resp.getWriter().print(renderer.render());
  }

  /**
   * TODO(jonemerson): There's probably a better way to write a JSONObject
   * to a OutputStream besides converting the whole thing to a String first.
   */
  protected void writeJson(HttpServletRequest req, HttpServletResponse resp, JSONObject o)
      throws IOException {
    resp.setContentType("application/json; charset=utf-8");
    if (hasParameter(req, "indent")) {
      int indentFactor = 2;
      try {
        indentFactor = Integer.parseInt(getParameter(req, "indent"));
      } catch (Exception e) {}
      resp.getOutputStream().write(o.toString(indentFactor).getBytes());
    } else {
      resp.getOutputStream().write(o.toString().getBytes());
    }
  }

  protected final JSONObject createSuccessResponse() {
    JSONObject response = new JSONObject();
    response.put("success", true);
    return response;
  }

  public static String getRemoteAddr(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    String xForwardedForHeader = request.getHeader("X-FORWARDED-FOR");
    if (xForwardedForHeader != null) {
      remoteAddr = xForwardedForHeader;
      int idx = remoteAddr.indexOf(',');
      if (idx > -1) {
        remoteAddr = remoteAddr.substring(0, idx);
      }
    }
    return remoteAddr;
  }
}
