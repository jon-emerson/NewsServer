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
    if (url.getHost().endsWith(".abcnews.go.com") || url.getHost().equals("abcnews.go.com")) {
      if (queryParameters.containsKey("page") && queryParameters.get("page").equals("1")) {
        queryParameters.remove("page");
      }
    }
    if (url.getHost().endsWith(".cnn.com") || url.getHost().equals("cnn.com")) {
      queryParameters.remove("eref");
      queryParameters.remove("hpt");
      queryParameters.remove("iid");
      queryParameters.remove("iref");
      queryParameters.remove("sr");
    }
    if (url.getHost().endsWith(".forbes.com") || url.getHost().equals("forbes.com")) {
      queryParameters.remove("linkId");
    }
    if (url.getHost().endsWith(".huffingtonpost.com") || url.getHost().equals("huffingtonpost.com")) {
      queryParameters.remove("ncid");
    }
    if (url.getHost().endsWith(".latimes.com") || url.getHost().equals("latimes.com")) {
      queryParameters.remove("akst_action");
      queryParameters.remove("replytocom");
      queryParameters.remove("track");
    }
    if (url.getHost().endsWith(".msnbc.com") || url.getHost().equals("msnbc.com")) {
      queryParameters.remove("CID");
    }
    if (url.getHost().endsWith(".nytimes.com") || url.getHost().equals("nytimes.com")) {
      queryParameters.remove("_r");
      queryParameters.remove("emc");
      queryParameters.remove("pagewanted");
      queryParameters.remove("partner");
      queryParameters.remove("scp");
      queryParameters.remove("smid");
      queryParameters.remove("sq");
      queryParameters.remove("src");
      queryParameters.remove("st");
    }
    if (url.getHost().endsWith(".reuters.com") || url.getHost().equals("reuters.com")) {
      queryParameters.remove("feedName");
      queryParameters.remove("feedType");
      queryParameters.remove("rpc");
    }
    if (url.getHost().endsWith(".theguardian.com") || url.getHost().equals("theguardian.com")) {
      queryParameters.remove("CMP");
      queryParameters.remove("TrackID");
    }
    if (url.getHost().endsWith(".thehindu.com") || url.getHost().equals("thehindu.com")) {
      queryParameters.remove("homepage");
    }
    if (url.getHost().endsWith(".wsj.com") || url.getHost().equals("wjs.com")) {
      queryParameters.remove("mg");
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
