package com.janknspank.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.Links;
import com.janknspank.bizness.Urls;
import com.janknspank.crawler.Interpreter;
import com.janknspank.crawler.RequiredFieldException;
import com.janknspank.crawler.UrlCleaner;
import com.janknspank.crawler.UrlWhitelist;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.InterpretedData;
import com.janknspank.proto.CoreProto.Url;

/**
 * This class takes files that have been previously crawled on the current
 * computer and "reinterprets" them - as though they're being crawled again.
 * The previously crawled files are read from the /data/ directory.  This is
 * useful if we purge the database and want a quick way to reconstitute our
 * schema without a bunch of network calls to news sites.
 */
public class ReinterpretCachedData {
  private static final Pattern FILE_PATTERN =
      Pattern.compile("^([a-zA-Z\\-\\_0-9]{22,24})\\.html$");

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
        Links.deleteFromOriginUrlId(ImmutableList.of(url.getId()));
      }

      System.out.println("Interpreting: " + url.getUrl());

      // Save this article and its keywords.
      Reader fileReader = null;
      try {
        fileReader = new FileReader(dataFile);
        InterpretedData interpretedData = Interpreter.interpret(url, fileReader);
        Database.insert(interpretedData.getArticle());

        // Make sure to filter and clean the URLs - only store the ones we want to crawl!
        Iterable<Url> destinationUrls = Urls.put(
            Iterables.transform(
                Iterables.filter(interpretedData.getUrlList(), UrlWhitelist.PREDICATE),
                UrlCleaner.TRANSFORM_FUNCTION),
            url.getUrl());
        Links.put(url, destinationUrls);
        Urls.markCrawlFinish(url);

      } catch (DatabaseSchemaException | DatabaseRequestException | FileNotFoundException e) {
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
