package com.janknspank.server;

import java.util.Set;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.reflections.Reflections;

import com.google.common.collect.Sets;

public class NewsServer {
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    int port = (System.getenv("PORT") == null) ? 5000 : Integer.valueOf(System.getenv("PORT"));
    Server server = new Server(port);

    // Use web.xml as the definition for this server's end points.
    WebAppContext root = new WebAppContext();
    root.setDescriptor("WEB-INF/web.xml");
    root.setResourceBase("");
    root.setParentLoaderPriority(true);

    Set<String> urlPatternSet = Sets.newHashSet();
    for (Class<?> servletClass :
        new Reflections(NewsServer.class.getPackage().getName())
            .getTypesAnnotatedWith(ServletMapping.class)) {
      try {
        String urlPattern = servletClass.getAnnotation(ServletMapping.class).urlPattern();
        if (urlPatternSet.contains(urlPattern)) {
          throw new IllegalStateException("URL pattern cannot be used twice: " + urlPattern);
        }
        root.addServlet((Class<? extends Servlet>) servletClass, urlPattern);
        urlPatternSet.add(urlPattern);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    server.setHandler(root);

    // Start server.
    server.start();
    server.join();
  }
}
