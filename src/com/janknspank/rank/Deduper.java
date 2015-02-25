package com.janknspank.rank;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
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
    private final Set<Integer> top3Industries = Sets.newHashSet();

    public ArticleExtraction(Article article) {
      publishTime = article.getPublishedTime();
      stems.addAll(article.getDedupingStemsList());

      // Finding a 1-in-3 match out-performed 1-in-2, 1-in-4, 1-in-5, and 2-in-5
      // when considering dupe matches (10 points), false positives (dupes that
      // are not really dupes, -2 points), and false negatives (undetected
      // dupes, -5 points).
      TopList<Integer, Double> topIndustryFeatures = new TopList<>(3);
      for (ArticleFeature feature : article.getFeatureList()) {
        if (FeatureId.fromId(feature.getFeatureId()).getFeatureType() == FeatureType.INDUSTRY) {
          topIndustryFeatures.add(feature.getFeatureId(), feature.getSimilarity());
        }
      }
      top3Industries.addAll(topIndustryFeatures.getKeys());
    }

    public boolean isDuplicate(ArticleExtraction extraction2) {
      if (Math.abs(extraction2.publishTime - publishTime) > STEM_INTERSECTION_PUBLISH_DATE_RANGE) {
        return false;
      }
      int stemIntersectionCount = Sets.intersection(stems, extraction2.stems).size();
      int industryIntersectionCount =
          Sets.intersection(top3Industries, extraction2.top3Industries).size();
      return stemIntersectionCount >= STEM_INTERSECTION_COUNT_MINIMUM
          && industryIntersectionCount >= 1;
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
  public static Iterable<Article> filterOutDupes(Iterable<Article> articles) {
    // This cache saves us about 30ms when de-duping 100 articles.
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
            nonDupeExtractions.set(i, extraction);
            System.out.println("Dupe removed: " + extractionMap.get(nonDupeExtraction).getTitle()
                + " (Better article: " + extractionMap.get(extraction).getTitle() + ")");
          } else {
            System.out.println("Dupe removed: " + extractionMap.get(extraction).getTitle()
                + " (Better article: " + extractionMap.get(nonDupeExtraction).getTitle() + ")");
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
    return Iterables.transform(nonDupeExtractions, new Function<ArticleExtraction, Article>() {
      @Override
      public Article apply(ArticleExtraction extraction) {
        return extractionMap.get(extraction);
      }
    });
  }
}
