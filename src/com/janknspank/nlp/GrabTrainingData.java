package com.janknspank.nlp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Joiner;
import com.janknspank.crawler.ParagraphFinder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;

/**
 * Grabs content from all the URLs in "URLS", then writes their tokenized
 * versions to the /trainingdata folder under their appropriate subdirectories.
 */
public class GrabTrainingData {
  private final static Fetcher FETCHER = new Fetcher();

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
          ("uk".equals(tld) || "au".equals(tld) || "nz".equals(tld))
          ? components.subList(Math.max(0, count - 3), count)
          : components.subList(Math.max(0, count - 2), count));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private DocumentNode getDocumentNode(String url) throws FetchException {
    FetchResponse fetchResponse = FETCHER.get(url);
    if (fetchResponse.getStatusCode() == HttpServletResponse.SC_OK) {
      return fetchResponse.getDocumentNode();
    } else {
      throw new FetchException("Could not read web site");
    }
  }

  public static void main(String args[]) throws Exception {
    GrabTrainingData grabTrainingData = new GrabTrainingData();

    for (String url : URLS) {
      // Get all the paragraphs.
      List<Node> paragraphs =
          ParagraphFinder.getParagraphNodes(grabTrainingData.getDocumentNode(url));

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
      System.err.println("Writing to " + path + "/" + filename + ".txt ...");
      BufferedWriter writer = new BufferedWriter(new FileWriter(file));

      // Write the original URL first, for tracking purposes.
      writer.write(url);
      writer.newLine();

      // Write out all the sentences, tokenized.
      for (Node paragraph : paragraphs) {
        String paragraphText = paragraph.getFlattenedText();
        for (String sentence : KeywordFinder.getInstance().getSentences(paragraphText)) {
          boolean first = true;
          for (String token : KeywordFinder.getInstance().getTokens(sentence)) {
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
