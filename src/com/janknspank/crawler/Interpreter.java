package com.janknspank.crawler;

import java.io.Reader;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import com.google.api.client.util.Lists;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.database.Database;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.InterpretedData;
import com.janknspank.proto.ArticleProto.Video;
import com.janknspank.proto.CoreProto.Url;

/**
 * Built off the power of SiteParser, this class further interprets a web page
 * by taking the paragraphs and breaking them down into sentences, tokens, and
 * then into people, organizations, and locations.
 */
public class Interpreter {
  private static final Fetcher FETCHER = new Fetcher();
  private static final int MAX_VIDEO_SOURCE_LENGTH =
      Database.getStringLength(Video.class, "video_source");

  // YouTube URL matcher.  Supports:
  // http://youtu.be/sBakqLUBWP0
  // and
  // https://www.youtube.com/watch?v=sBakqLUBWP0
  // and
  // //www.youtube.com/watch?v=sBakqLUBWP0
  // etc., etc.
  private static final Pattern YOUTUBE_VIDEO_URL_PATTERN = Pattern.compile(
      "^(https?:)?//(youtu.be|www\\.youtube\\.com/watch\\?v=)([^\\&]+)");
  private static final Pattern YOUTUBE_EMBED_URL_PATTERN = Pattern.compile(
      "^(https?:)?//www\\.youtube\\.com/embed/([^\\?]+)");

  /**
   * Retrieves the passed URL by making a request to the respective website,
   * and then interprets the returned results.
   */
  public static InterpretedData interpret(Url url)
      throws FetchException, RequiredFieldException {
    FetchResponse response = null;
    Reader reader = null;
    try {
      response = FETCHER.get(url.getUrl());
      if (response.getStatusCode() != HttpServletResponse.SC_OK) {
        throw new FetchException(
            "URL not found (" + response.getStatusCode() + "): " + url.getUrl());
      }
      Document document = response.getDocument();
      if (ReadWriteArticleHandler.isReadWriteArticle(document)) {
        document = ReadWriteArticleHandler.getRealDocument(document);
      }
      return interpret(url, document);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  private static Iterable<Video> getVideos(Document document, Iterable<String> urls) {
    Set<String> youTubeIdsSoFar = Sets.newHashSet();
    List<Video> videos = Lists.newArrayList();

    // Find YouTube URLs, add them to the article.
    for (String urlString : urls) {
      Matcher matcher = YOUTUBE_VIDEO_URL_PATTERN.matcher(urlString);
      if (matcher.find()) {
        if (youTubeIdsSoFar.contains(matcher.group(3))) {
          continue;
        }
        videos.add(Video.newBuilder()
            .setType("youtube")
            .setYoutubeUrl("https://www.youtube.com/watch?v=" + matcher.group(3))
            .build());
        youTubeIdsSoFar.add(matcher.group(3));
      }
    }

    // Find embedded YouTube videos.
    for (Element iframeEl : document.select("body iframe[src]")) {
      String src = iframeEl.attr("src");
      Matcher matcher = YOUTUBE_EMBED_URL_PATTERN.matcher(src);
      if (matcher.find()) {
        if (youTubeIdsSoFar.contains(matcher.group(2))) {
          continue;
        }
        Video.Builder videoBuilder = Video.newBuilder()
            .setType("youtube")
            .setYoutubeUrl("https://www.youtube.com/watch?v=" + matcher.group(2));
        String width = iframeEl.attr("width");
        if (width != null) {
          videoBuilder.setWidthPx(NumberUtils.toInt(width, 0));
        }
        String height = iframeEl.attr("height");
        if (height != null) {
          videoBuilder.setHeightPx(NumberUtils.toInt(height, 0));
        }
        videos.add(videoBuilder.build());
        youTubeIdsSoFar.add(matcher.group(2));
      }
    }

    // Find HTML5 videos.
    for (Element videoEl : document.select("body video")) {
      String width = videoEl.attr("width");
      String height = videoEl.attr("height");
      for (Node sourceEl : videoEl.select("source[src]")) {
        String source = sourceEl.attr("src");
        if (source.length() < MAX_VIDEO_SOURCE_LENGTH) {
          Video.Builder videoBuilder = Video.newBuilder().setVideoSource(source);
          if (sourceEl.hasAttr("type")) {
            videoBuilder.setType(sourceEl.attr("type"));
          } else {
            videoBuilder.setType("unknown");
          }
          if (width != null) {
            videoBuilder.setWidthPx(NumberUtils.toInt(width, 0));
          }
          if (height != null) {
            videoBuilder.setHeightPx(NumberUtils.toInt(height, 0));
          }
          videos.add(videoBuilder.build());
        }
      }
    }

    return videos;
  }

  /**
   * Advanced method: If we already have the data from the URL, use this method
   * to interpret the web page using said data.
   */
  public static InterpretedData interpret(Url url, Document document)
      throws FetchException, RequiredFieldException {

    // Parse the article and any links it contains.
    Article article = ArticleCreator.create(url, document);
    Iterable<String> urls = UrlFinder.findUrls(document);

    // Are there videos?
    Iterable<Video> videos = getVideos(document, urls);
    if (!Iterables.isEmpty(videos)) {
      article = article.toBuilder().addAllVideo(videos).build();
    }

    return InterpretedData.newBuilder()
        .setArticle(article)
        .addAllUrl(urls)
        .build();
  }

  public static void main(String args[])
      throws FetchException, RequiredFieldException {
    if (args.length != 1 || !args[0].startsWith("http")) {
      System.out.println("Tell us what URL to interpret please... and only 1!");
    }
    System.out.println(Interpreter.interpret(Url.newBuilder().setUrl(args[0]).build()));
  }
}
