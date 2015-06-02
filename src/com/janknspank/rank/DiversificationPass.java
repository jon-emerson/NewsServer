package com.janknspank.rank;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.common.TopList;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.server.soy.ViewFeedSoy;

/**
 * This class takes an ordered list of Articles and redistributes them so that
 * the same topics aren't mentioned sequentially, unless that's all we got.
 */
public abstract class DiversificationPass {
  public abstract Iterable<Article> diversify(Iterable<Article> articles);

  /**
   * This is the diversification strategy to use on user's main streams.  All
   * aspects of articles are used to encourage diversity.
   */
  public static class MainStreamPass extends StandardDiversificationPass {
    public MainStreamPass() {
      super(true);
    }
  }

  /**
   * This is the diversification strategy to use on a stream for a particular
   * industry.  The differences in article industries are not used.
   */
  public static class IndustryStreamPass extends StandardDiversificationPass {
    public IndustryStreamPass() {
      super(false);
    }
  }

  /**
   * This is the diversification strategy to use on streams where
   * diversification is not important.  E.g. when looking for news on a
   * specific company.
   */
  public static class NoOpPass extends DiversificationPass {
    @Override
    public synchronized Iterable<Article> diversify(Iterable<Article> articles) {
      return articles;
    }
  }

  private static abstract class StandardDiversificationPass extends DiversificationPass {
    private static final int MAX_INDUSTRIES_PER_ARTICLE = 3;
    private static final int LOOK_BEHIND_COUNT = 3;
    private static final int CANDIDATE_ARTICLE_BUCKET_SIZE = 8;

    private final boolean enableIndustryDiversification;

    private final Map<Article, Set<String>> articleKeywords = Maps.newHashMap();
    private final Map<Article, Set<String>> articleStems = Maps.newHashMap();
    private final Map<Article, Set<FeatureId>> articleIndustryFeatureIds = Maps.newHashMap();
    private final Map<Article, String> domainMap = Maps.newHashMap();

    public StandardDiversificationPass(boolean enableIndustryDiversification) {
      this.enableIndustryDiversification = enableIndustryDiversification;
    }

    @Override
    public synchronized Iterable<Article> diversify(Iterable<Article> articles) {
      articleKeywords.clear();
      articleStems.clear();
      articleIndustryFeatureIds.clear();
      domainMap.clear();

      for (Article article : articles) {
        Set<String> keywordSet = Sets.newHashSet();
        for (ArticleKeyword articleKeyword : article.getKeywordList()) {
          if (articleKeyword.getStrength() > 30) {
            keywordSet.add(articleKeyword.getKeyword().toLowerCase());
          }
        }
        articleKeywords.put(article, keywordSet);

        TopList<FeatureId, Double> topFeatureIds = new TopList<>(MAX_INDUSTRIES_PER_ARTICLE);
        for (ArticleFeature articleFeature : article.getFeatureList()) {
          FeatureId featureId = FeatureId.fromId(articleFeature.getFeatureId());
          if (featureId != null && featureId.getFeatureType() == FeatureType.INDUSTRY) {
            topFeatureIds.add(featureId, articleFeature.getSimilarity());
          }
        }
        articleIndustryFeatureIds.put(article, ImmutableSet.copyOf(topFeatureIds));

        articleStems.put(article, ImmutableSet.copyOf(article.getDedupingStemsList()));
        domainMap.put(article, ViewFeedSoy.getDomain(article));
      }

      // As we choose each article, look ahead at CANDIDATE_ARTICLE_BUCKET_SIZE
      // articles.  Choose the most differentiated article as the next article
      // to show the user in his stream.
      Set<Article> workingSet = Sets.newLinkedHashSet(articles);
      List<Article> articlesChosenSoFar = Lists.newArrayList();
      while (!workingSet.isEmpty()) {
        Article bestNextArticle = null;
        int bestNextArticleScore = Integer.MIN_VALUE;
        for (Article article : Iterables.limit(workingSet, CANDIDATE_ARTICLE_BUCKET_SIZE)) {
          int score = getDifferentiationScore(article, articlesChosenSoFar);
          if (score > bestNextArticleScore) {
            bestNextArticle = article;
            bestNextArticleScore = score;
          }
        }
        articlesChosenSoFar.add(bestNextArticle);
        workingSet.remove(bestNextArticle);
      }
      return ImmutableList.copyOf(articlesChosenSoFar);
    }

    private int getDifferenceBetween(Article article, Article article2) {
      int sharedKeywords = Sets.intersection(
          articleKeywords.get(article), articleKeywords.get(article2)).size();
      int sharedStems = Sets.intersection(
          articleStems.get(article), articleStems.get(article2)).size();
      int domainShared = domainMap.get(article).equals(domainMap.get(article2)) ? 5 : 0;
      int score = 100 - (sharedKeywords * 20 + sharedStems + domainShared);

      // In the main stream, try to show articles from all the user's industries.
      // In topic streams, don't try to do this.
      if (enableIndustryDiversification) {
        // The value of this is in (0, MAX_INDUSTRIES_PER_ARTICLE * 2), which is currently
        // between 0 and 6.
        int industryDifferences = Sets.symmetricDifference(
            articleIndustryFeatureIds.get(article), articleIndustryFeatureIds.get(article2)).size();
        // Increase the score by up to 18 based on industry differences.
        score += (industryDifferences * industryDifferences) / 2;
      }

      return score;
    }

    /**
     * Returns a score for how different this article is versus previous articles
     * that we've added to the distributed list.  If the passed article has no
     * keywords in common and no industries in common with previous articles, it
     * will get the highest score.  As the passed article has more attributes that
     * are similar to articles previously selected, its score will get lower.
     */
    private int getDifferentiationScore(Article article, List<Article> articlesChosenSoFar) {
      int score = 0;
      for (int i = 1; i <= LOOK_BEHIND_COUNT; i++) {
        if (articlesChosenSoFar.size() >= i) {
          int differenceBetweenIndex = getDifferenceBetween(article,
              articlesChosenSoFar.get(articlesChosenSoFar.size() - i));
          score += (differenceBetweenIndex / i);
        } else {
          score += 120 / i;
        }
      }
      return score;
    }
  }
}
