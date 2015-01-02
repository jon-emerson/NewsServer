package com.janknspank.dom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.base.Joiner;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;

/**
 * Grabs content from all the URLs in "URLS", then writes their tokenized
 * versions to the /trainingdata folder under their appropriate subdirectories.
 */
public class GrabTrainingData {
  private static final String[] URLS = {
    "http://www.bbc.co.uk/news/uk-30625945"
  };

  /**
   * Return what trainingdata/ subdirectory to use for the passed url.
   * E.g. a NYTimes article will go into "nytimes.com".
   */
  private static String getPathForUrl(String url) {
    try {
      URL bigUrl = new URL(url);
      String domain = bigUrl.getHost();
      List<String> components = Arrays.asList(domain.split("\\."));
      int count = components.size();
      String tld = components.get(count - 1);
      return Joiner.on(".").join(
          "uk".equals(tld) || "au".equals(tld) || "nz".equals(tld) ?
          components.subList(Math.max(0, count - 3), count) :
          components.subList(Math.max(0, count - 2), count));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private static InputStream getPage(String url) throws ParserException {
    HttpGet httpget = new HttpGet(url);

    RequestConfig config = RequestConfig.custom()
//        .setCookieSpec(CookieSpecs.IGNORE_COOKIES) // Don't pick up cookies.
        .build();
    CloseableHttpClient httpclient = HttpClients.custom()
        .setDefaultRequestConfig(config)
        .build();

    try {
      CloseableHttpResponse response = httpclient.execute(httpget);
      if (response.getStatusLine().getStatusCode() == 200) {
        return response.getEntity().getContent();
      }
      throw new ParserException("Bad response, status code = " +
          response.getStatusLine().getStatusCode());
    } catch (IOException e) {
      throw new ParserException("Could not read web site", e);
    }
  }

  public static void main(String args[]) throws Exception {
    SiteParser siteParser = new SiteParser();
    for (String url : URLS) {
      // Get all the paragraphs.
      List<Node> paragraphs = siteParser.getParagraphNodes(getPage(url), url);

      // Open a file for writing all the paragraphs and sentences.
      String filename = url;
      if (filename.endsWith("/")) {
        filename = url.substring(0, filename.length() - "/".length());
      }
      if (filename.endsWith(".html")) {
        filename = url.substring(0, filename.length() - ".html".length());
      }
      if (filename.endsWith(".php")) {
        filename = url.substring(0, filename.length() - ".php".length());
      }
      if (filename.endsWith("/index")) {
        filename = url.substring(0, filename.length() - "/index".length());
      }
      filename = filename.substring(filename.lastIndexOf("/") + 1);
      filename = filename.replaceAll("[\\?\\=]", "-");
      String path = getPathForUrl(url);

      // Open the writer, creating a new directory if necessary.
      File directory = new File("trainingdata/" + path);
      if (!directory.exists()) {
        directory.mkdir();
      }
      File file = new File(directory, filename + ".txt");
//      if (file.exists()) {
//        throw new RuntimeException("File already exists! " + file.getAbsolutePath());
//      }
      System.err.println("Writing to " + path + "/" + filename + ".txt ...");
      BufferedWriter writer = new BufferedWriter(new FileWriter(file));

      // Write the original URL first, for tracking purposes.
      writer.write(url);
      writer.newLine();

      // Write out all the sentences, tokenized.
      for (Node paragraph : paragraphs) {
        String paragraphText = paragraph.getFlattenedText();
        for (String sentence : Interpreter.getSentences(paragraphText)) {
          boolean first = true;
          for (String token : Interpreter.getTokens(sentence)) {
            if (first) {
              first = false;
            } else {
              writer.write(" ");
            }
            writer.write(token);
          }
          writer.newLine();
        }
      }
      writer.close();
    }
  }
}
