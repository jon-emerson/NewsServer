package com.janknspank.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.protobuf.TextFormat;
import com.janknspank.crawler.ArticleUrlDetector;
import com.janknspank.crawler.Interpreter;
import com.janknspank.crawler.RequiredFieldException;
import com.janknspank.crawler.UrlWhitelist;
import com.janknspank.fetch.FetchException;
import com.janknspank.proto.CoreProto.Url;

/**
 * Utility class that grabs a single Article and prints the results to console.
 * The database is not modified.
 */
public class GrabArticle {
  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("Usage: bin/grab.sh http://path/to/url");
      System.exit(-1);
    }
    String urlString = args[0];

    // Validate the URL.
    try {
      new URL(urlString);
    } catch (MalformedURLException e) {
      System.out.println("Bad URL: " + urlString);
      System.exit(-1);
    }
    System.out.println("Processing URL: " + urlString);

    // Check URL whitelist.
    if (!UrlWhitelist.isOkay(urlString)) {
      System.out.println("URL is blacklisted.");
      return;
    }

    // Check Article regexes.
    if (!ArticleUrlDetector.isArticle(urlString)) {
      System.out.println("URL is not an article.");
      return;
    }

    // Grab the URL and print out the results.
    try {
      PrintWriter writer = new PrintWriter(System.out);
      TextFormat.print(
          Interpreter.interpret(Url.newBuilder().setUrl(urlString).build()).getArticle(),
          writer);
      writer.flush();
    } catch (IOException | FetchException | RequiredFieldException e) {
      e.printStackTrace();
    }
  }
}
