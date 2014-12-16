package com.janknspank.dom;

import java.util.List;

import com.google.common.collect.Multisets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

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

  public static void main(String args[]) throws Exception {
    SiteParser siteParser = new SiteParser();
    String[] urls = {
        "http://bits.blogs.nytimes.com/2010/01/07/google-applies-to-become-power-marketer/"
    };
    for (String url : urls) {
      // Get all the paragraphs.
      List<Node> paragraphs = siteParser.getParagraphNodes(url);

      SortedMultiset<String> names = TreeMultiset.create();
      SortedMultiset<String> organizations = TreeMultiset.create();
      SortedMultiset<String> locations = TreeMultiset.create();

      for (Node paragraph : paragraphs) {
        String paragraphText = paragraph.getFlattenedText();
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
      for (String name : Multisets.copyHighestCountFirst(names).elementSet()) {
        System.out.println("NAME: " + name + " (" + names.count(name) + " occurrences)");
      }
      for (String organization : Multisets.copyHighestCountFirst(organizations).elementSet()) {
        System.out.println("ORG: " + organization + " (" + organizations.count(organization) +
            " occurrences)");
      }
      for (String location : Multisets.copyHighestCountFirst(locations).elementSet()) {
        System.out.println("LOCATION: " + location + " (" + locations.count(location) +
            " occurrences)");
      }
    }
  }
}
