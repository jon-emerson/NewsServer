package com.janknspank.interpreter;

import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Interpreter.InterpretedData;

/**
 * Built off the power of SiteParser, this class further interprets a web page
 * by taking the paragraphs and breaking them down into sentences, tokens, and
 * then into people, organizations, and locations.
 */
public class Interpreter {
  private static final Fetcher FETCHER = new Fetcher();

  public static InterpretedData interpret(Url url)
      throws FetchException, ParserException, RequiredFieldException {
    FetchResponse response = FETCHER.fetch(url);

    String urlId = url.getId();
    DocumentNode documentNode = DocumentBuilder.build(url.getUrl(), response.getReader());

    return InterpretedData.newBuilder()
        .setArticle(ArticleCreator.create(urlId, documentNode))
        .addAllKeyword(KeywordFinder.findKeywords(urlId, documentNode))
        .addAllUrl(UrlFinder.findUrls(documentNode))
        .build();
  }
}
