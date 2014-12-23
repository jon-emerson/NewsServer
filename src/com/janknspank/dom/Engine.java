package com.janknspank.dom;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class Engine {

  public static void printNode(Node node, int depth) {
    for (int i = 0; i < depth; i ++) {
      System.out.print("  ");
    }
    System.out.println(
        node instanceof DocumentNode ? "DOCUMENT" : node.getTagName().toLowerCase());
    for (int i = 0; i < node.getChildCount(); i++) {
      if (node.isChildTextNode(i)) {
        printText(node.getChildText(i), depth + 1);
      } else {
        printNode(node.getChildNode(i), depth + 1);
      }
    }
  }

  public static void printText(String text, int depth) {
    for (int i = 0; i < depth; i ++) {
      System.out.print("  ");
    }
    System.out.println("TEXT: \"" + text + "\"");
  }

  private static InputStream getPage(String url) throws ParseException {
    HttpGet httpget = new HttpGet(url);

    RequestConfig config = RequestConfig.custom()
        .build();
    CloseableHttpClient httpclient = HttpClients.custom()
        .setDefaultRequestConfig(config)
        .build();

    try {
      CloseableHttpResponse response = httpclient.execute(httpget);
      if (response.getStatusLine().getStatusCode() == 200) {
        return response.getEntity().getContent();
      }
      throw new ParseException("Bad response, status code = " +
          response.getStatusLine().getStatusCode());
    } catch (IOException e) {
      throw new ParseException("Could not read web site", e);
    }
  }

  public static void main(String args[]) throws Exception {
    String[] urls = {
        "http://bits.blogs.nytimes.com/2010/01/07/google-applies-to-become-power-marketer/"
    };
    for (String url : urls) {
      InterpretedData interpretedData = new Interpreter(getPage(url), url).getInterpretedData();
      for (String person : interpretedData.getPeople()) {
        System.out.println("PERSON: " + person + " (" + interpretedData.getPersonCount(person) +
            " occurrences)");
      }
      for (String organization : interpretedData.getOrganizations()) {
        System.out.println("ORG: " + organization + " (" +
            interpretedData.getOrganizationCount(organization) + " occurrences)");
      }
      for (String location : interpretedData.getLocations()) {
        System.out.println("LOCATION: " + location + " (" +
            interpretedData.getLocationCount(location) + " occurrences)");
      }
    }
  }
}
