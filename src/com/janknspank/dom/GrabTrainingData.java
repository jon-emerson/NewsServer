package com.janknspank.dom;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Grabs content from all the URLs in "URLS", then writes their tokenized
 * versions to the /trainingdata folder under their appropriate subdirectories.
 * CAREFUL: THIS WILL OVERWRITE LOCAL COPIES YOU MAY HAVE ALREADY ANNOTATED.
 */
public class GrabTrainingData {
  private static final String[] URLS = {
    // Put URLs here...
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
      String path = url.contains("curbed.com/") ? "curbed.com" : "techcrunch.com";
      FileOutputStream fos = new FileOutputStream("trainingdata/" + path + "/" + filename + ".txt");
      System.err.println("Writing to " + filename + ".txt ...");

      // Write the original URL first, for tracking purposes.
      fos.write(url.getBytes());
      fos.write("\n".getBytes());

      // Write out all the sentences, tokenized.
      for (Node paragraph : paragraphs) {
        String paragraphText = paragraph.getFlattenedText();
        for (String sentence : Tokenizer.getSentences(paragraphText)) {
          boolean first = true;
          for (String token : Tokenizer.getTokens(sentence)) {
            if (first) {
              first = false;
            } else {
              fos.write(" ".getBytes());
            }
            fos.write(token.getBytes());
          }
          fos.write("\n".getBytes());
        }
      }
      fos.close();
    }
  }
}
