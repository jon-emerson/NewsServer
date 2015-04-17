package com.janknspank.crawler;

import java.io.Reader;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.ArticleProto.InterpretedData;
import com.janknspank.proto.CoreProto.Url;

/**
 * Built off the power of SiteParser, this class further interprets a web page
 * by taking the paragraphs and breaking them down into sentences, tokens, and
 * then into people, organizations, and locations.
 */
public class Interpreter {
  private static final Fetcher FETCHER = new Fetcher();

  /**
   * Retrieves the passed URL by making a request to the respective website,
   * and then interprets the returned results.
   */
  public static InterpretedData interpret(Url url)
      throws FetchException, ParserException, RequiredFieldException {
    FetchResponse response = null;
    Reader reader = null;
    try {
      response = FETCHER.fetch(url.getUrl());
      if (response.getStatusCode() != HttpServletResponse.SC_OK) {
        throw new FetchException(
            "URL not found (" + response.getStatusCode() + "): " + url.getUrl());
      }
      return interpret(url, response.getDocumentNode());
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  /**
   * Advanced method: If we already have the data from the URL, use this method
   * to interpret the web page using said data.
   */
  public static InterpretedData interpret(Url url, DocumentNode documentNode)
      throws FetchException, ParserException, RequiredFieldException {

    return InterpretedData.newBuilder()
        .setArticle(ArticleCreator.create(url, documentNode))
        .addAllUrl(UrlFinder.findUrls(documentNode))
        .build();
  }

  public static void main(String args[])
      throws FetchException, ParserException, RequiredFieldException {
    if (args.length != 1 || !args[0].startsWith("http")) {
      System.out.println("Tell us what URL to interpret please... and only 1!");
    }
    System.out.println(Interpreter.interpret(Url.newBuilder().setUrl(args[0]).build()));
  }
}
