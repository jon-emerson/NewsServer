package com.janknspank.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.ArticleProto.DuplicateArticle;
import com.janknspank.rank.Deduper;
import com.janknspank.rank.RankException;

public class FindDupes {
  public static final double DUPLICATE_SIMILARITY_THRESHOLD = 0.25;
  public static final long PUBLISHED_TIME_WINDOW_FOR_DUPES = TimeUnit.HOURS.toMillis(6);
  /**
   * Queries the database for a set of articles that are potential duplicates based
   * on keyword matches and time published. Then checks similarities and returns
   * all articles that are actually duplicates. This method is used during
   * crawl time to store dupes on Article objects.
   */
  public static Iterable<DuplicateArticle> findDupes(ArticleOrBuilder article) 
      throws RankException {
    // Query for articles that were published within 1 day of this article
    // and contain any of the same keywords
    List<String> keywords = new ArrayList<>();
    for (ArticleKeyword articleKeyword : article.getKeywordList()) {
      keywords.add(articleKeyword.getKeyword());
    }

    Iterable<Article> possibleDupes;
    try {
      possibleDupes = Database.with(Article.class).get(
          new QueryOption.WhereEquals("keyword.keyword", keywords),
          new QueryOption.DescendingSort("published_time"),
          new QueryOption.Limit(50));
    } catch (DatabaseSchemaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new RankException("Can't query for potential duplicates: " + e.getMessage(), e);
    }

    ArrayList<DuplicateArticle> dupes = new ArrayList<>();
    for (Article possibleDupe : possibleDupes) {
      if (publishedTimeDifference(article, possibleDupe) < PUBLISHED_TIME_WINDOW_FOR_DUPES) {
        double similarity = Deduper.similarity(article, possibleDupe);

        if (similarity > DUPLICATE_SIMILARITY_THRESHOLD) {
          dupes.add(DuplicateArticle.newBuilder()
              .setUrlId(possibleDupe.getUrlId())
              .setSimilarity(similarity)
              .build());
        }
      }
    }

    return dupes;
  }
  
  public static long publishedTimeDifference(ArticleOrBuilder article1, ArticleOrBuilder article2) {
    return Math.abs(article1.getPublishedTime() - article2.getPublishedTime());
  }
}
