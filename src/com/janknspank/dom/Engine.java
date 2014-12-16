package com.janknspank.dom;

import java.io.FileOutputStream;
import java.util.List;

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

  private static final String[] urls = {
//    "http://techcrunch.com/2014/12/15/why-its-right-to-report-on-the-sony-hack/",
//    "http://techcrunch.com/2014/12/15/crowdsolve-wants-to-give-serials-legion-of-armchair-detectives-a-crime-solving-platform/",
//    "http://techcrunch.com/2014/12/15/mark-zuckerbergs-facebook-profile-under-attack-from-brazilian-trolls/",
//    "http://techcrunch.com/2014/12/15/these-australian-social-media-reactions-to-the-sydneysiege-are-perfect/",
//    "http://techcrunch.com/2014/12/15/homejoy-le-pause/",
//    "http://techcrunch.com/2014/12/15/hiptic-games-launches-its-gamethrive-push-notification-service/",
//    "http://techcrunch.com/2014/12/15/how-to-speak-startup/",
//    "http://techcrunch.com/2014/12/15/hbo-go-finally-comes-to-amazon-fire-tv/",
//    "http://techcrunch.com/2014/12/15/the-tonewoodamp-blows-amplified-sound-out-of-your-guitars-sound-hole/",
//    "http://techcrunch.com/2014/12/15/the-nest-thermostat-can-now-be-controlled-by-voice/",
    "http://curbed.com/archives/2014/12/15/winn-wittman-soaring-wings-austin-for-sale.php",
    "http://curbed.com/archives/2014/12/15/le-palais-royale-florida-photos.php",
    "http://curbed.com/archives/2014/12/12/mint-hill-north-carolina-home-for-sale.php",
    "http://curbed.com/archives/2014/12/15/la-shed-architecture-de-gaspe-renovation.php",
    "http://curbed.com/archives/2014/12/15/minimalist-holiday-decorating.php",
    "http://curbed.com/archives/2014/12/15/brutalism-the-game-bbc-tv-show.php",
    "http://sf.curbed.com/archives/2014/08/06/how_a_san_francisco_architect_reframes_design_for_the_blind.php",
    "http://curbed.com/archives/2014/12/15/masculine-feminine-design-decor-writing.php",
    "http://curbed.com/archives/2014/12/15/nyc-american-museum-of-natural-history-jeanne-gang.php",
    "http://sf.curbed.com/archives/2014/12/12/a_look_inside_brewcade_2015_zagat_results_state_bird_sequel_almost_here_more.php",
  };
  public static void main(String args[]) throws Exception {
    SiteParser siteParser = new SiteParser();
    for (String url : urls) {
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

//      SortedMultiset<String> names = TreeMultiset.create();
//      SortedMultiset<String> organizations = TreeMultiset.create();
//      SortedMultiset<String> locations = TreeMultiset.create();

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

//        for (String name : Tokenizer.getNames(paragraphText)) {
//          names.add(name);
//        }
//        for (String organization : Tokenizer.getOrganizations(paragraphText)) {
//          organizations.add(organization);
//        }
//        for (String location : Tokenizer.getLocations(paragraphText)) {
//          locations.add(location);
//        }
//      }
//      for (String name : names.descendingMultiset()) {
//        System.out.println("NAME: " + name + " (" + names.count(name) + " occurrences)");
//      }
//      for (String organization : organizations.descendingMultiset()) {
//        System.out.println("ORG: " + organization + " (" + organizations.count(organization) +
//            " occurrences)");
//      }
//      for (String location : locations.descendingMultiset()) {
//        System.out.println("LOCATION: " + location + " (" + locations.count(location) +
//            " occurrences)");
//      }

    }
  }
}
