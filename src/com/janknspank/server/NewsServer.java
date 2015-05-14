package com.janknspank.server;

import java.util.Set;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.reflections.Reflections;

import com.google.common.collect.Sets;
import com.janknspank.classifier.Feature;
import com.janknspank.nlp.KeywordCanonicalizer;

public class NewsServer {
  /**
   * Do slow things before we open a socket and Heroku thinks we're live.
   */
  private static void initialize() {
    KeywordCanonicalizer.getKeywordToEntityIdMap();
    Feature.getAllFeatures();
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    initialize();

    int port = (System.getenv("PORT") == null) ? 5000 : Integer.valueOf(System.getenv("PORT"));
    Server server = new Server(port);

    // Use web.xml as the definition for this server's end points.
    WebAppContext root = new WebAppContext();
    root.setDescriptor("WEB-INF/web.xml");
    root.setResourceBase("");
    root.setParentLoaderPriority(true);
    root.setMaxFormContentSize(5 * 1000 * 1000);

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
    try {
      server.start();
      server.join();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
