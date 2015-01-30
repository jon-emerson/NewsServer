package com.janknspank.bizness;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
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

  public static SocialEngagement getEngagementForURL(String url) throws BiznessException {
    SocialEngagement engagement = null;
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
        // There is no engagement if the share object is missing
        engagement = SocialEngagement.newBuilder()
            .setSite(Site.FACEBOOK)
            .setLikeCount(0)
            .setShareCount(0)
            .setCommentCount(0)
            .setCreateTime(System.currentTimeMillis())
            .build();
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

        engagement = SocialEngagement.newBuilder()
            .setSite(Site.FACEBOOK)
            .setLikeCount(likeCount)
            .setShareCount(shareCount)
            .setCommentCount(commentCount)
            .setCreateTime(System.currentTimeMillis())
            .build();
      }
    } catch (FacebookOAuthException e) {
      e.printStackTrace();
      throw new BiznessException("Can't get FB engagement for url "
          + url + ": " + e.getMessage(), e);
    } catch (JsonException e) {
      e.printStackTrace();
      throw new BiznessException("Can't parse Facebook JSON: " + e.getMessage(), e);
    }

    // Save the engagement object
    try {
      Database.insert(engagement);
    } catch (DatabaseSchemaException | DatabaseRequestException e) {
      throw new BiznessException("Error inserting facebook engagement:" + e.getMessage(), e);
    }

    return engagement;
  }

  private static FacebookClient getFacebookClient() throws BiznessException {
    if (__facebookClient == null) {
      Properties properties = getFacebookProperties();
      String appSecret = properties.getProperty("appSecret");
      String appId = properties.getProperty("appId");
      __facebookClient =
          new DefaultFacebookClient(appId + "|" + appSecret, appSecret, Version.VERSION_2_2);
    }
    return __facebookClient;
  }

  private static Properties getFacebookProperties() throws BiznessException {
    Properties properties = new Properties();
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream("facebook.properties");
      properties.load(inputStream);
      return properties;
    } catch (IOException e) {
      throw new BiznessException("Could not read facebook.properties: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }
}