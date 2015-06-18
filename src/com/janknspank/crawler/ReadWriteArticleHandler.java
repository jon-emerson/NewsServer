package com.janknspank.crawler;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.common.base.Strings;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.Fetcher;

/**
 * Uses ReadWrite.com's AJAX endpoint to fetch article contents.
 */
public class ReadWriteArticleHandler {
  private static final Fetcher FETCHER = new Fetcher();

  public static boolean isReadWriteArticle(Document document) {
    String url = Strings.nullToEmpty(document.baseUri());
    return url.startsWith("http://readwrite.com/")
        || url.startsWith("https://readwrite.com/");
  }

  public static Document getRealDocument(final Document document)
      throws RequiredFieldException {
    String pageUrl = document.baseUri();
    if (!isReadWriteArticle(document)) {
      throw new RequiredFieldException("Invalid site URL for ReadWriteParagraphFinder: " + pageUrl);
    }

    String articleName = pageUrl.substring(pageUrl.lastIndexOf("/") + 1);
    String ajaxPath = "http://api.readwrite.com/:apiproxy-anon/content-sites/cs019099924683860e/"
        + "articles/@published/@by-slug/" + articleName;
    try {
      String response = FETCHER.getResponseBody(ajaxPath);
      JSONObject responseObj = new JSONObject(response);
      JSONArray entries = responseObj.getJSONArray("entries");
      JSONObject articleObj = entries.getJSONObject(0);
      return Jsoup.parse(
          "<html>"
          + "<head>"
          + "<title>" + StringEscapeUtils.escapeHtml4(articleObj.getString("title")) + "</title>"
          + "<meta itemprop=\"date\">"
              + StringEscapeUtils.escapeHtml4(articleObj.getString("createdTimestamp")) + "</meta>"
          + "<meta itemprop=\"modificationDate\">"
              + StringEscapeUtils.escapeHtml4(articleObj.getString("lastModifiedTimestamp")) + "</meta>"
          + "</head>"
          + "<body>"
          + articleObj.getString("bodyTml")
          + "</body>"
          + "</html>",
          pageUrl);
    } catch (JSONException e) {
      throw new RequiredFieldException("Unexpected JSON problem", e);
    } catch (FetchException e) {
      throw new RequiredFieldException("Could not fetch article contents", e);
    }
  }
}
