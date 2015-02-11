package com.janknspank.interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.janknspank.classifier.ClassifierException;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.json.JsonException;
import com.restfb.json.JsonObject;

public class FacebookData {
  private static FacebookClient __facebookClient = null;

  public static SocialEngagement getEngagementForURL(ArticleOrBuilder article) throws FacebookException {
    String url = article.getUrl();
    try {
      // Example urlObject: http://goo.gl/JVf3tt
      String encodedURL;
      try {
        encodedURL = URLEncoder.encode(url, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        System.out.println("Can't encode url: " + url);
        encodedURL = url;
      }
      JsonObject urlObject = getFacebookClient().fetchObject(encodedURL, JsonObject.class);

      // Get shares and comments
      if (!urlObject.has("share")) {
        return null;
      }
      else {
        JsonObject shareObject = urlObject.getJsonObject("share");
        int shareCount = shareObject.getInt("share_count");
        int commentCount = shareObject.getInt("comment_count");

        // Get likes
        String objectId = urlObject.getJsonObject("og_object")
            .getString("id");
        JsonObject likesObject = getFacebookClient().fetchObject(objectId,
            JsonObject.class,
            Parameter.with("fields", "likes.summary(true)"));
        int likeCount = likesObject.getJsonObject("likes")
            .getJsonObject("summary")
            .getInt("total_count");

        return SocialEngagement.newBuilder()
            .setSite(Site.FACEBOOK)
            .setLikeCount(likeCount)
            .setShareCount(shareCount)
            .setShareScore(FacebookShareNormalizer.getInstance().getShareScore(
                url,
                shareCount,
                Math.max(0, System.currentTimeMillis() - article.getPublishedTime())
                    /* ageInMillis */))
            .setCommentCount(commentCount)
            .setCreateTime(System.currentTimeMillis())
            .build();
      }
    } catch (ClassifierException e) {
      // FacebookShareNormalizer failed to instantiate from disk - this is an invalid state.
      throw new IllegalStateException(e);
    } catch (FacebookOAuthException e) {
      e.printStackTrace();
      throw new FacebookException("Can't get FB engagement for url "
          + url + ": " + e.getMessage(), e);
    } catch (JsonException e) {
      e.printStackTrace();
      throw new FacebookException("Can't parse Facebook JSON: " + e.getMessage(), e);
    }
  }

  private static FacebookClient getFacebookClient() throws FacebookException {
    if (__facebookClient == null) {
      Properties properties = getFacebookProperties();
      String appSecret = properties.getProperty("appSecret");
      String appId = properties.getProperty("appId");
      __facebookClient =
          new DefaultFacebookClient(appId + "|" + appSecret, appSecret, Version.VERSION_2_2);
    }
    return __facebookClient;
  }

  private static Properties getFacebookProperties() throws FacebookException {
    Properties properties = new Properties();
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream("facebook.properties");
      properties.load(inputStream);
      return properties;
    } catch (IOException e) {
      throw new FacebookException("Could not read facebook.properties: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }
}
