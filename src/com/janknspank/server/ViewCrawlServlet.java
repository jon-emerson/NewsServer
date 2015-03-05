package com.janknspank.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CrawlerProto.CrawlHistory;

@ServletMapping(urlPattern = "/viewCrawl")
public class ViewCrawlServlet extends HttpServlet {
  SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      PrintWriter pw = new PrintWriter(response.getOutputStream());
      pw.write("<html><body><ul>");
      for (CrawlHistory history : Database.with(CrawlHistory.class).get(
          new QueryOption.DescendingSort("start_time"),
          new QueryOption.Limit(50))) {
        pw.write("<li>" + DATE_FORMAT.format(history.getStartTime())
            + " (<b>" + history.getHost() + "</b>, " + history.getMillis() / 100 + " seconds)");
        pw.write("<ul>");
        pw.write("<li>Interrupted: " + history.getWasInterrupted());
        pw.write("<li>" + history.getSiteCount() + " sites crawled");

        TopList<String, Long> slowestSites = new TopList<>(10);
        TopList<String, Integer> biggestSites = new TopList<>(10);
        for (CrawlHistory.Site site : history.getSiteList()) {
          String descriptor = site.getRootDomain() + " (" + site.getArticlesCrawled() + " articles, "
              + site.getMillis() / 1000 + " seconds)";
          slowestSites.add(descriptor, site.getMillis());
          if (site.getArticlesCrawled() > 0) {
            biggestSites.add(descriptor, site.getArticlesCrawled());
          }
        }
        pw.write("<li>Slowest sites:");
        pw.write("<ul>");
        for (String slowestSite : slowestSites) {
          pw.write("<li>" + slowestSite);
        }
        pw.write("</ul>");

        pw.write("<li>Most fruitful sites:");
        pw.write("<ul>");
        for (String biggestSite : biggestSites) {
          pw.write("<li>" + biggestSite);
        }
        pw.write("</ul>");

        pw.write("</ul>");
      }
      pw.write("</ul></body></html>");
      pw.flush();
    } catch (DatabaseSchemaException | IOException e) {
      e.printStackTrace();
    }
  }
}
