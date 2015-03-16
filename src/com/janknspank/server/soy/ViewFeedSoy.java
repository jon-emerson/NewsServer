package com.janknspank.server.soy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.google.api.client.util.Lists;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.bizness.Users;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;
import com.janknspank.server.AbstractArticlesServlet;

public class ViewFeedSoy {
  public static SoyListData toSoyListData(
      final Iterable<Article> articles,
      final User user,
      final Function<Article, Double> scoreFunction) {
    Set<String> userKeywordSet = Users.getUserKeywordSet(user);

    List<SoyMapData> articleSoyMapDataList = Lists.newArrayList();
    int i = 0;
    for (Article article : articles) {
      SoyMapData articleSoyMapData = new SoyMapData(
          "title", article.getTitle(),
          "url", article.getUrl(),
          "urlId", article.getUrlId(),
          "description", article.getDescription(),
          "image_url", article.getImageUrl(),
          "score", scoreFunction.apply(article),
          "inputNodes", getInputNodesString(user, article),
          "keywords", getKeywordsString(article),
          "features", getFeaturesString(article));

      // Article number.
      if (++i <= 10) {
        articleSoyMapData.put("number", Integer.toString(i));
      }

      // Tags.
      List<String> highlightedTags = Lists.newArrayList();
      List<String> unhighlightedTags = Lists.newArrayList();
      for (ArticleKeyword keyword : Articles.getBestKeywords(article, userKeywordSet)) {
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
      articleSoyMapData.put("fb_score", (int) engagement.getShareScore());
      articleSoyMapData.put("fb_likes", (int) engagement.getLikeCount());
      articleSoyMapData.put("fb_shares", (int) engagement.getShareCount());
      articleSoyMapData.put("fb_comments" , (int) engagement.getCommentCount());

      // Attribution.
      articleSoyMapData.put("attribution" , getAttribution(article));

      // Attribution.
      articleSoyMapData.put("rawArticle", "article = "
          + AbstractArticlesServlet.serialize(article, userKeywordSet, i).toString(2));

      articleSoyMapDataList.add(articleSoyMapData);
    }
    return new SoyListData(articleSoyMapDataList);
  }

  private static String getInputNodesString(User user, Article article) {
    return Joiner.on("\n").withKeyValueSeparator("=").join(
        NeuralNetworkScorer.generateInputNodes(user, article));
  }

  private static String getFeaturesString(Article article) {
    StringBuilder sb = new StringBuilder();
    sb.append("Features = [");
    int i = 0;
    for (ArticleFeature articleFeature : article.getFeatureList()) {
      if (i++ == 0) {
        sb.append(", ");
      }
      JSONObject o = new JSONObject();
      o.put("id", articleFeature.getFeatureId());
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
      if (i++ == 0) {
        sb.append(",");
      }
      sb.append(Serializer.toJSON(keyword).toString(2));
    }
    sb.append("]");
    return sb.toString();
  }

  private static String getAttribution(Article article) {
    String time;
    long age = System.currentTimeMillis() - Articles.getPublishedTime(article);
    if (age < TimeUnit.MINUTES.toMillis(60)) {
      time = Long.toString(TimeUnit.MINUTES.convert(age, TimeUnit.MILLISECONDS)) + "m";
    } else if (age < TimeUnit.HOURS.toMillis(24)) {
      time = Long.toString(TimeUnit.HOURS.convert(age, TimeUnit.MILLISECONDS)) + "h";
    } else {
      time = Long.toString(TimeUnit.DAYS.convert(age, TimeUnit.MILLISECONDS)) + "d";
    }

    String domain = "";
    try {
      domain = new URL(article.getUrl()).getHost().replaceAll("^www\\.", "");
    } catch (MalformedURLException e) {}

    return time + " ago - " + domain;
  }
}
