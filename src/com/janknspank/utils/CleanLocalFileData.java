package com.janknspank.utils;

import java.io.File;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;
import com.janknspank.database.Database;
import com.janknspank.proto.Core.Article;

/**
 * Deletes all the files in /data/ that don't correspond to a crawled article.
 */
public class CleanLocalFileData {
  private static final Pattern FILE_PATTERN =
      Pattern.compile("^([a-zA-Z\\-\\_0-9]{22})\\.html$");

  public static void main(String args[]) throws Exception {
    // Figure out what articles we've crawled already.
    Set<String> crawledArticleIds = Sets.newHashSet();
    for (Article article : Database.with(Article.class).get()) {
      crawledArticleIds.add(article.getUrlId());
    }

    File dataDirectory = new File("data/");
    int filesDeleted = 0;
    for (File dataFile : dataDirectory.listFiles()) {
      Matcher matcher = FILE_PATTERN.matcher(dataFile.getName());
      if (matcher.matches()) {
        String articleId = matcher.group(1);
        if (!crawledArticleIds.contains(articleId)) {
          dataFile.delete();
          filesDeleted++;
        }
      }
    }
    System.out.println(filesDeleted + " files deleted");
  }
}
