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
public class DistributionPass {
  private final Map<Article, Set<String>> articleKeywords = Maps.newHashMap();
  private final Map<Article, Set<String>> articleStems = Maps.newHashMap();
  private final Map<Article, Set<FeatureId>> articleIndustryFeatureIds = Maps.newHashMap();
  private final Map<Article, String> domainMap = Maps.newHashMap();
  private final List<Article> distributedList;

  public DistributionPass(Iterable<Article> articles) {
    for (Article article : articles) {
      Set<String> keywordSet = Sets.newHashSet();
      for (ArticleKeyword articleKeyword : article.getKeywordList()) {
        if (articleKeyword.getStrength() > 10) {
          keywordSet.add(articleKeyword.getKeyword().toLowerCase());
        }
      }
      articleKeywords.put(article, keywordSet);

      TopList<FeatureId, Double> topFeatureIds = new TopList<>(3);
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

    // For each article, look ahead 10 articles.  Choose the most differentiated
    // article as the next article to show the user in his stream.
    Set<Article> workingSet = Sets.newLinkedHashSet(articles);
    List<Article> articlesChosenSoFar = Lists.newArrayList();
    while (!workingSet.isEmpty()) {
      Article bestNextArticle = null;
      int bestNextArticleScore = Integer.MIN_VALUE;
      for (Article article : Iterables.limit(workingSet, 10)) {
        int score = getDifferentiationScore(article, articlesChosenSoFar);
        if (score > bestNextArticleScore) {
          bestNextArticle = article;
          bestNextArticleScore = score;
        }
      }
      articlesChosenSoFar.add(bestNextArticle);
      workingSet.remove(bestNextArticle);
    }
    distributedList = ImmutableList.copyOf(articlesChosenSoFar);
  }

  public List<Article> getList() {
    return distributedList;
  }

  private int getDifferenceBetween(Article article, Article article2) {
    int sharedKeywords = Sets.intersection(
        articleKeywords.get(article), articleKeywords.get(article2)).size();
    int sharedIndustries = Sets.intersection(
        articleIndustryFeatureIds.get(article), articleIndustryFeatureIds.get(article2)).size();
    int sharedStems = Sets.intersection(
        articleStems.get(article), articleStems.get(article2)).size();
    int domainShared = domainMap.get(article).equals(domainMap.get(article2)) ? 2 : 0;
    return 100 - (sharedKeywords * 3 + sharedStems + domainShared)
        + ((sharedIndustries == 0) ? 20 : 0);
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
    for (int i = 1; i <= 4; i++) {
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
