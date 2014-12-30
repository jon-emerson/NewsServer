package com.janknspank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.xml.sax.SAXException;

import com.janknspank.ArticleHandler.ArticleCallback;
import com.janknspank.dom.InterpretedData;
import com.janknspank.dom.Interpreter;
import com.janknspank.dom.LenientSaxParser;
import com.janknspank.dom.ParseException;
import com.janknspank.proto.Core.Url;

public class Crawler {
  private final ArticleCallback callback;

  public Crawler(ArticleCallback callback) {
    this.callback = callback;
  }

  public void crawl(Url url) {
    try {
      HttpGet httpget = new HttpGet(url.getUrl());

      // Don't pick up cookies.
      RequestConfig config = RequestConfig.custom()
          .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
          .build();
      CloseableHttpClient httpclient = HttpClients.custom()
          .setDefaultRequestConfig(config)
          .build();

      // Avoid bugs in Apache Http Client from crashing our process.  E.g.
      // https://issues.apache.org/jira/browse/HTTPCLIENT-1544
      CloseableHttpResponse response;
      try {
        response = httpclient.execute(httpget);
      } catch (NullPointerException e) {
        e.printStackTrace();
        return;
      }

      // TODO(jonemerson): Should we update the database if we were
      // redirected, so that it now points at the canonical URL?
      if (response.getStatusLine().getStatusCode() == 200) {
        System.err.println("Crawling: " + url.getUrl());
        File file = writeToFile(url.getId() + ".html", response.getEntity().getContent());
        ArticleHandler handler = new ArticleHandler(callback, url);
        InterpretedData interpretedData =
            new Interpreter(new FileInputStream(file), url.getUrl()).getInterpretedData();
        handler.setArticle(interpretedData.getArticleBody());
        new LenientSaxParser().parse(new FileInputStream(file), handler);
      }

    } catch (SAXException | IOException | IllegalArgumentException | ParseException e) {
      e.printStackTrace();
    }
  }

  private File writeToFile(String filename, InputStream input) throws IOException {
    File file = new File("data/" + filename);
    FileOutputStream fos = new FileOutputStream(file);
    try {
      byte[] buffer = new byte[10240];
      int readBytes = input.read(buffer, 0, buffer.length);
      while (readBytes > 0) {
        fos.write(buffer, 0, readBytes);
        readBytes = input.read(buffer, 0, buffer.length);
      }
    } finally {
      fos.close();
    }
    return file;
  }
}
