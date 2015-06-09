package com.janknspank.crawler;

import java.io.Reader;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.api.client.util.Lists;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.dom.parser.ParserException;
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
      throws FetchException, ParserException, RequiredFieldException {
    FetchResponse response = null;
    Reader reader = null;
    try {
      response = FETCHER.get(url.getUrl());
      if (response.getStatusCode() != HttpServletResponse.SC_OK) {
        throw new FetchException(
            "URL not found (" + response.getStatusCode() + "): " + url.getUrl());
      }
      DocumentNode documentNode = response.getDocumentNode();
      if (ReadWriteArticleHandler.isReadWriteArticle(documentNode)) {
        documentNode = ReadWriteArticleHandler.getRealDocumentNode(documentNode);
      }
      return interpret(url, documentNode);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  private static Iterable<Video> getVideos(DocumentNode documentNode, Iterable<String> urls) {
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
    for (Node iframeNode : documentNode.findAll("body iframe[src]")) {
      String src = iframeNode.getAttributeValue("src");
      Matcher matcher = YOUTUBE_EMBED_URL_PATTERN.matcher(src);
      if (matcher.find()) {
        if (youTubeIdsSoFar.contains(matcher.group(2))) {
          continue;
        }
        Video.Builder videoBuilder = Video.newBuilder()
            .setType("youtube")
            .setYoutubeUrl("https://www.youtube.com/watch?v=" + matcher.group(2));
        String width = iframeNode.getAttributeValue("width");
        if (width != null) {
          videoBuilder.setWidthPx(NumberUtils.toInt(width, 0));
        }
        String height = iframeNode.getAttributeValue("height");
        if (height != null) {
          videoBuilder.setHeightPx(NumberUtils.toInt(height, 0));
        }
        videos.add(videoBuilder.build());
        youTubeIdsSoFar.add(matcher.group(2));
      }
    }

    // Find HTML5 videos.
    for (Node videoNode : documentNode.findAll("body video")) {
      String width = videoNode.getAttributeValue("width");
      String height = videoNode.getAttributeValue("height");
      for (Node sourceNode : videoNode.findAll("source[src]")) {
        Video.Builder videoBuilder = Video.newBuilder()
            .setVideoSource(sourceNode.getAttributeValue("src"));
        if (sourceNode.hasAttribute("type")) {
          videoBuilder.setType(sourceNode.getAttributeValue("type"));
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

    return videos;
  }

  /**
   * Advanced method: If we already have the data from the URL, use this method
   * to interpret the web page using said data.
   */
  public static InterpretedData interpret(Url url, DocumentNode documentNode)
      throws FetchException, ParserException, RequiredFieldException {

    // Parse the article and any links it contains.
    Article article = ArticleCreator.create(url, documentNode);
    Iterable<String> urls = UrlFinder.findUrls(documentNode);

    // Are there videos?
    Iterable<Video> videos = getVideos(documentNode, urls);
    if (!Iterables.isEmpty(videos)) {
      article = article.toBuilder().addAllVideo(videos).build();
    }

    return InterpretedData.newBuilder()
        .setArticle(article)
        .addAllUrl(urls)
        .build();
  }

  public static void main(String args[])
      throws FetchException, ParserException, RequiredFieldException {
    if (args.length != 1 || !args[0].startsWith("http")) {
      System.out.println("Tell us what URL to interpret please... and only 1!");
    }
    System.out.println(Interpreter.interpret(Url.newBuilder().setUrl(args[0]).build()));
  }
}
