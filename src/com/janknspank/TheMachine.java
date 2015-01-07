package com.janknspank;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.common.UrlCleaner;
import com.janknspank.common.UrlWhitelist;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.Links;
import com.janknspank.data.Urls;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.interpreter.Interpreter;
import com.janknspank.interpreter.RequiredFieldException;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Interpreter.InterpretedData;

public class TheMachine {
  public void start() throws DataInternalException {
    String originUrl = "http://www.nytimes.com/";
    Urls.put(originUrl, false);

    while (true) {
      final Url startUrl = Urls.markAsCrawled(Urls.getNextUrlToCrawl());
//      final Url startUrl = Url.newBuilder()
//          .setId("pYZDE7M36zxQNxbFTUVFCQ")
//          .setUrl("http://techcrunch.com/2015/01/03/the-sharing-economy-and-the-"
//              + "future-of-finance/")
//          .setTweetCount(0)
//          .setDiscoveryTime(System.currentTimeMillis())
//          .build();
      if (startUrl == null) {
        // Some other thread has likely claimed this URL - Go get another.
        continue;
      }

      if (!UrlWhitelist.isOkay(startUrl.getUrl())) {
        System.err.println("Removing now-blacklisted page: " + startUrl.getUrl());
        Links.deleteIds(ImmutableList.of(startUrl.getId()));
        Database.delete(startUrl);
        continue;
      }

      System.err.println("Crawling: " + startUrl.getUrl());

      // Save this article and its keywords.
      try {
        InterpretedData interpretedData = Interpreter.interpret(startUrl);
        Database.insert(interpretedData.getArticle());
        Database.insert(interpretedData.getKeywordList());

        // Make sure to filter and clean the URLs - only store the ones we want to crawl!
        Collection<Url> destinationUrls = Urls.put(
            Iterables.transform(
                Iterables.filter(interpretedData.getUrlList(), UrlWhitelist.PREDICATE),
                UrlCleaner.TRANSFORM_FUNCTION),
            false /* isTweet */);
        Links.put(startUrl, destinationUrls);

      } catch (ValidationException e) {
        // Internal error (bug in our code).
        e.printStackTrace();
      } catch (FetchException|ParserException|RequiredFieldException e) {
        // Bad article.
        e.printStackTrace();
      }
    }
  }

  public static void main(String args[]) throws Exception {
    new TheMachine().start();
  }
}
