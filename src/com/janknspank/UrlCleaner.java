package com.janknspank;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.codec.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 * Utility class for cleaning up news site URLs, so they can be reduced to their
 * canonical forms.
 */
public class UrlCleaner {
  public static String clean(String dirtyUrl) throws MalformedURLException {
    URL url = new URL(dirtyUrl);
    TreeMap<String, String> queryParameters = new TreeMap<>(new Comparator<String>() {
      @Override
      public int compare(String one, String two) {
        return one.compareTo(two);
      }
    });
    for (NameValuePair nameValue : URLEncodedUtils.parse(url.getQuery(), Charsets.UTF_8)) {
      queryParameters.put(nameValue.getName(), nameValue.getValue());
    }

    queryParameters.remove("utm_medium");
    queryParameters.remove("utm_campaign");
    queryParameters.remove("utm_channel");
    queryParameters.remove("utm_content");
    queryParameters.remove("utm_hp_ref");
    queryParameters.remove("utm_term");
    queryParameters.remove("utm_source");
    if (url.getHost().endsWith(".abc.net.au") || url.getHost().equals("abc.net.au")) {
      queryParameters.remove("cid");
      queryParameters.remove("pfm");
    }
    if (url.getHost().endsWith(".abcnews.go.com") || url.getHost().equals("abcnews.go.com")) {
      if (queryParameters.containsKey("page") && queryParameters.get("page").equals("1")) {
        queryParameters.remove("page");
      }
    }
    if (url.getHost().endsWith(".arstechnica.com") || url.getHost().equals("arstechnica.com")) {
      queryParameters.remove("comments");
      queryParameters.remove("post");
      queryParameters.remove("theme");
      queryParameters.remove("view");
    }
    if (url.getHost().endsWith(".bbc.co.uk") || url.getHost().equals("bbc.co.uk") ||
        url.getHost().endsWith(".bbc.com") || url.getHost().equals("bbc.com")) {
      queryParameters.remove("filter");
    }
    if (url.getHost().endsWith(".bloomberg.com") || url.getHost().equals("bloomberg.com")) {
      queryParameters.remove("hootPostID");
    }
    if (url.getHost().endsWith(".boston.com") || url.getHost().equals("boston.com")) {
      queryParameters.remove("p1");
    }
    if (url.getHost().endsWith(".businessweek.com") || url.getHost().equals("businessweek.com")) {
      queryParameters.remove("hootPostID");
    }
    if (url.getHost().endsWith(".chron.com") || url.getHost().equals("chron.com")) {
      queryParameters.remove("cmpid");
    }
    if (url.getHost().endsWith(".cnn.com") || url.getHost().equals("cnn.com")) {
      queryParameters.remove("eref");
      queryParameters.remove("hpt");
      queryParameters.remove("iid");
      queryParameters.remove("iref");
      queryParameters.remove("nbd");
      queryParameters.remove("npt");
      queryParameters.remove("sr");
      queryParameters.remove("source");
      queryParameters.remove("_s");
    }
    if (url.getHost().endsWith(".chicagotribune.com") || url.getHost().equals("chicagotribune.com")) {
      queryParameters.remove("cid");
    }
    if (url.getHost().endsWith(".economist.com") || url.getHost().equals("economist.com")) {
      queryParameters.remove("fsrc");
    }
    if (url.getHost().endsWith(".forbes.com") || url.getHost().equals("forbes.com")) {
      queryParameters.remove("commentId");
      queryParameters.remove("feed");
      queryParameters.remove("linkId");
    }
    if (url.getHost().endsWith(".foxnews.com") || url.getHost().equals("foxnews.com")) {
      queryParameters.remove("cmpid");
      queryParameters.remove("intcmp");
    }
    if (url.getHost().endsWith(".guardian.co.uk") || url.getHost().equals("guardian.co.uk")) {
      queryParameters.remove("intcmp");
      queryParameters.remove("INTCMP");
    }
    if (url.getHost().endsWith(".huffingtonpost.com") || url.getHost().equals("huffingtonpost.com")) {
      // "ir" might be bad to remove.  It controls which header people see on the top of the page.
      // But I'm removing it because it's better to consolidate the same articles.
      queryParameters.remove("ir"); 

      queryParameters.remove("m");
      queryParameters.remove("ncid");
    }
    if (url.getHost().endsWith(".latimes.com") || url.getHost().equals("latimes.com")) {
      queryParameters.remove("akst_action");
      queryParameters.remove("dlvrit");
      queryParameters.remove("replytocom");
      queryParameters.remove("track");
    }
    if (url.getHost().endsWith(".mashable.com") || url.getHost().equals("mashable.com")) {
      queryParameters.remove("utm_cid");
    }
    if (url.getHost().endsWith(".msnbc.com") || url.getHost().equals("msnbc.com")) {
      queryParameters.remove("CID");
    }
    if (url.getHost().endsWith(".nationalgeographic.com") ||
        url.getHost().equals("nationalgeographic.com")) {
      queryParameters.remove("now");
    }
    if (url.getHost().endsWith(".news.yahoo.com") || url.getHost().equals("news.yahoo.com")) {
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
    if (url.getHost().endsWith(".nytimes.com") || url.getHost().equals("nytimes.com")) {
      queryParameters.remove("_r");
      queryParameters.remove("WT_nav");
      queryParameters.remove("WT.mc_id");
      queryParameters.remove("WT.mc_ev");
      queryParameters.remove("WT.mc_c");
      queryParameters.remove("WT.nav");
      queryParameters.remove("action");
      queryParameters.remove("contentCollection");
      queryParameters.remove("emc");
      queryParameters.remove("hp");
      queryParameters.remove("inline");
      queryParameters.remove("module");
      queryParameters.remove("nl"); // Found on http://learning.blogs.nytimes.com/.
      queryParameters.remove("nlid"); // Found on http://learning.blogs.nytimes.com/.
      queryParameters.remove("pagewanted");
      queryParameters.remove("partner");
      queryParameters.remove("pgtype");
      queryParameters.remove("ref");
      queryParameters.remove("region");
      queryParameters.remove("ribbon-ad-idx");
      queryParameters.remove("rref");
      queryParameters.remove("scp");
      queryParameters.remove("smid");
      queryParameters.remove("sq");
      queryParameters.remove("src");
      queryParameters.remove("st");
      queryParameters.remove("version");
    }
    if (url.getHost().endsWith(".reuters.com") || url.getHost().equals("reuters.com")) {
      queryParameters.remove("feedName");
      queryParameters.remove("feedType");
      queryParameters.remove("rpc");
    }
    if (url.getHost().endsWith(".sfgate.com") || url.getHost().equals("sfgate.com")) {
      queryParameters.remove("cmpid");
    }
    if (url.getHost().endsWith(".theguardian.com") || url.getHost().equals("theguardian.com")) {
      queryParameters.remove("CMP");
      queryParameters.remove("TrackID");
    }
    if (url.getHost().endsWith(".thehindu.com") || url.getHost().equals("thehindu.com")) {
      queryParameters.remove("homepage");
    }
    if (url.getHost().endsWith(".washingtonpost.com") ||
        url.getHost().equals("washingtonpost.com")) {
      queryParameters.remove("tid");
    }
    if (url.getHost().endsWith(".wsj.com") || url.getHost().equals("wjs.com")) {
      queryParameters.remove("mg");
      queryParameters.remove("mod");
      queryParameters.remove("tesla");
    }

    // Recreate the URL with alphabetized query parameters, lowercased scheme
    // and domain, and consistentized port, and no fragment.
    StringBuilder b = new StringBuilder();
    b.append(url.getProtocol().toLowerCase());
    b.append("://");
    b.append(url.getHost().toLowerCase());
    if (url.getPort() != -1 &&
        ("http".equalsIgnoreCase(url.getProtocol()) && url.getPort() != 80 ||
         "https".equalsIgnoreCase(url.getProtocol()) && url.getPort() != 443)) {
      b.append(":" + url.getPort());
    }
    if (url.getPath().length() > 1) {
      b.append(url.getPath());
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
