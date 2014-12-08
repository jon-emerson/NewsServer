package com.janknspank.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class NewsServer {
  public static void main(String[] args) throws Exception {
    int port = (System.getenv("PORT") == null) ? 5000 : Integer.valueOf(System.getenv("PORT"));
    Server server = new Server(port);

    // Use web.xml as the definition for this server's endpoints.
    WebAppContext root = new WebAppContext();
    root.setDescriptor("WEB-INF/web.xml");
    root.setResourceBase("");
    root.setParentLoaderPriority(true);
    server.setHandler(root);

    // Start server.
    server.start();
    server.join();
  }
}
