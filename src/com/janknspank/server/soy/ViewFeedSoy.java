package com.janknspank.server.soy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.bizness.UserInterests;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;
import com.janknspank.server.ArticleSerializer;

public class ViewFeedSoy {
  public static SoyListData toSoyListData(
      final Iterable<Article> articles,
      final User user) {
    Set<String> userKeywordSet =
        UserInterests.getUserKeywordSet(user, ImmutableSet.<InterestType>of());

    List<SoyMapData> articleSoyMapDataList = Lists.newArrayList();
    int i = 0;
    for (Article article : articles) {
      SoyMapData articleSoyMapData = new SoyMapData(
          "title", article.getTitle(),
          "url", article.getUrl(),
          "urlId", article.getUrlId(),
          "description", article.getDescription(),
          "score", article.getScore(),
          "neuralNetwork", getNeuralNetworkString(user, article),
          "keywords", getKeywordsString(article),
          "features", getFeaturesString(user, article));

      // Image url.
      if (article.hasImageUrl()) {
        articleSoyMapData.put("image_url", article.getImageUrl());
      }

      // Article number.
      if (++i <= 10) {
        articleSoyMapData.put("number", Integer.toString(i));
      }

      // Tags.
      List<String> highlightedTags = Lists.newArrayList();
      List<String> unhighlightedTags = Lists.newArrayList();
      Set<Integer> userIndustryFeatureIdIds = UserInterests.getUserIndustryFeatureIdIds(user);
      for (ArticleKeyword keyword
          : ArticleSerializer.getBestKeywords(article, userKeywordSet, userIndustryFeatureIdIds)) {
        String keywordStr = keyword.getKeyword();
        if (userKeywordSet.contains(keywordStr.toLowerCase())) {
          highlightedTags.add(keywordStr);
        } else {
          unhighlightedTags.add(keywordStr);
        }
      }
      articleSoyMapData.put("highlightedTags", highlightedTags);
      articleSoyMapData.put("unhighlightedTags", unhighlightedTags);

      // Social engagement.
      SocialEngagement engagement =
          SocialEngagements.getForArticle(article, SocialEngagement.Site.FACEBOOK);
      if (engagement == null) {
        engagement = SocialEngagement.getDefaultInstance();
      }
      articleSoyMapData.put("fb_score", engagement.getShareScore());
      articleSoyMapData.put("fb_likes", (int) engagement.getLikeCount());
      articleSoyMapData.put("fb_shares", (int) engagement.getShareCount());
      articleSoyMapData.put("fb_comments" , (int) engagement.getCommentCount());

      // Attribution.
      articleSoyMapData.put("attribution" ,
          ArticleSerializer.getClientDate(article) + " - " + getDomain(article));

      // Attribution.
      articleSoyMapData.put("rawArticle", "article = "
          + ArticleSerializer.serialize(article, userKeywordSet, userIndustryFeatureIdIds, i)
              .toString(2));

      articleSoyMapDataList.add(articleSoyMapData);
    }
    return new SoyListData(articleSoyMapDataList);
  }

  private static String getNeuralNetworkString(User user, Article article) {
    LinkedHashMap<String, Double> neuralNetworkInputNodes =
        NeuralNetworkScorer.generateInputNodes(user, article);
    double score = NeuralNetworkScorer.getInstance().getScore(neuralNetworkInputNodes);
    return Joiner.on("\n").withKeyValueSeparator(" = ").join(neuralNetworkInputNodes)
        + "\n\noutput -> " + score;
  }

  private static String getFeaturesString(User user, Article article) {
    List<ArticleFeature> features = Lists.newArrayList(article.getFeatureList());
    features.sort(new Comparator<ArticleFeature>() {
      @Override
      public int compare(ArticleFeature feature1, ArticleFeature feature2) {
        return -Double.compare(feature1.getSimilarity(), feature2.getSimilarity());
      }
    });
    Set<Integer> userFeatureIds = Sets.newHashSet();
    for (Interest interest : UserInterests.getInterests(user)) {
      if (interest.hasIndustryCode()) {
        userFeatureIds.add(interest.getIndustryCode());
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append("features = [");
    int i = 0;
    for (ArticleFeature articleFeature : features) {
      if (i++ != 0) {
        sb.append(", ");
      }
      JSONObject o = new JSONObject();
      o.put("id", articleFeature.getFeatureId());
      if (userFeatureIds.contains(articleFeature.getFeatureId())) {
        o.put("isUserInterest", true);
      }
      FeatureId featureId = FeatureId.fromId(articleFeature.getFeatureId());
      o.put("description", featureId != null
          ? (featureId.getFeatureType().name() + ": " + featureId.getTitle())
          : "UNKNOWN!");
      o.put("similarity", articleFeature.getSimilarity());
      sb.append(o.toString(2));
    }
    sb.append("]");
    return sb.toString();
  }

  private static String getKeywordsString(Article article) {
    List<ArticleKeyword> keywords = Lists.newArrayList(article.getKeywordList());
    keywords.sort(new Comparator<ArticleKeyword>() {
      @Override
      public int compare(ArticleKeyword keyword1, ArticleKeyword keyword2) {
        return -Integer.compare(keyword1.getStrength(), keyword2.getStrength());
      }
    });
    StringBuilder sb = new StringBuilder();
    sb.append("keywords = [");
    int i = 0;
    for (ArticleKeyword keyword : keywords) {
      if (i++ != 0) {
        sb.append(", ");
      }
      sb.append(Serializer.toJSON(keyword).toString(2));
    }
    sb.append("]");
    return sb.toString();
  }

  public static String getDomain(Article article) {
    try {
      return new URL(article.getUrl()).getHost().replaceAll("^www\\.", "");
    } catch (MalformedURLException e) {}
    return "";
  }
}
