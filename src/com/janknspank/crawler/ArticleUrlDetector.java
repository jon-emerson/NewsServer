package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
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
  private static class PatternCache extends ThreadLocal<LinkedHashMap<String, Pattern>> {
    private static final int CACHE_SIZE_PER_THREAD = 100;

    @Override
    protected LinkedHashMap<String, Pattern> initialValue() {
      return new LinkedHashMap<String, Pattern>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Pattern> eldest) {
          return size() > CACHE_SIZE_PER_THREAD;
        }
      };
    }

    public Pattern getPattern(final String regex) {
      if (this.get().containsKey(regex)) {
        return this.get().get(regex);
      }
      Pattern pattern = Pattern.compile(regex);
      this.get().put(regex, pattern);
      return pattern;
    };
  }

  public static boolean isArticle(String urlString) {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      return false;
    }

    SiteManifest contentSite = SiteManifests.getForUrl(url);
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
