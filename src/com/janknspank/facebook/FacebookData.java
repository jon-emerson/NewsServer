package com.janknspank.facebook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.janknspank.data.DataInternalException;
import com.janknspank.proto.Core.ArticleFacebookEngagement;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.json.JsonException;
import com.restfb.json.JsonObject;

public class FacebookData {
  private static FacebookClient facebookClient;
  
  static {
    generateFacebookClient();
  }
  
  public static ArticleFacebookEngagement getEngagementForURL(String url) 
      throws DataInternalException {
    try {
      // Example urlObject: http://goo.gl/JVf3tt
      JsonObject urlObject = facebookClient.fetchObject(url, JsonObject.class);
      
      // Get shares and comments
      JsonObject shareObject = urlObject.getJsonObject("share");
      int shareCount = shareObject.getInt("share_count");
      int commentCount = shareObject.getInt("comment_count");
      
      // Get likes
      String objectId = urlObject.getJsonObject("og_object")
          .getString("id");
      JsonObject likesObject = facebookClient.fetchObject(objectId, 
          JsonObject.class,
          Parameter.with("fields", "likes.summary(true)"));
      int likeCount = likesObject.getJsonObject("likes")
          .getJsonObject("summary")
          .getInt("total_count");
      
      ArticleFacebookEngagement engagement = ArticleFacebookEngagement.newBuilder()
      .setUrl(url)
      .setLikeCount(likeCount)
      .setShareCount(shareCount)
      .setCommentCount(commentCount)
      .setCreateTime(System.currentTimeMillis())
      .build();
      
      return engagement;
    } catch (FacebookOAuthException e) {
      // Handle case where the accessToken expires during a long running
      // request (like a neural network train, or vector space model initializer)
      System.out.println("Handle FacebookOAuthException for url: " + url);
      if (e.getErrorCode() == 104) {
        System.out.println("Don't have permissions for the url: " + url);
        System.out.println("Skipping getting the FB shares for it...");
        return null;
      }
      else {
        e.printStackTrace();
        return null;
        //generateFacebookClient();
      }
    } catch (JsonException e) {
      System.out.println("JsonException error getting engagement for url: " + url);
      e.printStackTrace();
      // Assuming there is no enggagment if the share object is missing
      ArticleFacebookEngagement engagement = ArticleFacebookEngagement.newBuilder()
          .setUrl(url)
          .setLikeCount(0)
          .setShareCount(0)
          .setCommentCount(0)
          .setCreateTime(System.currentTimeMillis())
          .build();
      return engagement;
    }
  }
  
  private static void generateFacebookClient() {
    Properties properties;
    try {
      properties = getFacebookProperties();
      String appSecret = properties.getProperty("appSecret");
      String appId = properties.getProperty("appId");
//      AccessToken accessToken =
//          new DefaultFacebookClient().obtainAppAccessToken(appId, appSecret);
//      System.out.println("accessToken: " + accessToken.getAccessToken());
      facebookClient = new DefaultFacebookClient(appId + "|" + appSecret, appSecret, Version.VERSION_2_2);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private static Properties getFacebookProperties() throws IOException {
    Properties properties = new Properties();
    String propFileName = "facebook.properties";
     
    InputStream inputStream = new FileInputStream(propFileName);
     
    if (inputStream != null) {
      properties.load(inputStream);
    } else {
      throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
    }
    
    return properties;
  }
}
