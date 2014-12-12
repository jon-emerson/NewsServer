package com.janknspank.dom;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.xml.sax.SAXException;

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

  private static final String[] urls = {
    "http://www.nytimes.com/2014/02/23/opinion/sunday/friedman-how-to-get-a-job-at-google.html",
    "http://www.nytimes.com/2014/12/09/technology/esports-colleges-breeding-grounds-professional-gaming.html",
    "http://www.nytimes.com/2014/12/04/us/politics/roy-h-beck-quietly-leads-a-grass-roots-army.html",
    "http://www.nytimes.com/2014/09/25/technology/exposing-hidden-biases-at-google-to-improve-diversity.html",
    "http://www.nytimes.com/2014/12/08/business/obamas-net-neutrality-bid-divides-civil-rights-groups.html",
    "http://bits.blogs.nytimes.com/2014/12/08/ipod-lawsuit-continues-with-plaintiffs-status-still-in-doubt/",
    "http://dealbook.nytimes.com/2014/12/08/tech-figures-join-to-fund-change-org-petition-site/",
    "http://bits.blogs.nytimes.com/2014/12/08/european-privacy-debate-on-display-in-paris/",
    "http://www.nytimes.com/2014/12/09/world/asia/new-delhi-bans-uber-after-driver-is-accused-of-rape.html",
    "http://bits.blogs.nytimes.com/2014/12/08/a-trip-to-california-for-chinas-internet-czar/",
    "http://www.nytimes.com/2014/12/08/technology/grappling-with-the-culture-of-free-in-napsters-aftermath.html",
    "http://www.nytimes.com/2014/12/08/business/north-korea-denies-hacking-sony-but-calls-attack-a-righteous-deed.html",
    "http://www.nytimes.com/2014/12/08/business/ralph-h-baer-dies-inventor-of-odyssey-first-system-for-home-video-games.html",
    "http://bits.blogs.nytimes.com/2014/12/07/how-i-made-26-cents-with-the-latest-in-sharing-economy-apps/",
    "http://www.nytimes.com/2014/12/07/upshot/how-technology-could-help-fight-income-inequality.html",
    "http://bits.blogs.nytimes.com/2014/12/05/uber-to-portland-were-here-deal-with-it/",
    "http://www.nytimes.com/2014/12/08/business/international/nurturing-start-up-culture-in-the-lower-cost-balkans.html",
    "http://www.nytimes.com/2014/12/07/jobs/how-ibm-brings-ideas-forward-from-its-teams.html",
    "http://bits.blogs.nytimes.com/2014/12/05/in-suit-cisco-accuses-arista-of-copying-work/",
    "http://dealbook.nytimes.com/2014/12/05/senate-to-hold-hearing-on-cyberattacks-against-finance/",
    "http://www.nytimes.com/2014/12/06/your-money/some-drawbacks-in-tapping-the-phone-to-deposit-a-check.html",
    "http://dealbook.nytimes.com/2014/12/08/with-bank-of-america-order-s-e-c-breaks-the-mold/",
    "http://dealbook.nytimes.com/2014/12/08/merck-agrees-to-acquire-drug-maker-cubist-for-9-5-billion/",
    "http://www.nytimes.com/2014/12/09/business/hotels-let-guests-borrow-items-or-leave-them.html",
    "http://www.nytimes.com/2014/12/09/business/media/madison-avenue-sees-rough-times-ahead-tempered-by-growth.html"
  };
  public static void main(String args[]) throws Exception {
    for (String url : urls) {
      // Open a file for writing all the paragraphs and sentences.
      String filename = url;
      if (filename.endsWith("/")) {
        filename = url.substring(0, filename.length() - "/".length());
      } if (filename.endsWith(".html")) {
        filename = url.substring(0, filename.length() - ".html".length());
      }
      filename = filename.substring(filename.lastIndexOf("/") + 1);
      FileOutputStream fos = new FileOutputStream("trainingdata/" + filename + ".txt");
      System.err.println("Writing to " + filename + ".txt ...");

      // Get all the paragraphs.
      DocumentNode node = crawl(url);
      List<Node> paragraphs = new ArrayList<>();
      paragraphs.addAll(node.findAll("article > p"));
      paragraphs.addAll(node.findAll("article > div > p"));

//      SortedMultiset<String> names = TreeMultiset.create();
//      SortedMultiset<String> organizations = TreeMultiset.create();
//      SortedMultiset<String> locations = TreeMultiset.create();
//
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
