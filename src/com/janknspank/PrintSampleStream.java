package com.janknspank;

import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

public final class PrintSampleStream {
  public static void main(String[] args) throws TwitterException {
    final TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
    twitterStream.addListener(new StatusListener());
    twitterStream.sample();
  }
}
