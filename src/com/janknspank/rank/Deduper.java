package com.janknspank.rank;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;

public class Deduper {
  // This is the number of matched stems we need to consider an article to be
  // a duplicate.  If our deduping stems are based on article titles only (which
  // they are, at least currently), then 2 is the only value that delivers good
  // results here.  If stems also include stems from the description or first
  // paragraph, then this value value should be 3.  (Not that we're doing this,
  // but just saying, in case we revisit our de-duping strategy.)
  private static int STEM_INTERSECTION_COUNT_MINIMUM = 2;

  // Turns out a pretty large value gives us the best results, based on this
  // score calculation:
  // int score = positives * 10 - (5 * missedDupes + 2 * falseDupes);
  private static final long STEM_INTERSECTION_PUBLISH_DATE_RANGE = TimeUnit.HOURS.toMillis(30);

  /**
   * Helper class that contains extracted values from an article that are
   * helpful for determining dupes.
   */
  private static class ArticleExtraction {
    private final long publishTime;
    private final Set<String> stems = Sets.newHashSet();

    // Marks Articles that have been responsible for the killing of a duplicate.
    // E.g. this is true if this article has won against a similar article.
    private boolean killedADupe = false;

    public ArticleExtraction(Article article) {
      publishTime = article.getPublishedTime();
      stems.addAll(article.getDedupingStemsList());
    }

    public boolean isDuplicate(ArticleExtraction extraction2) {
      if (Math.abs(extraction2.publishTime - publishTime) > STEM_INTERSECTION_PUBLISH_DATE_RANGE) {
        return false;
      }
      return Sets.intersection(stems, extraction2.stems).size() >= STEM_INTERSECTION_COUNT_MINIMUM;
    }

    public void markHasKilledDupe() {
      killedADupe = true;
    }

    public boolean hasKilledADupe() {
      return killedADupe;
    }
  }

  public static boolean isDupe(Article article1, Article article2) {
    return new ArticleExtraction(article1).isDuplicate(new ArticleExtraction(article2));
  }

  /**
   * This method filters out duplicates based on the DuplicateArticles
   * that are written on Article objects at crawl time. This is more efficient
   * than computing duplicates from scratch using cosine similarity. This
   * method should be used during getArticles, not the "dedupe" method below.
   */
  public static List<Article> filterOutDupes(Iterable<Article> articles) {
    // This cache saves us about 500ms when de-duping 100 articles.
    final Map<ArticleExtraction, Article> extractionMap = Maps.newHashMap();
    for (Article article : articles) {
      extractionMap.put(new ArticleExtraction(article), article);
    }

    List<ArticleExtraction> nonDupeExtractions = Lists.newArrayList();
    long startMillis = System.currentTimeMillis();
    for (ArticleExtraction extraction : extractionMap.keySet()) {
      boolean foundDupe = false;
      for (int i = 0; i < nonDupeExtractions.size(); i++) {
        ArticleExtraction nonDupeExtraction = nonDupeExtractions.get(i);
        if (extraction.isDuplicate(nonDupeExtraction)) {
          // Choose the one with the higher social score.
          SocialEngagement engagement =
              SocialEngagements.getForArticle(extractionMap.get(extraction), Site.FACEBOOK);
          SocialEngagement nonDupeEngagement =
              SocialEngagements.getForArticle(extractionMap.get(nonDupeExtraction), Site.FACEBOOK);
          if (nonDupeEngagement == null
              || (engagement != null
                  && engagement.getShareScore() > nonDupeEngagement.getShareScore())) {
            // The new article is more socially valuable, use it instead.
            extraction.markHasKilledDupe();
            nonDupeExtractions.set(i, extraction);
          } else {
            nonDupeExtraction.markHasKilledDupe();
          }
          foundDupe = true;
          break;
        }
      }
      if (!foundDupe) {
        nonDupeExtractions.add(extraction);
      }
    }
    System.out.println("Duplicates calculated in "
        + (System.currentTimeMillis() - startMillis) + "ms");

    List<Article> dedupedArticles = Lists.newArrayList();
    for (ArticleExtraction extraction : nonDupeExtractions) {
      if (extraction.hasKilledADupe()) {
        dedupedArticles.add(extractionMap.get(extraction).toBuilder()
            .setHot(true)
            .build());
      } else {
        dedupedArticles.add(extractionMap.get(extraction));
      }
    }
    return dedupedArticles;
  }

  public static void main(String args[]) throws Exception {
    String url1 = args[0];
    String url2 = args[1];
    Map<String, Article> articles = ArticleCrawler.getArticles(ImmutableList.of(url1, url2), false);
    Article article1 = Iterables.get(articles.values(), 0);
    Article article2 = Iterables.get(articles.values(), 1);
    System.out.println("Article 1 deduping stems: \""
        + Joiner.on("\", \"").join(article1.getDedupingStemsList()) + "\"");
    System.out.println("Article 2 deduping stems: \""
        + Joiner.on("\", \"").join(article2.getDedupingStemsList()) + "\"");
    System.out.println("Is dupe? " + isDupe(article1, article2));
  }
}
