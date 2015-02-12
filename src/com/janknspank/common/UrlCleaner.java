package com.janknspank.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Utility class for cleaning up news site URLs, so they can be reduced to their
 * canonical forms.
 */
public class UrlCleaner {
  public static final Function<String, String> TRANSFORM_FUNCTION =
      new Function<String, String>() {
        @Override
        public String apply(String dirtyUrl) {
          return clean(dirtyUrl);
        }
      };

  /**
   * Cleans the passed query parameter map, allowing only strings from the
   * passed allowed keys.  The Map is modified in place.
   */
  private static void allowOnlyQueryParameters(
      TreeMap<String, String> queryParameters, String... allowedKeys) {
    Set<String> allowedKeySet = new HashSet<String>();
    allowedKeySet.addAll(Arrays.asList(allowedKeys));
    for (String key : Lists.newArrayList(queryParameters.keySet())) {
      if (!allowedKeySet.contains(key)) {
        queryParameters.remove(key);
      }
    }
  }

  public static String clean(String dirtyUrl) {
    URL url;
    try {
      url = new URL(dirtyUrl);
    } catch (MalformedURLException e) {
      // TODO(jonemerson): Handle this more elegantly.  Perhaps the URL
      // whitelist and the URL cleaner should be combined?
      e.printStackTrace();
      return dirtyUrl;
    }
    TreeMap<String, String> queryParameters = new TreeMap<>(new Comparator<String>() {
      @Override
      public int compare(String one, String two) {
        return one.compareTo(two);
      }
    });
    for (NameValuePair nameValue : URLEncodedUtils.parse(url.getQuery(), Charsets.UTF_8)) {
      queryParameters.put(nameValue.getName(), nameValue.getValue());
    }

    // These are pretty prevalent on all web sites.
    queryParameters.remove("utm_medium");
    queryParameters.remove("utm_campaign");
    queryParameters.remove("utm_channel");
    queryParameters.remove("utm_cid");
    queryParameters.remove("utm_content");
    queryParameters.remove("utm_hp_ref");
    queryParameters.remove("utm_term");
    queryParameters.remove("utm_source");

    // Do website-specific query parameter filtering and URL canonicalization.
    String host = url.getHost();
    String path = url.getPath();
    if (host.endsWith(".abc.net.au") || host.equals("abc.net.au")) {
      queryParameters.remove("cid");
      queryParameters.remove("pfm");
      queryParameters.remove("ref");
      queryParameters.remove("section");
      queryParameters.remove("site");
    }
    if (host.endsWith(".abcnews.go.com") || host.equals("abcnews.go.com")) {
      queryParameters.remove("page");
      queryParameters.remove("singlePage");
    }
    if (host.endsWith(".arstechnica.com") || host.equals("arstechnica.com")) {
      queryParameters.remove("amp");
      queryParameters.remove("comments");
      queryParameters.remove("post");
      queryParameters.remove("theme");
      queryParameters.remove("view");
    }
    if (host.endsWith(".bbc.co.uk") || host.equals("bbc.co.uk") ||
        host.endsWith(".bbc.com") || host.equals("bbc.com")) {
      queryParameters.remove("filter");
      queryParameters.remove("ns_campaign");
      queryParameters.remove("ns_linkname");
      queryParameters.remove("ns_mchannel");
      queryParameters.remove("ns_source");
      queryParameters.remove("postId");
      queryParameters.remove("sortBy");
      queryParameters.remove("sortOrder");
    }
    if (host.endsWith(".bloomberg.com") || host.equals("bloomberg.com")) {
      queryParameters.remove("bgref");
      queryParameters.remove("cmpid");
      queryParameters.remove("hootPostID");
      queryParameters.remove("terminal");
    }
    if (host.endsWith(".boston.com") || host.equals("boston.com")) {
      queryParameters.remove("Technology_subheadline_hp");
      queryParameters.remove("comments");
      queryParameters.remove("mastheadLogo");
      queryParameters.remove("p1");
      queryParameters.remove("page");
      queryParameters.remove("pg");
      queryParameters.remove("rss_id");
    }
    if (host.endsWith(".buffalonews.com") || host.equals("buffalonews.com")) {
      queryParameters.remove("ref");
    }
    if (host.endsWith(".businessinsider.com") || host.equals("businessinsider.com")) {
      if (path.contains("/")) {
        path = path.substring(0, path.indexOf("/"));
      }
      queryParameters.clear();
    }
    if (host.endsWith(".businessweek.com") || host.equals("businessweek.com")) {
      queryParameters.remove("hootPostID");
    }
    if (host.endsWith(".cbc.ca") || host.equals("cbc.ca")) {
      queryParameters.remove("cmp");
    }
    if (host.endsWith(".cbc.ca") || host.equals("cbc.ca")) {
      queryParameters.remove("cmp");
    }
    if (host.endsWith(".channelnewsasia.com") ||
        host.equals("channelnewsasia.com")) {
      queryParameters.remove("cid");
    }
    if (host.endsWith(".chicagotribune.com") || host.equals("chicagotribune.com")) {
      queryParameters.remove("cid");
    }
    if (host.endsWith(".cnbc.com") || host.equals("cnbc.com")) {
      queryParameters.remove("trknav");
      queryParameters.remove("__source");
    }
    if (host.endsWith(".cnn.com") || host.equals("cnn.com")) {
      queryParameters.remove("Page");
      queryParameters.remove("cnn");
      queryParameters.remove("eref");
      queryParameters.remove("hpt");
      queryParameters.remove("iid");
      queryParameters.remove("imw");
      queryParameters.remove("iref");
      queryParameters.remove("nbd");
      queryParameters.remove("npt");
      queryParameters.remove("sr");
      queryParameters.remove("source");
      queryParameters.remove("switchEdition");
      queryParameters.remove("xid");
      queryParameters.remove("_s");
      if (path.endsWith("/index.html")) {
        path = path.substring(0, path.length() - "index.html".length());
      }
    }
    if (host.endsWith(".economist.com") || host.equals("economist.com")) {
      queryParameters.remove("fsrc");
    }
    if (host.endsWith(".forbes.com") || host.equals("forbes.com")) {
      queryParameters.remove("commentId");
      queryParameters.remove("feed");
      queryParameters.remove("linkId");
    }
    if (host.endsWith(".foxnews.com") || host.equals("foxnews.com")) {
      queryParameters.remove("cmpid");
      queryParameters.remove("intcmp");
    }
    if (host.endsWith(".guardian.co.uk") || host.equals("guardian.co.uk")) {
      queryParameters.remove("intcmp");
      queryParameters.remove("INTCMP");
    }
    if (host.endsWith(".huffingtonpost.com") || host.equals("huffingtonpost.com")) {
      // "ir" might be bad to remove.  It controls which header people see on the top of the page.
      // But I'm removing it because it's better to consolidate the same articles.
      queryParameters.remove("ir"); 

      queryParameters.remove("m");
      queryParameters.remove("ncid");
    }
    if (host.endsWith(".latimes.com") || host.equals("latimes.com")) {
      queryParameters.remove("akst_action");
      queryParameters.remove("dlvrit");
      queryParameters.remove("replytocom");
      queryParameters.remove("track");
    }
    if (host.endsWith(".mashable.com") || host.equals("mashable.com")) {
      queryParameters.remove("geo");
    }
    if (host.endsWith(".mercurynews.com") || host.equals("mercurynews.com")) {
      queryParameters.remove("source");
    }
    if (host.endsWith(".msnbc.com") || host.equals("msnbc.com")) {
      queryParameters.remove("CID");
    }
    if (host.endsWith(".nationalgeographic.com") ||
        host.equals("nationalgeographic.com")) {
      queryParameters.remove("now");
    }
    if (host.endsWith(".news.yahoo.com") || host.equals("news.yahoo.com")) {
      if (queryParameters.containsKey(".pg") && queryParameters.get(".pg").equals("1")) {
        queryParameters.remove(".pg");
      }
      queryParameters.remove(".b");
      queryParameters.remove(".h");
      queryParameters.remove(".intl");
      queryParameters.remove(".lang");
      queryParameters.remove(".nx");
      queryParameters.remove(".to");
      queryParameters.remove(".show_comments");
      queryParameters.remove("_esi");
      queryParameters.remove("_intl");
      queryParameters.remove("_lang");
      queryParameters.remove("_lf");
      queryParameters.remove("_orig");
      queryParameters.remove("soc_src");
      queryParameters.remove("soc_trk");
    }
    if (host.endsWith(".nytimes.com") || host.equals("nytimes.com")) {
      // Seriously, they're all worthless.  And I found 30+ of them.
      queryParameters.clear();
    }
    if (host.endsWith(".reuters.com") || host.equals("reuters.com")) {
      queryParameters.remove("feedName");
      queryParameters.remove("feedType");
      queryParameters.remove("rpc");
    }
    if (host.endsWith(".sfgate.com") || host.equals("sfgate.com")) {
      queryParameters.remove("cmpid");
    }
    if (host.endsWith(".slate.com") || host.equals("slate.com")) {
      // Sometimes Slate paginates its articles - Make sure we always get a
      // single page.
      if (path.endsWith(".html") && !path.endsWith(".single.html")) {
        path = path.substring(0, path.length() - ".html".length()) + ".single.html";
      }
    }
    if (host.endsWith(".siliconbeat.com") || host.equals("siliconbeat.com")) {
      queryParameters.remove("msg");
      queryParameters.remove("shared");
    }
    if (host.endsWith(".techcrunch.com") || host.equals("techcrunch.com")) {
      queryParameters.remove("ncid");
    }
    if (host.endsWith(".theguardian.com") || host.equals("theguardian.com")) {
      queryParameters.remove("CMP");
      queryParameters.remove("TrackID");
    }
    if (host.endsWith(".thehindu.com") || host.equals("thehindu.com")) {
      queryParameters.remove("homepage");
    }
    if (host.endsWith(".theverge.com") || host.equals("theverge.com")) {
      if (path.matches(".*\\/in\\/[0-9]{6,9}")) {
        path = path.substring(0, path.lastIndexOf("/in/"));
      }
    }
    if (host.endsWith(".washingtonpost.com") ||
        host.equals("washingtonpost.com")) {
      // All worthless except 'p'.
      allowOnlyQueryParameters(queryParameters, "p");

      if (path.startsWith("/pb/")) {
        path = path.substring("/pb".length());
      }
    }
    if (host.endsWith(".wsj.com") || host.equals("wsj.com")) {
      queryParameters.remove("mg");
      queryParameters.remove("mod");
      queryParameters.remove("tesla");
    }

    // Recreate the URL with alphabetized query parameters, lowercased scheme
    // and domain, and consistentized port, and no fragment.
    StringBuilder b = new StringBuilder();
    b.append(url.getProtocol().toLowerCase());
    b.append("://");
    b.append(host.toLowerCase());
    if (url.getPort() != -1 &&
        ("http".equalsIgnoreCase(url.getProtocol()) && url.getPort() != 80 ||
         "https".equalsIgnoreCase(url.getProtocol()) && url.getPort() != 443)) {
      b.append(":" + url.getPort());
    }
    if (path.length() > 1) {
      b.append(path);
    }
    if (queryParameters.size() > 0) {
      List<NameValuePair> nameValuePairList = new ArrayList<>();
      for (String key : queryParameters.keySet()) {
        nameValuePairList.add(new BasicNameValuePair(key, queryParameters.get(key)));
      }
      b.append("?");
      b.append(URLEncodedUtils.format(nameValuePairList, Charsets.UTF_8));
    }
    return b.toString();
  }
}
