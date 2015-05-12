package com.janknspank.crawler.social;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.janknspank.bizness.Articles;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.common.DateParser;
import com.janknspank.common.Logger;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.CoreProto.Url;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.json.JsonException;
import com.restfb.json.JsonObject;

public class FacebookData {
  private static final Logger LOG = new Logger(FacebookData.class);
  private static final String CRAWLER_APP_ID = "6897962850";
  private static final String CRAWLER_APP_SECRET = "42d651b1aa4cf747e343fde5f7c33ad7";
  private static FacebookClient __crawlerClient = null;
  private static final String FRONTENT_APP_ID = "317027871839276";
  private static final String FRONTENT_APP_SECRET = "4324edc68cb6fd1ff4753b3b9ff54fdd";
  private static FacebookClient __frontendClient = null;

  private static String encodeUrl(String url) {
    // Example urlObject: http://goo.gl/JVf3tt
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.warning("Can't encode url: " + url);
      return url;
    }
  }

  public static Long getPublishTime(Url url) throws SocialException {
    String encodedURL = encodeUrl(url.getUrl());
    JsonObject urlObject = getCrawlerClient().fetchObject(encodedURL, JsonObject.class);
    if (urlObject != null
        && urlObject.has("og_object")
        && urlObject.getJsonObject("og_object").has("id")) {
      String objectId = urlObject.getJsonObject("og_object").getString("id");
      JsonObject object = getCrawlerClient().fetchObject(objectId, JsonObject.class);
      if (object != null
          && object.has("created_time")) {
        return DateParser.parseDateTime(object.getString("created_time"));
      }
    }
    return null;
  }

  public static SocialEngagement getEngagementForArticle(ArticleOrBuilder article)
      throws SocialException {
    String url = article.getUrl();
    try {
      String encodedURL = encodeUrl(url);
      JsonObject urlObject = getCrawlerClient().fetchObject(encodedURL, JsonObject.class);

      // Get shares and comments
      if (!urlObject.has("share")) {
        return null;
      }
      else {
        JsonObject shareObject = urlObject.getJsonObject("share");
        int shareCount = shareObject.getInt("share_count");
        int commentCount = shareObject.getInt("comment_count");

        // Get likes
        String objectId = urlObject.getJsonObject("og_object").getString("id");
        JsonObject likesObject = getCrawlerClient().fetchObject(objectId, JsonObject.class,
            Parameter.with("fields", "likes.summary(true)"));
        int likeCount = likesObject.getJsonObject("likes")
            .getJsonObject("summary")
            .getInt("total_count");

        return SocialEngagement.newBuilder()
            .setSite(Site.FACEBOOK)
            .setLikeCount(likeCount)
            .setShareCount(shareCount)
            .setShareScore(ShareNormalizer.getInstance(Site.FACEBOOK).getShareScore(
                url,
                shareCount,
                System.currentTimeMillis() - Articles.getPublishedTime(article) /* ageInMillis */))
            .setCommentCount(commentCount)
            .setCreateTime(System.currentTimeMillis())
            .build();
      }
    } catch (ClassifierException e) {
      // FacebookShareNormalizer failed to instantiate from disk - this is an invalid state.
      throw new IllegalStateException(e);
    } catch (FacebookOAuthException e) {
      e.printStackTrace();
      throw new SocialException("Can't get FB engagement for url "
          + url + ": " + e.getMessage(), e);
    } catch (JsonException e) {
      e.printStackTrace();
      throw new SocialException("Can't parse Facebook JSON: " + e.getMessage(), e);
    }
  }

  private static synchronized FacebookClient getCrawlerClient() throws SocialException {
    if (__crawlerClient == null) {
      __crawlerClient =
          new DefaultFacebookClient(CRAWLER_APP_ID + "|" + CRAWLER_APP_SECRET,
              CRAWLER_APP_SECRET, Version.VERSION_2_2);
    }
    return __crawlerClient;
  }

  public static synchronized FacebookClient getFrontendClient() throws SocialException {
    if (__frontendClient == null) {
      __frontendClient =
          new DefaultFacebookClient(FRONTENT_APP_ID + "|" + FRONTENT_APP_SECRET,
              FRONTENT_APP_SECRET, Version.VERSION_2_2);
    }
    return __frontendClient;
  }

  public static void main(String args[]) throws Exception {
    System.out.println(getPublishTime(Url.newBuilder()
        .setUrl("http://firstround.com/review/Top-Hacks-from-a-PM-Behind-Two-of"
            + "-Techs-Hottest-Products/")
        .build()));
  }
}
