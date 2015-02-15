package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.janknspank.proto.CrawlProto.ContentSite;
import com.janknspank.proto.CrawlProto.ContentSite.ArticleUrlPattern;

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
  private static final LoadingCache<String, Pattern> PATTERN_CACHE = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .build(
          new CacheLoader<String, Pattern>() {
            public Pattern load(String regex) {
              return Pattern.compile(regex);
            }
          });

  public static boolean isArticle(String urlString) {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      return false;
    }

    ContentSite contentSite = UrlWhitelist.getContentSiteForUrl(url);
    if (contentSite == null) {
      return false;
    }
    String domain = url.getHost().toLowerCase();
    String path = url.getPath();
    boolean matched = false;
    while (!matched && domain.contains(".")) {
      for (ArticleUrlPattern articleUrlPattern : contentSite.getArticleUrlPatternList()) {
        if (articleUrlPattern.hasSubdomain() && domain.equals(articleUrlPattern.getSubdomain())
            || domain.equals(contentSite.getRootDomain())
            || Iterables.contains(contentSite.getAkaRootDomainList(), domain)) {
          matched = true;
          try {
            if (PATTERN_CACHE.get(articleUrlPattern.getPathRegex()).matcher(path).find()) {
              return true;
            }
          } catch (ExecutionException e) {
            Throwables.propagateIfPossible(e);
            throw new Error("Bad pattern for domain: " + domain + "(\""
                + articleUrlPattern.getPathRegex() + "\"), " + e.getMessage(), e);
          }
        }
      }
      domain = domain.substring(domain.indexOf(".") + 1);
    }
    return false;
  }
}
