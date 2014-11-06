package com.janknspank;

import java.net.MalformedURLException;
import java.net.URL;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.URLEntity;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class StatusListener implements twitter4j.StatusListener {
  private final URLResolver resolver = URLResolver.getInstance();
  private final Multiset<String> domainSet = ConcurrentHashMultiset.create();
  private final Multiset<String> shortenerServiceSet = ConcurrentHashMultiset.create();
  private final Multiset<String> urlSet = ConcurrentHashMultiset.create();
  private int count = 0;

  @SuppressWarnings("unused")
  private static void print(long id, String shortUrl, String longUrl) {
    System.out.println(id + ": " + shortUrl + " (" + longUrl + ")");
  }

  private static void printTop(Multiset<String> multiset) {
    TopList list = new TopList(10);
    for (Multiset.Entry<String> s : multiset.entrySet()) {
      list.add(s.getElement(), s.getCount());
    }
    for (String s : list.getKeys()) {
      System.out.println("  " + s + ": " + list.getValue(s));
    }
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
          "m12oney-addict2.ru".equals(shortUrl.getHost()) ||
          "n12ewsfirstworld30.ru".equals(shortUrl.getHost()) ||
          "n12ewsoneworld30.ru".equals(shortUrl.getHost()) ||
          "n1ewsfirstworld30.ru".equals(shortUrl.getHost()) ||
          "n1ewso5neworld.ru".equals(shortUrl.getHost()) ||
          "p-utin.cf".equals(shortUrl.getHost()) ||
          "p-utin.ga".equals(shortUrl.getHost()) ||
          "putin1410.cf".equals(shortUrl.getHost()) ||
          "putin1410.ga".equals(shortUrl.getHost()) ||
          "put-in1410.ga".equals(shortUrl.getHost())) {
        continue;
      }

      Futures.addCallback(resolver.resolve(entity.getExpandedURL()),
          new FutureCallback<String>() {
            @Override
            public void onFailure(Throwable e) {
              // e.printStackTrace();
              // print(status.getId(), entity.getExpandedURL(), "lookup failed");
            }

            @Override
            public void onSuccess(String longUrl) {
              //print(status.getId(), entity.getExpandedURL(), longUrl);

              try {
                URL url = new URL(longUrl);
                domainSet.add(url.getHost());
                if (!url.getHost().equals(shortUrl.getHost())) {
                  shortenerServiceSet.add(shortUrl.getHost());
                }
                urlSet.add(longUrl);
              } catch (MalformedURLException e) {
                e.printStackTrace();
              }

              ++count;
              if (count % 100 == 50) {
                System.out.println("Top domains: ");
                printTop(domainSet);
                System.out.println("Top shortening services: ");
                printTop(shortenerServiceSet);
                System.out.println("Top URLs: ");
                printTop(urlSet);
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
}