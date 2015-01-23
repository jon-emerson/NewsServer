package com.janknspank.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.common.UrlCleaner;
import com.janknspank.common.UrlWhitelist;
import com.janknspank.data.ArticleKeywords;
import com.janknspank.data.Database;
import com.janknspank.data.Links;
import com.janknspank.data.Urls;
import com.janknspank.data.ValidationException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.interpreter.Interpreter;
import com.janknspank.interpreter.RequiredFieldException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;
import com.janknspank.proto.Interpreter.InterpretedData;

public class ReinterpretCachedData {
  private static final Pattern FILE_PATTERN =
      Pattern.compile("^([a-zA-Z\\-\\_0-9]{22})\\.html$");

  public static void main(String args[]) throws Exception {
    File dataDirectory = new File("data/");
    for (File dataFile : dataDirectory.listFiles()) {
      Matcher matcher = FILE_PATTERN.matcher(dataFile.getName());
      if (!matcher.matches()) {
        continue;
      }
      String urlId = matcher.group(1);

      Url url = Urls.getById(urlId);
      if (url == null) {
        System.out.println("WARNING: Could not find URL for article: " + urlId);
        continue;
      }
      if (url.hasLastCrawlFinishTime() && url.getLastCrawlFinishTime() > 1000) {
        continue;
      }

      // Kinda hacky, but helps us handle errors better for utility purposes.
      if (!url.hasLastCrawlStartTime()) {
        url = Urls.markCrawlStart(url);
      } else {
        System.out.println("Cleaning old data for URL: " + url.getUrl());
        Database.with(Article.class).delete(url.getId());
        ArticleKeywords.deleteForUrlIds(ImmutableList.of(url.getId()));
        Links.deleteFromOriginUrlId(ImmutableList.of(url.getId()));
      }

      System.out.println("Interpreting: " + url.getUrl());

      // Save this article and its keywords.
      Reader fileReader = null;
      try {
        fileReader = new FileReader(dataFile);
        InterpretedData interpretedData = Interpreter.interpret(url, fileReader);
        Database.insert(interpretedData.getArticle());
        Database.insert(interpretedData.getKeywordList());

        // Make sure to filter and clean the URLs - only store the ones we want to crawl!
        Iterable<Url> destinationUrls = Urls.put(
            Iterables.transform(
                Iterables.filter(interpretedData.getUrlList(), UrlWhitelist.PREDICATE),
                UrlCleaner.TRANSFORM_FUNCTION),
            false /* isTweet */);
        Links.put(url, destinationUrls);
        Urls.markCrawlFinish(url);

      } catch (ValidationException | FileNotFoundException e) {
        // Internal error (bug in our code).
        e.printStackTrace();
      } catch (FetchException|ParserException|RequiredFieldException e) {
        // Bad article.
        e.printStackTrace();
      } finally {
        if (fileReader != null) {
          fileReader.close();
        }
      }
    }
  }
}
