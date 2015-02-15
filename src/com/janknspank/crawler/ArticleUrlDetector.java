package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;

/**
 * Utility method for determining whether a URL is a news article, solely
 * by inspecting its URL.
 */
public class ArticleUrlDetector {
  public static final Predicate<String> PREDICATE = new Predicate<String>() {
    @Override
    public boolean apply(String url) {
      return ArticleUrlDetector.isArticle(url);
    }
  };

  public static boolean isArticle(String urlString) {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      return false;
    }

    String host = url.getHost().toLowerCase();
    String path = url.getPath();
    if (host.endsWith("abc.net.au")) {
      return Pattern.compile("\\/[0-9]{6,10}(.htm)?$").matcher(path).find();
    }
    if (host.endsWith("abcnews.go.com")) {
      return Pattern.compile("\\/story$").matcher(path).find()
          || Pattern.compile("^\\/blogs\\/.*\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\/$$").matcher(path).find()
          || Pattern.compile("^\\/[^\\/]+\\/wireStory\\/.*[0-9]{7,9}$").matcher(path).find();
    }
    if (host.equals("advice.careerbuilder.com")) {
      return Pattern.compile("^\\/posts\\/").matcher(path).find();
    }
    if (host.endsWith("aljazeera.com")) {
      return Pattern.compile("^\\/articles\\/20[0-9]{2}\\/1?[0-9]\\/[1-3]?[0-9]\\/.*\\.html$").matcher(path).find();
    }
    if (host.endsWith("allthingsd.com")) {
      return Pattern.compile("^\\/20[0-9]{2}[01][0-9][0-3][0-9]\\/").matcher(path).find();
    }
    if (host.endsWith("arstechnica.com")) {
      return Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\/$").matcher(path).find()
          || Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\.ars$").matcher(path).find()
          || Pattern.compile("^\\/news\\/.*[0-9]{5,12}\\.html$").matcher(path).find()
          || Pattern.compile("\\/20[0-9]{2}[01][0-9][0-3][0-9]\\-[0-9]{3,10}\\.html$").matcher(path).find();
    }
    if (host.endsWith("bbc.co.uk") || host.endsWith("bbc.com")) {
      return Pattern.compile("-[0-9]{5,10}$").matcher(path).find()
          || Pattern.compile("\\/story\\/20[0-9]{2}[01][0-9][0-3][0-9]\\-").matcher(path).find()
          || Pattern.compile("\\/newsbeat\\/[0-9]{7,10}$").matcher(path).find();
    }
    if (host.endsWith("bdnews24.com")) {
      return Pattern.compile("^\\/[^\\/]+\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/").matcher(path).find();
    }
    if (host.endsWith("bloomberg.com")) {
      return Pattern.compile("^\\/[a-z\\-\\_]+\\/20[0-9]{2}-[01][0-9]-[0-3][0-9](-|\\/).*(\\/|\\.html)$").matcher(path).find()
          || Pattern.compile("^\\/news\\/articles\\/(19|20)[0-9]{2}-[01][0-9]-[0-3][0-9]\\/[^\\/]+$").matcher(path).find()
          || Pattern.compile("^\\/bb\\/newsarchive\\/[a-zA-Z0-9_]{5,15}\\.html$").matcher(path).find();
    }
    if (host.endsWith("boston.com")) {
      if (host.equals("finance.boston.com")) {
        return Pattern.compile("\\/read\\/[0-9]{6,10}\\/[^\\/]+\\/$").matcher(path).find();
      }
      return Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/.*story\\.html$").matcher(path).find()
          || Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\.html$").matcher(path).find()
          || Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/\\.]+\\.html$").matcher(path).find();
    }
    if (host.endsWith("breitbart.com")) {
      return Pattern.compile("\\/[A-Za-z0-9\\-\\_]+\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/.*\\/$").matcher(path).find()
          || Pattern.compile("\\/news\\/[^\\/]+\\-[^\\/]+\\-[^\\/]+\\/$").matcher(path).find();
    }
    if (host.endsWith("buffalonews.com")) {
      return Pattern.compile("\\-20[0-9]{6}$").matcher(path).find()
          || Pattern.compile("^\\/article\\/20[0-9]{2}[01][0-9][0-3][0-9]\\/").matcher(path).find()
          || Pattern.compile("^\\/apps\\/pbcs.dll\\/article").matcher(path).find();
    }
    if (host.endsWith("businessinsider.com")) {
      return Pattern.compile("^\\/(?!category\\/).*\\-.*\\-").matcher(path).find();
    }
    if (host.endsWith("cbc.ca")) {
      return Pattern.compile("\\/news\\/.*\\-1\\.[0-9]{6,10}$").matcher(path).find()
          || Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/\\.]+\\.html$").matcher(path).find();
    }
    if (host.endsWith("cbsnews.com")) {
      return Pattern.compile("\\/news\\/(?!cbs-news-)[^\\/]+\\/$").matcher(path).find()
          || Pattern.compile("\\/8301\\-[0-9_\\-]+\\/[^\\/]+\\/$").matcher(path).find()
          || Pattern.compile("\\/[^\\/]+\\/[0-9a-f]+\\/([0-9]+\\/)?$").matcher(path).find();
    }
    if (host.endsWith("channelnewsasia.com")) {
      return Pattern.compile("^\\/news\\/.*\\/[0-9]+\\.html$").matcher(path).find()
          || Pattern.compile("^\\/premier\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/.*\\/$").matcher(path).find();
    }
    if (host.endsWith("chicagotribune.com")) {
      if (host.equals("articles.chicagotribune.com")) {
        return Pattern.compile("^\\/(19|20)[0-9]{2}-[01][0-9]-[0-3][0-9]\\/.*[0-9]{8}").matcher(path).find();
      }
      return Pattern.compile("20[0-9]{2}[01][0-9][0-3][0-9]\\-(column|htmlstory|story|storygallery)\\.html$").matcher(path).find()
          || Pattern.compile(",[0-9]{6,10}\\.(column|htmlstory|story|storygallery)$").matcher(path).find();
    }
    if (host.endsWith("chron.com")) {
      return Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\/$").matcher(path).find()
          || Pattern.compile("(\\-|\\/)[0-9]{7,10}\\.(php|html)$").matcher(path).find();
    }
    if (host.endsWith("cleveland.com")) {
      return Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/\\.]+\\.html$").matcher(path).find();
    }
    if (host.endsWith("cnbc.com")) {
      return Pattern.compile("^\\/id\\/[0-9]{9,11}(\\/[^\\/]*)?$").matcher(path).find();
    }
    if (host.endsWith("cnn.com")) {
      if (host.equals("travel.cnn.com")) {
        return Pattern.compile("\\-[0-9]{5,8}$").matcher(path).find();
      }
      return Pattern.compile("\\/20[0-9]{2}\\/([A-Z]+\\/([A-Za-z]+\\/)?)?[01][0-9]\\/[0-3][0-9]\\/").matcher(path).find()
          || Pattern.compile("\\/SPECIALS\\/.*\\/index.html?$").matcher(path).find();
    }
    if (host.endsWith("curbed.com")) {
      return Pattern.compile("\\/20[0-9]{2}\\/[01][0-9](\\/[0-3][0-9])?\\/[^\\/\\.]+\\.php$").matcher(path).find();
    }
    if (host.endsWith("engadget.com")) {
      return Pattern.compile("^\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/.*\\-").matcher(path).find();
    }
    if (host.endsWith("fastcompany.com")) {
      return Pattern.compile("\\/3[0-9]{6}\\/.*").matcher(path).find();
    }
    if (host.endsWith("forbes.com")) {
      return Pattern.compile("^\\/sites\\/[^\\/]+\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/").matcher(path).find();
    }
    if (host.endsWith("fortune.com")) {
      return Pattern.compile("^\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/(.*\\-.*|[0-9]+)\\/$").matcher(path).find();
    }
    if (host.endsWith("gizmodo.com")) {
      return Pattern.compile("[\\/-]1[0-9]{9}(\\/[^\\/]+)?$").matcher(path).find();
    }
    if (host.endsWith("latimes.com")) {
      return Pattern.compile("20[0-9]{2}[01][0-9][0-3][0-9]\\-(column|htmlstory|story|storygallery)\\.html$").matcher(path).find()
          || Pattern.compile(",[0-9]{6,10}\\.(column|htmlstory|story|storygallery)$").matcher(path).find();
    }
    if (host.equals("markets.cbsnews.com")) {
      return Pattern.compile("^\\/[^\\/]+\\/[0-9a-f]{12,19}\\/").matcher(path).find();
    }
    if (host.equals("mashable.com")) {
      return Pattern.compile("^\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/.*\\-.*\\/$").matcher(path).find();
    }
    if (host.endsWith("medium.com")) {
      return Pattern.compile("[\\/-][0-9a-f]{12}$").matcher(path).find();
    }
    if (host.endsWith("mercurynews.com")) {
      return Pattern.compile("\\/ci_[0-9]{7,10}(\\/.*)?$").matcher(path).find()
          || Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/[^\\/]+\\/$").matcher(path).find();
    }
    if (host.endsWith("nytimes.com")) {
      return Pattern.compile("^\\/(aponline\\/)?(19|20)[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/.*\\-.*\\-.*(\\/|\\.html)$").matcher(path).find();
    }
    if (host.endsWith("pcmag.com")) {
      return Pattern.compile("\\/article2\\/.+\\.asp$").matcher(path).find();
    }
    if (host.endsWith("recode.net")) {
      return Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/[^\\/]+\\/$").matcher(path).find();
    }
    if (host.endsWith("redherring.com")) {
      return Pattern.compile("\\/[a-z]+\\/[^\\/]+-[^\\/]+\\/$").matcher(path).find();
    }
    if (host.endsWith("sfgate.com")) {
      return Pattern.compile("\\/article.*(\\-|\\/)[0-9]{7,10}\\.php$").matcher(path).find()
          || Pattern.compile("\\/[0-9a-z\\-]+\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/[^\\/]+\\/$").matcher(path).find();
    }
    if (host.endsWith("siliconbeat.com")) {
      return Pattern.compile("^\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/.*\\-.*\\/$").matcher(path).find();
    }
    if (host.endsWith("slate.com")) {
      return Pattern.compile("^\\/(articles|blogs)\\/.*\\/20[0-9]{2}\\/[01][0-9]\\/.*\\.html$").matcher(path).find();
    }
    if (host.endsWith("startupworkout.com")) {
      return Pattern.compile("^\\/(?!author\\/).+\\-.+").matcher(path).find();
    }
    if (host.endsWith("techcrunch.com")) {
      return Pattern.compile("^\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/[^\\/]+\\/$").matcher(path).find();
    }
    if (host.endsWith("technologyreview.com")) {
      return Pattern.compile("\\/[a-z]+\\/[0-9]{5,7}\\/").matcher(path).find();
    }
    if (host.endsWith("telegraph.co.uk")) {
      return Pattern.compile("\\/1[0-9]{7}\\/").matcher(path).find();
    }
    if (host.endsWith("theguardian.com")) {
      return Pattern.compile("\\/20[0-9]{2}\\/(jan|feb|mar|apr|may|jun|jul|aug|sep|nov|dec)\\/[0-3][0-9]\\/").matcher(path).find();
    }
    if (host.endsWith("thenextweb.com")) {
      return Pattern.compile("^\\/([a-z\\_\\-]+\\/)?20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/[^\\/]+\\/$").matcher(path).find();
    }
    if (host.endsWith("theverge.com")) {
      return Pattern.compile("^\\/([a-z\\_\\-]+\\/)?20[0-9]{2}\\/1?[0-9]\\/[1-3]?[0-9]\\/[0-9]{7,9}\\/").matcher(path).find();
    }
    if (host.endsWith("venturebeat.com")) {
      return Pattern.compile("^\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/[^\\/]+\\/$").matcher(path).find();
    }
    if (host.endsWith("washingtonpost.com")) {
      return Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/[^\\/]+(\\/|_story\\.html)$").matcher(path).find()
          || Pattern.compile("\\-20[0-9]{2}[01][0-9][0-3][0-9]\\.html$").matcher(path).find()
          || Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/\\.]+\\.html$").matcher(path).find();
    }
    if (host.endsWith("wired.com")) {
      return Pattern.compile("^\\/20[0-9]{2}\\/[01][0-9]\\/.*\\/$").matcher(path).find();
    }

    return false;
  }
}
