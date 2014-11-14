package com.janknspank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Crawler {
  public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";
  private static final DateTimeFormatter ISO_DATE_TIME_FORMAT =
      DateTimeFormat.forPattern(ISO_8601_DATE_FORMAT).withOffsetParsed();
  private static final DateFormat[] KNOWN_DATE_FORMATS = {
      new SimpleDateFormat("MMMM dd, yyyy, hh:mm a"), // CBS News.
      new SimpleDateFormat("MMMM dd, yyyy"), // Chicago Tribune.
      new SimpleDateFormat("yyyy-MM-dd"), // New York Times.
      new SimpleDateFormat("yyyyMMdd") // Washington Post.
  };
  private final CrawlerCallback callback;

  public interface CrawlerCallback {
    public void foundUrl(String url);
    public void foundCrawlData(CrawlData data);
  }

  private class SaxParser extends DefaultHandler {
    private final URL baseUrl;
    private final CrawlData.Builder dataBuilder = new CrawlData.Builder();
    private String lastCharacters;

    public SaxParser(DiscoveredUrl startUrl) {
      try {
        this.baseUrl = new URL(startUrl.getUrl());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
      dataBuilder.setId(startUrl.getId());
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      // Simple sanity check before we allocate a huge string.
      if (length < 1000) {
        lastCharacters = String.copyValueOf(ch, start, length);
      } else {
        lastCharacters = null;
      }
    }

    @Override
    public void endDocument() {
      try {
        Crawler.this.callback.foundCrawlData(dataBuilder.build());
      } catch (ValidationException e) {
        // This is OK - Some documents just don't have enough data.
        System.err.println("Bad crawl data for URL: " + baseUrl.toString());
        e.printStackTrace();
      }
    }

    @Override
    public void endElement(String namespaceURI,
        String localName,
        String qName) throws SAXException {
      if (!dataBuilder.hasTitle() && "title".equalsIgnoreCase(localName)) {
        dataBuilder.setTitle(lastCharacters);
      }
    }

    @Override
    public void startElement(String namespaceURI,
        String localName,
        String qName, 
        Attributes attrs)
        throws SAXException {
      if ("a".equalsIgnoreCase(localName)) {
        String href = attrs.getValue("href");
        if (href != null &&
            !"nofollow".equalsIgnoreCase(attrs.getValue("rel")) &&
            (href.startsWith("http://") || href.startsWith("https://"))) {
          try {
            Crawler.this.callback.foundUrl(new URL(baseUrl, href).toString());
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
        }
      }
      if ("meta".equalsIgnoreCase(localName)) {
        String name = attrs.getValue("name");
        if ("author".equalsIgnoreCase(name)) {
          dataBuilder.setAuthor(attrs.getValue("content"));
        }
        if ("copyright".equalsIgnoreCase(name)) {
          dataBuilder.setCopyright(attrs.getValue("content"));
        }
        if ("description".equalsIgnoreCase(name)) {
          dataBuilder.setDescription(attrs.getValue("content"));
        }
        if ("fb_title".equalsIgnoreCase(name)) {
          dataBuilder.setTitle(attrs.getValue("content"));
        }

        String property = attrs.getValue("property");
        if ("og:title".equalsIgnoreCase(property)) {
          dataBuilder.setTitle(attrs.getValue("content"));
        }
        if ("og:type".equalsIgnoreCase(property)) {
          dataBuilder.setType(attrs.getValue("content"));
        }
        if ("og:image".equalsIgnoreCase(property)) {
          dataBuilder.setImageUrl(attrs.getValue("content"));
        }
        if ("og:description".equalsIgnoreCase(property)) {
          dataBuilder.setDescription(attrs.getValue("content"));
        }

        String itemprop = attrs.getValue("itemprop");
        if ("datePublished".equalsIgnoreCase(itemprop)) {
          dataBuilder.setPublishedTime(parseDateTime(attrs.getValue("content")));
        }
        if ("dateModified".equalsIgnoreCase(itemprop)) {
          dataBuilder.setModifiedTime(parseDateTime(attrs.getValue("content")));
        }
      }
    }

    private Date parseDateTime(String dateStr) {
      if (dateStr == null) {
        return null;
      }
      try {
        // This is the most common date format.
        return ISO_DATE_TIME_FORMAT.parseDateTime(dateStr).toDate();
      } catch (IllegalArgumentException e) {
        for (DateFormat format : KNOWN_DATE_FORMATS) {
          try {
            return format.parse(dateStr);
          } catch (ParseException e2) {
            // This is OK - we just don't match.  Try the next one.
          }
        }
      }
      System.err.println("COULD NOT PARSE DATE: " + dateStr);
      return null;
    }
  }

  public Crawler(CrawlerCallback callback) {
    this.callback = callback;
  }

  public void crawl(DiscoveredUrl url) {
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
        File file = writeToFile(url.getId() + ".html", response.getEntity().getContent());
        SAXParserImpl.newInstance(null).parse(new FileInputStream(file), new SaxParser(url));
      }

    } catch (SAXException | IOException | IllegalArgumentException e) {
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
