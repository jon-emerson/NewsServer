package com.janknspank.crawler;

import java.io.StringReader;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.Fetcher;

/**
 * Uses ReadWrite.com's AJAX endpoint to fetch article contents.
 */
public class ReadWriteArticleHandler {
  private static final Fetcher FETCHER = new Fetcher();

  public static boolean isReadWriteArticle(DocumentNode documentNode) {
    String url = Strings.nullToEmpty(documentNode.getUrl());
    return url.startsWith("http://readwrite.com/")
        || url.startsWith("https://readwrite.com/");
  }

  public static DocumentNode getRealDocumentNode(final DocumentNode documentNode)
      throws RequiredFieldException {
    String pageUrl = documentNode.getUrl();
    if (!isReadWriteArticle(documentNode)) {
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
      return DocumentBuilder.build(pageUrl, new StringReader(
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
          + "</html>"));
    } catch (JSONException | ParserException e) {
      throw new RequiredFieldException("Could not parse article contents", e);
    } catch (FetchException e) {
      throw new RequiredFieldException("Could not fetch article contents", e);
    }
  }
}
