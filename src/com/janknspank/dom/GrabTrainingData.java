package com.janknspank.dom;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Grabs content from all the URLs in "URLS", then writes their tokenized
 * versions to the /trainingdata folder under their appropriate subdirectories.
 * CAREFUL: THIS WILL OVERWRITE LOCAL COPIES YOU MAY HAVE ALREADY ANNOTATED.
 */
public class GrabTrainingData {
  private static final String[] URLS = {
    // Put URLs here...
    "http://dealbook.nytimes.com/2013/11/13/redfin-raises-50-million-in-latest-financing-round",
    "http://www.nytimes.com/2013/08/25/business/be-yourself-redfins-glenn-kelman-says-even-if-youre-a-little-goofy.html",
    "http://dealbook.nytimes.com/2014/07/28/zillow-to-buy-trulia-for-3-5-billion/",
    "http://www.nytimes.com/2006/09/03/business/yourmoney/03real.html",
    "http://www.nytimes.com/2014/08/21/fashion/at-burning-man-the-tech-elite-one-up-one-another.html",
    "http://bits.blogs.nytimes.com/2014/08/31/san-francisco-exhales-during-burning-man-exodus/",
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
      String path = getPathForUrl(url);
      System.err.println("Writing to " + path + "/" + filename + ".txt ...");
      BufferedWriter writer = new BufferedWriter(
          new FileWriter("trainingdata/" + path + "/" + filename + ".txt"));

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
