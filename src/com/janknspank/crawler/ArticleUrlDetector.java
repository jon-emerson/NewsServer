package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.janknspank.common.PatternCache;
import com.janknspank.proto.CrawlerProto.SiteManifest;
import com.janknspank.proto.CrawlerProto.SiteManifest.ArticleUrlPattern;

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

  private static final PatternCache PATTERN_CACHE = new PatternCache();

  public static boolean isArticle(String urlString) {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      return false;
    }

    SiteManifest site = SiteManifests.getForUrl(url);
    if (site == null) {
      return false;
    }
    String domain = url.getHost().toLowerCase();
    String path = url.getPath();
    boolean matched = false;
    while (!matched && domain.contains(".")) {
      for (ArticleUrlPattern articleUrlPattern : site.getArticleUrlPatternList()) {
        if (articleUrlPattern.hasSubdomain() && domain.equals(articleUrlPattern.getSubdomain())
            || domain.equals(site.getRootDomain())
            || Iterables.contains(site.getAkaRootDomainList(), domain)) {
          matched = true;
          if (!articleUrlPattern.hasPathRegex() && !articleUrlPattern.hasQueryRegex()) {
            System.out.println("Warning: article_url_pattern has no path_regex or query_regex");
            return false;
          }
          boolean pathOkay = !articleUrlPattern.hasPathRegex()
              || PATTERN_CACHE.getPattern(articleUrlPattern.getPathRegex()).matcher(path).find();
          boolean queryOkay = !articleUrlPattern.hasQueryRegex()
              || PATTERN_CACHE.getPattern(articleUrlPattern.getQueryRegex())
                  .matcher(Strings.nullToEmpty(url.getQuery())).find();
          if (pathOkay && queryOkay) {
            return true;
          }
        }
      }
      domain = domain.substring(domain.indexOf(".") + 1);
    }
    return false;
  }
}
