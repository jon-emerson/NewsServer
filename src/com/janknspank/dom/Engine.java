package com.janknspank.dom;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.xml.sax.SAXException;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

public class Engine {
  public static DocumentNode crawl(String url) throws SAXException, IOException {
    HttpGet httpget = new HttpGet(url);

    RequestConfig config = RequestConfig.custom()
//        .setCookieSpec(CookieSpecs.IGNORE_COOKIES) // Don't pick up cookies.
        .build();
    CloseableHttpClient httpclient = HttpClients.custom()
        .setDefaultRequestConfig(config)
        .build();

    CloseableHttpResponse response = httpclient.execute(httpget);
    if (response.getStatusLine().getStatusCode() == 200) {
      return new HtmlHandler(response.getEntity().getContent()).getDocumentNode();
    }
    throw new IOException("Bad response, status code = " +
        response.getStatusLine().getStatusCode());
  }

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

  public static void main(String args[]) throws Exception {
    DocumentNode node = crawl("http://www.nytimes.com/2014/02/23/opinion/sunday/" +
        "friedman-how-to-get-a-job-at-google.html");
    //DocumentNode node = crawl("http://www.nytimes.com/2014/09/25/technology/" +
    //    "exposing-hidden-biases-at-google-to-improve-diversity.html");
    printNode(node, 0);
//    DocumentNode node = crawl("http://www.nytimes.com/2014/12/04/us/politics/" +
//        "roy-h-beck-quietly-leads-a-grass-roots-army.html");
    List<Node> paragraphs = node.findAll("article > p");

    SortedMultiset<String> names = TreeMultiset.create();
    SortedMultiset<String> organizations = TreeMultiset.create();
    SortedMultiset<String> locations = TreeMultiset.create();

    for (Node paragraph : paragraphs) {
      String paragraphText = paragraph.getFlattenedText();
      for (String sentence : Tokenizer.getSentences(paragraphText)) {
        System.out.println(sentence);
      }
      for (String name : Tokenizer.getNames(paragraphText)) {
        names.add(name);
      }
      for (String organization : Tokenizer.getOrganizations(paragraphText)) {
        organizations.add(organization);
      }
      for (String location : Tokenizer.getLocations(paragraphText)) {
        locations.add(location);
      }
    }
    for (String name : names.descendingMultiset()) {
      System.out.println("NAME: " + name + " (" + names.count(name) + " occurrences)");
    }
    for (String organization : organizations.descendingMultiset()) {
      System.out.println("ORG: " + organization + " (" + organizations.count(organization) +
          " occurrences)");
    }
    for (String location : locations.descendingMultiset()) {
      System.out.println("LOCATION: " + location + " (" + locations.count(location) +
          " occurrences)");
    }
  }
}
