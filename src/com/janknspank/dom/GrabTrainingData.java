package com.janknspank.dom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Grabs content from all the URLs in "URLS", then writes their tokenized
 * versions to the /trainingdata folder under their appropriate subdirectories.
 */
public class GrabTrainingData {
  private static final String[] URLS = {
    // Put URLs here...
    "http://www.abc.net.au/news/2014-12-16/sydney-siegesocial-media-a-liability-says-expert/5971622",
    "http://abcnews.go.com/US/wireStory/waterlogged-northern-california-rain-27626164",
    "http://america.aljazeera.com/articles/2014/12/16/russia-raises-keyinterestrateto17percent.html",
    "http://hosted.ap.org/dynamic/stories/U/US_GOOGLE_TOP_SEARCHES_OF_2014",
    "http://arstechnica.com/tech-policy/2014/12/dashcam-catches-texas-cop-manhandling-stunning-elderly-man/",
    "http://bdnews24.com/business/2014/12/03/malaysia-also-keen-to-cut-trade-deficit-with-bangladesh",
    "http://www.bloomberg.com/news/2014-12-16/european-index-futures-signal-equities-rebound-from-six-day-drop.html",
    "http://www.boston.com/business/technology/2014/12/11/free-apps-you-need-for-local-holiday-shopping-deals/eL9q6O9h6RDCBDq6S7l6uL/pictures.html",
    "http://www.breitbart.com/Big-Journalism/2014/12/13/Berkeley-Hecklers-Expose-Free-Speech-Movement-Fraud",
    "http://www.buffalonews.com/city-region/development/a-forgotten-road-now-turning-heads-and-opening-up-opportunities-20141215"
  };

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

  /**
   * Return what trainingdata/ subdirectory to use for the passed url.
   * E.g. a NYTimes article will go into "nytimes.com".
   */
  public static String getPathForUrl(String url) {
    try {
      URL bigUrl = new URL(url);
      String domain = bigUrl.getHost();
      while (domain.lastIndexOf(".") != domain.indexOf(".")) {
        domain = domain.substring(domain.indexOf(".") + 1);
      }
      return domain;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String args[]) throws Exception {
    SiteParser siteParser = new SiteParser();
    for (String url : URLS) {
      // Get all the paragraphs.
      List<Node> paragraphs = siteParser.getParagraphNodes(url);

      // Open a file for writing all the paragraphs and sentences.
      String filename = url;
      if (filename.endsWith("/")) {
        filename = url.substring(0, filename.length() - "/".length());
      } if (filename.endsWith(".html")) {
        filename = url.substring(0, filename.length() - ".html".length());
      } if (filename.endsWith(".php")) {
        filename = url.substring(0, filename.length() - ".php".length());
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
        for (String sentence : Tokenizer.getSentences(paragraphText)) {
          boolean first = true;
          for (String token : Tokenizer.getTokens(sentence)) {
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
