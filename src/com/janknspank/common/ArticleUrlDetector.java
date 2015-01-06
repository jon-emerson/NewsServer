package com.janknspank.common;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * Utility method for determining whether a URL is a news article, solely
 * by inspecting its URL.
 */
public class ArticleUrlDetector {
  private static final Pattern ABC_NET_AU_PATH = Pattern.compile("\\/[0-9]{6,10}.htm$");
  private static final Pattern ABC_NEWS_ID_PARAM = Pattern.compile("^[0-9]{5,10}$");
  private static final Pattern ABC_NEWS_BLOG_PATH =
      Pattern.compile("^\\/blogs\\/.*\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\/$$");
  private static final Pattern ARS_TECHNICA_PATH_1 =
      Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\/$");
  private static final Pattern ARS_TECHNICA_PATH_2 =
      Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\.ars$");
  private static final Pattern ARS_TECHNICA_PATH_3 =
      Pattern.compile("^\\/(archive|news)\\/.*[0-9]{5,12}\\.html$");
  private static final Pattern ARS_TECHNICA_PATH_4 =
      Pattern.compile("\\/20[0-9]{2}[01][0-9][0-3][0-9]\\-[0-9]{3,10}\\.html$");
  private static final Pattern BBC_CO_UK_PATH_1 =
      Pattern.compile("\\/story\\/20[0-9]{2}[01][0-9][0-3][0-9]\\-");
  private static final Pattern BBC_CO_UK_PATH_2 =
      Pattern.compile("\\/newsbeat\\/[0-9]{7,10}$");
  private static final Pattern BLOOMBERG_ARCHIVE_PATH =
      Pattern.compile("^\\/bb\\/newsarchive\\/[a-zA-Z0-9_]{5,15}\\.html$");
  private static final Pattern BOSTON_COM_PATH_1 =
      Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/.*story\\.html$");
  private static final Pattern BOSTON_COM_PATH_2 =
      Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\.html$");
  private static final Pattern BOSTON_FINANCE_COM_PATH =
      Pattern.compile("\\/read\\/[0-9]{6,10}\\/[^\\/]+\\/$");
  private static final Pattern BUFFALO_NEWS_PATH =
      Pattern.compile("\\-20[0-9]{6}$");
  private static final Pattern CBC_PATH =
      Pattern.compile("\\/news\\/.*\\-1\\.[0-9]{6,10}$");
  private static final Pattern CBS_NEWS_PATH_1 =
      Pattern.compile("\\/news\\/[^\\/]+\\/$");
  private static final Pattern CBS_NEWS_PATH_2 =
      Pattern.compile("\\/8301\\-[0-9_\\-]+\\/[^\\/]+\\/$");
  private static final Pattern CBS_NEWS_PATH_3 =
      Pattern.compile("\\/[^\\/]+\\/[0-9a-f]+\\/([0-9]+\\/)?$");
  private static final Pattern CHANNEL_NEWS_ASIA_PATH =
      Pattern.compile("\\/news\\/.*\\/[0-9]+\\.html$");
  private static final Pattern CHRON_BLOG_PATH =
      Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/]+\\/$");
  private static final Pattern CHRON_PATH =
      Pattern.compile("(\\-|\\/)[0-9]{7,10}\\.(php|html)$");
  private static final Pattern CNBC_PATH =
      Pattern.compile("^\\/id\\/[0-9]{9,11}(\\/[^\\/]*)?$");
  private static final Pattern CNN_PATH_1 =
      Pattern.compile("\\/20[0-9]{2}\\/([A-Z]+\\/([A-Za-z]+\\/)?)?[01][0-9]\\/[0-3][0-9]\\/");
  private static final Pattern CNN_PATH_2 =
      Pattern.compile("\\/SPECIALS\\/.*\\/index.html?$");
  private static final Pattern CNN_TRAVEL_PATH =
      Pattern.compile("\\-[0-9]{5,8}$");
  private static final Pattern CURBED_PATH =
      Pattern.compile("\\/20[0-9]{2}\\/[01][0-9](\\/[0-3][0-9])?\\/[^\\/\\.]+\\.php$");
  private static final Pattern LATIMES_PATH_1 =
      Pattern.compile("20[0-9]{2}[01][0-9][0-3][0-9]\\-" +
          "(column|htmlstory|story|storygallery)\\.html$");
  private static final Pattern LATIMES_PATH_2 =
      Pattern.compile(",[0-9]{6,10}\\.(column|htmlstory|story|storygallery)$");
  private static final Pattern MARKETS_CBSNEWS_PATH =
      Pattern.compile("^\\/[^\\/]+\\/[0-9a-f]{12,19}\\/");
  private static final Pattern MERCURY_NEWS_PATH =
      Pattern.compile("\\/ci_[0-9]{7,10}(\\/.*)?$");
  private static final Pattern PATH_ENDS_WITH_DASH_NUMBER =
      Pattern.compile("-[0-9]{5,10}$");
  private static final Pattern SFGATE_PATH =
      Pattern.compile("\\/article.*(\\-|\\/)[0-9]{7,10}\\.php$");
  private static final Pattern WASHINGTON_POST_PATH_1 =
      Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[0-3][0-9]\\/[^\\/]+(\\/|_story\\.html)$");
  private static final Pattern WASHINGTON_POST_PATH_2 =
      Pattern.compile("\\-20[0-9]{2}[01][0-9][0-3][0-9]\\.html$");
  private static final Pattern YEAR_MONTH_THEN_ARTICLE_NAME_PATH =
      Pattern.compile("\\/20[0-9]{2}\\/[01][0-9]\\/[^\\/\\.]+\\.html$");
  private static Map<String, String> getParameters(String urlString) throws URISyntaxException {
    List<NameValuePair> parameters = URLEncodedUtils.parse(new URI(urlString), Charsets.UTF_8.name());
    Map<String, String> parameterMap = Maps.newHashMap();
    for (NameValuePair nameValuePair : parameters) {
      parameterMap.put(nameValuePair.getName(), nameValuePair.getValue());
    }
    return parameterMap;
  }

  public static boolean isArticle(String urlString) {
    URL url;
    Map<String, String> parameters;
    try {
      url = new URL(urlString);
      parameters = getParameters(urlString);
    } catch (MalformedURLException|URISyntaxException e) {
      return false;
    }

    String host = url.getHost().toLowerCase();
    String path = url.getPath();
    if (host.endsWith("abc.net.au")) {
      return ABC_NET_AU_PATH.matcher(path).find() ||
          DateParser.parseDateFromUrl(urlString, false) != null;
    }
    if (host.endsWith("abcnews.go.com")) {
      return ABC_NEWS_ID_PARAM.matcher(Strings.nullToEmpty(parameters.get("id"))).find() ||
          ABC_NEWS_BLOG_PATH.matcher(path).find() ||
          (path.contains("/wireStory/") && PATH_ENDS_WITH_DASH_NUMBER.matcher(path).find());
    }
    if (host.endsWith("aljazeera.com")) {
      return DateParser.parseDateFromUrl(urlString, false) != null;
    }
    if (host.endsWith("arstechnica.com")) {
      return ARS_TECHNICA_PATH_1.matcher(path).find() ||
          ARS_TECHNICA_PATH_2.matcher(path).find() ||
          ARS_TECHNICA_PATH_3.matcher(path).find() ||
          ARS_TECHNICA_PATH_4.matcher(path).find();
    }
    if (host.endsWith("bbc.co.uk") || host.endsWith("bbc.com")) {
      return PATH_ENDS_WITH_DASH_NUMBER.matcher(path).find() ||
          BBC_CO_UK_PATH_1.matcher(path).find() ||
          BBC_CO_UK_PATH_2.matcher(path).find();
    }
    if (host.endsWith("bloomberg.com")) {
      return BLOOMBERG_ARCHIVE_PATH.matcher(path).find() ||
          DateParser.parseDateFromUrl(urlString, false) != null;
    }
    if (host.endsWith("boston.com")) {
      if (host.equals("finance.boston.com")) {
        return BOSTON_FINANCE_COM_PATH.matcher(path).find();
      }
      return BOSTON_COM_PATH_1.matcher(path).find() ||
          BOSTON_COM_PATH_2.matcher(path).find() ||
          YEAR_MONTH_THEN_ARTICLE_NAME_PATH.matcher(path).find();
    }
    if (host.endsWith("buffalonews.com")) {
      return BUFFALO_NEWS_PATH.matcher(path).find();
    }
    if (host.endsWith("businessweek.com")) {
      return YEAR_MONTH_THEN_ARTICLE_NAME_PATH.matcher(path).find() ||
          DateParser.parseDateFromUrl(urlString, false) != null;
    }
    if (host.endsWith("cbc.ca")) {
      return CBC_PATH.matcher(path).find() ||
          YEAR_MONTH_THEN_ARTICLE_NAME_PATH.matcher(path).find();
    }
    if (host.endsWith("cbsnews.com")) {
      return CBS_NEWS_PATH_1.matcher(path).find() ||
          CBS_NEWS_PATH_2.matcher(path).find() ||
          CBS_NEWS_PATH_3.matcher(path).find() ||
          DateParser.parseDateFromUrl(urlString, false) != null;
    }
    if (host.endsWith("channelnewsasia.com")) {
      return CHANNEL_NEWS_ASIA_PATH.matcher(path).find() ||
          path.startsWith("/premier/") && DateParser.parseDateFromUrl(urlString, false) != null;
    }
    if (host.endsWith("chron.com")) {
      if (host.equals("blog.chron.com") && CHRON_BLOG_PATH.matcher(path).find()) {
        return true;
      }
      return CHRON_PATH.matcher(path).find();
    }
    if (host.endsWith("cleveland.com")) {
      return YEAR_MONTH_THEN_ARTICLE_NAME_PATH.matcher(path).find();
    }
    if (host.endsWith("cnbc.com")) {
      return CNBC_PATH.matcher(path).find();
    }
    if (host.endsWith("cnn.com")) {
      if (host.equals("travel.cnn.com")) {
        return CNN_TRAVEL_PATH.matcher(path).find();
      }
      return CNN_PATH_1.matcher(path).find() ||
          CNN_PATH_2.matcher(path).find();
    }
    if (host.endsWith("curbed.com")) {
      return CURBED_PATH.matcher(path).find();
    }
    if (host.endsWith("latimes.com")) {
      return LATIMES_PATH_1.matcher(path).find() ||
          LATIMES_PATH_2.matcher(path).find();
    }
    if (host.equals("markets.cbsnews.com")) {
      return MARKETS_CBSNEWS_PATH.matcher(path).find();
    }
    if (host.endsWith("mercurynews.com")) {
      return MERCURY_NEWS_PATH.matcher(path).find();
    }
    if (host.endsWith("sfgate.com")) {
      return SFGATE_PATH.matcher(path).find() ||
          DateParser.parseDateFromUrl(urlString, false) != null;
    }
    if (host.endsWith("washingtonpost.com")) {
      return WASHINGTON_POST_PATH_1.matcher(path).find() ||
          WASHINGTON_POST_PATH_2.matcher(path).find() ||
          YEAR_MONTH_THEN_ARTICLE_NAME_PATH.matcher(path).find();
    }

    return DateParser.parseDateFromUrl(urlString, false) != null;
  }
}
