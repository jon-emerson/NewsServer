package com.janknspank.twitter;

import java.net.MalformedURLException;
import java.net.URL;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.URLEntity;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.Urls;
import com.janknspank.crawler.UrlCleaner;
import com.janknspank.crawler.UrlWhitelist;
import com.janknspank.database.DatabaseSchemaException;

public class TwitterCrawler implements twitter4j.StatusListener {
  private final UrlResolver resolver = UrlResolver.getInstance();

  @SuppressWarnings("unused")
  private static void print(long id, String shortUrl, String longUrl) {
    System.out.println(id + ": " + shortUrl + " (" + longUrl + ")");
  }

  @Override
  public void onStatus(final Status status) {
    for (final URLEntity entity : status.getURLEntities()) {
      final URL shortUrl;
      try {
        shortUrl = new URL(entity.getExpandedURL());
      } catch (MalformedURLException e) {
        continue;
      }

      // Filter spam.
      if ("adinsight.jp".equals(shortUrl.getHost()) ||
          "du3a.org".equals(shortUrl.getHost()) ||
          "m12oney-addict2.ru".equals(shortUrl.getHost()) ||
          "n12ewsfirstworld30.ru".equals(shortUrl.getHost()) ||
          "n12ewsoneworld30.ru".equals(shortUrl.getHost()) ||
          "n1ewsfirstworld30.ru".equals(shortUrl.getHost()) ||
          "n1ewso5neworld.ru".equals(shortUrl.getHost()) ||
          "p-utin.cf".equals(shortUrl.getHost()) ||
          "p-utin.ga".equals(shortUrl.getHost()) ||
          "putin1410.cf".equals(shortUrl.getHost()) ||
          "putin1410.ga".equals(shortUrl.getHost()) ||
          "put-in1410.ga".equals(shortUrl.getHost()) |
          "qurani.tv".equals(shortUrl.getHost())) {
        continue;
      }

      Futures.addCallback(resolver.resolve(entity.getExpandedURL()),
          new FutureCallback<String>() {
            @Override
            public void onFailure(Throwable e) {
            }

            @Override
            public void onSuccess(String longUrl) {
              if (UrlWhitelist.isOkay(longUrl)) {
                longUrl = UrlCleaner.clean(longUrl);

                System.err.println("News URL found: " + longUrl);
                String twitterUrl = "https://twitter.com/"
                    + status.getUser().getScreenName() + "/status/" + status.getId();

                try {
                  Urls.put(longUrl, twitterUrl, /* isTweet */ true);
                } catch (DatabaseSchemaException | BiznessException e) {
                  e.printStackTrace();
                }
              }
            }
          });
    }
  }

  @Override
  public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
  }

  @Override
  public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
  }

  @Override
  public void onScrubGeo(long userId, long upToStatusId) {
  }

  @Override
  public void onStallWarning(StallWarning warning) {
  }

  @Override
  public void onException(Exception ex) {
    ex.printStackTrace();
  }

  public static void main(String[] args) throws TwitterException {
    final TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
    twitterStream.addListener(new TwitterCrawler());
    twitterStream.sample();
  }
}
