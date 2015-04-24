package com.janknspank.crawler.social;

import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protobuf.Message;
import com.janknspank.bizness.Articles;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;

public class TwitterData {
  private static final Fetcher FETCHER = new Fetcher();

  /**
   * Returns the Twitter JSON API endpoint for retrieving Twitter social
   * engagement for the passed {@code contentUrl}.
   */
  private static String getTwitterEngagementUrl(String contentUrl) throws SocialException {
    try {
      return new URIBuilder("http://cdn.api.twitter.com/1/urls/count.json")
          .addParameter("url", contentUrl)
          .build()
          .toString();
    } catch (URISyntaxException e) {
      throw new SocialException("Error constructing URL: " + e.getMessage(), e);
    }
  }

  public static SocialEngagement getEngagementForArticle(ArticleOrBuilder article)
      throws SocialException {
    try {
      String url = article.getUrl();
      String responseBody = FETCHER.getResponseBody(getTwitterEngagementUrl(url));
      JSONObject responseObject = new JSONObject(responseBody);
      if (!responseObject.has("count")) {
        return null;
      }
      int shareCount = responseObject.getInt("count");
      return SocialEngagement.newBuilder()
          .setSite(Site.TWITTER)
          .setShareCount(shareCount)
          .setShareScore(ShareNormalizer.getInstance(Site.TWITTER).getShareScore(
              url,
              shareCount,
              System.currentTimeMillis() - Articles.getPublishedTime(article) /* ageInMillis */))
          .setCreateTime(System.currentTimeMillis())
          .build();
    } catch (FetchException | ClassifierException e) {
      throw new SocialException("Error reading share count from Twitter: " + e.getMessage(), e);
    }
  }

  private static int count = 0;
  private static synchronized int getCount() {
    return ++count;
  }

  private static class Updater implements Callable<Void> {
    private final Article article;

    public Updater(Article article) {
      this.article = article;
    }

    @Override
    public Void call() throws Exception {
      SocialEngagement socialEngagement = getEngagementForArticle(article);
      if (socialEngagement != null) {
        Database.push(article, "social_engagement", ImmutableList.of((Message) socialEngagement));
      }
      int count = getCount();
      if (count % 500 == 0) {
        System.out.println(count);
      }
      return null;
    }
  }

  public static void main(String args[]) throws DatabaseSchemaException {
    Iterable<Article> articles = Database.with(Article.class).get();
    System.out.println(Iterables.size(articles) + " articles retrieved");

    ExecutorService executor = Executors.newFixedThreadPool(50);
    for (Article article : articles) {
      // if (SocialEngagements.getForArticle(article, Site.TWITTER) == null) {
        executor.submit(new Updater(article));
      // }
    }
    executor.shutdown();
    try {
      executor.awaitTermination(TimeUnit.MINUTES.toMillis(120), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {}
  }
}
