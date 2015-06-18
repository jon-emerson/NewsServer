package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.BiznessException;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.Logger;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.ArticleProto.Article;

public class UrlFinder {
  private static final Logger LOG = new Logger(UrlFinder.class);
  private static final Fetcher FETCHER = new Fetcher();

  /**
   * Simple synchronized cache for <base> tag lookups on documents.
   */
  private static final Map<Document, String> BASE_URL_MAP = Collections.synchronizedMap(
      new LinkedHashMap<Document, String>() {
        protected boolean removeEldestEntry(Map.Entry<Document, String> eldest) {
          return this.size() > 50;
        }
      });

  /**
   * Retrieves the passed URL by making a request to the respective website,
   * and then interprets the returned results.
   */
  public static Set<String> findUrls(String url) throws FetchException, RequiredFieldException {

    if (url.startsWith("http://readwrite.com/")) {
      return findReadWriteUrls();
    }

    FetchResponse response = FETCHER.get(url);
    return findUrls(response.getDocument());
  }

  /**
   * Returns all the URLs from the passed document.  Note: There is no filtering
   * done!!  It's ALL THE URLs!!  We do not want to crawl them all!
   */
  public static Set<String> findUrls(Document document) {
    Elements linkEls = document.select("html > body a[href]");
    if (linkEls.isEmpty()) {
      // Some sites (like archrecord.construction.com) don't have <html> outer
      // tags.  It's illegal, but it don't matter, we still gotta crawl 'em.
      Element bodyEl = document.select("body").first();
      linkEls = bodyEl.select("a[href]");
    }

    Set<String> urlSet = Sets.newHashSet();
    for (Node linkEl : linkEls) {
      String href = linkEl.attr("href");
      String hrefToLowerCase = href.toLowerCase();
      if (!hrefToLowerCase.startsWith("javascript:") &&
          !hrefToLowerCase.startsWith("mailto:") &&
          !hrefToLowerCase.startsWith("whatsapp:")) {
        try {
          String resolvedUrl = resolveUrl(document, href);
          // To save on space, only save links to articles.  Without this,
          // 80% of our data is links to general category pages and the like.
          if (ArticleUrlDetector.isArticle(resolvedUrl)) {
            urlSet.add(resolvedUrl);
          }
        } catch (MalformedURLException e) {
          LOG.info("Bad relative URL: " + linkEl.attr("href"));
        }
      }
    }
    return ImmutableSet.copyOf(Iterables.transform(urlSet, UrlCleaner.TRANSFORM_FUNCTION));
  }

  /**
   * Resolves a relative URL to its fully-qualified version based on either the
   * <base> tag in the article, or the article URL itself.
   */
  private static String resolveUrl(Document document, String relativeUrl)
      throws MalformedURLException {
    String baseUrl = BASE_URL_MAP.get(document);
    if (baseUrl == null) {
      Node baseEl = document.select("base").first();
      baseUrl = (baseEl == null) ? document.baseUri() : baseEl.attr("href");
      BASE_URL_MAP.put(document, baseUrl);
    }
    return new URL(new URL(baseUrl), relativeUrl).toString();
  }

  private static Set<String> findReadWriteUrls() {
    Set<String> urls = Sets.newHashSet();

    Set<String> queries = ImmutableSet.of(
        "articles/@published");
        // Don't need to crawl everything now that we're up to date...
        // (FYI max-results= below can go up to 50.)
        // "items/@published?q=launch",
        // "articles/@published/@by-section/cloud",
        // "articles/@published/@by-section/hack",
        // "articles/@published/@by-section/mobile",
        // "articles/@published/@by-section/social",
        // "articles/@published/@by-section/start",
        // "articles/@published/@by-section/wear",
        // "articles/@published/@by-section/web",
        // "articles/@published/@by-section/work");
    for (String query : queries) {
      try {
        String response = FETCHER.getResponseBody(
            "http://api.readwrite.com/:apiproxy-anon/content-sites/cs019099924683860e/"
            + query + (query.contains("?") ? "&" : "?") + "max-results=24");
        JSONObject responseObj = new JSONObject(response);
        JSONArray entries = responseObj.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
          JSONObject entry = entries.getJSONObject(i);
          String date = entry.getString("publicationTimestamp").substring(0, 10);
          String slug = entry.getString("slug");
          urls.add("http://readwrite.com/"
              + date.replaceAll("-", "/")
              + "/"
              + slug);
        }
      } catch (FetchException e) {
        e.printStackTrace();
      }
    }
    return urls;
  }

  public static void main(String args[])
      throws FetchException, RequiredFieldException, BiznessException {
    for (String url : findReadWriteUrls()) {
      Article article = Iterables.getFirst(
          ArticleCrawler.getArticles(ImmutableList.of(url), true /* retain */).values(), null);
      if (ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_LAUNCHES) < 0.1) {
        System.out.println(url);
      }
    }
  }
}
