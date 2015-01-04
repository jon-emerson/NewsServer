package com.janknspank;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.janknspank.dom.InterpretedData;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.LenientXMLReader;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;

public class ArticleHandlerTest {
  private static final List<Node> ARTICLE_NODES = ImmutableList.of(
      new Node(new DocumentNode(), "html", 0));
  private static final String DESCRIPTION = "Hello there boys and girls";
  private static final String LINK_URL_1 = "http://www.cnn.com/foo1?query=true&utm_campaign=bar";
  private static final String LINK_URL_2 = "http://nyti.ms/asda35qwas";
  private static final String LINK_URL_3 = "http://www.google.com/?q=moo";
  private static final Url URL = Url.newBuilder()
        .setUrl("http://www.cnn.com/2014/05/24/hello.html")
        .setId("test id")
        .setTweetCount(0)
        .setDiscoveryTime(500L)
        .build();
  private TestArticleCallback callback;
  private ArticleHandler handler;

  private static class TestArticleCallback implements ArticleHandler.ArticleCallback {
    List<String> foundUrls = Lists.newArrayList();
    List<Article> foundArticles = Lists.newArrayList();
    Set<String> keywords = null;

    @Override
    public void foundUrl(String url) {
      foundUrls.add(url);
    }

    @Override
    public void foundArticle(Article article,
        InterpretedData interpretedData,
        Set<String> keywords) {
      assertTrue(foundArticles.isEmpty());
      foundArticles.add(article);
      this.keywords = keywords;
    }
  }

  @Before
  public void setUp() {
    callback = new TestArticleCallback();
    handler = new ArticleHandler(callback, URL);
  }

  /**
   * Verifies that we pull dates out of article URLs.
   */
  @Test
  public void testFindDateInUrl() throws Exception {
    ArticleHandler handler = new ArticleHandler(new TestArticleCallback(), URL);

    // Pull the published time from the Handler and see what date it has set.
    Calendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(handler.articleBuilder.getPublishedTime());
    assertEquals(2014, calendar.get(Calendar.YEAR));
    assertEquals(4, calendar.get(Calendar.MONTH)); // 0-based months.
    assertEquals(24, calendar.get(Calendar.DAY_OF_MONTH));
  }

  /**
   * Verifies that we handle meta tags.
   */
  @Test
  public void testHandleMetaTags() throws Exception {
    handler.setInterpretedData(
        new InterpretedData.Builder().setArticleNodes(ARTICLE_NODES).build());
    String htmlPage =
        "<html><head>" +
        "<meta name=\"keywords\" content=\"BBC, Capital,story,STORY-VIDEO,Office Space\">" +
        "<meta name=\"description\" content=\"" + DESCRIPTION + "\">" +
        "<title>Title</title>" +
        "</head><body>" +
        "<a href=\"" + LINK_URL_1 + "\">" +
        "<a class=\"clazzzzzz moo\" href=\"" + LINK_URL_2 + "\">" +
        "<a id=\"hello\" href=\"" + LINK_URL_3 + "\">" +
        "</body</html>";
    XMLReader reader = new LenientXMLReader();
    reader.setContentHandler(handler);
    reader.parse(new InputSource(new StringReader(htmlPage)));

    assertTrue(callback.keywords.contains("BBC"));
    assertTrue(callback.keywords.contains("Capital"));
    assertTrue(callback.keywords.contains("story"));
    assertTrue(callback.keywords.contains("STORY-VIDEO"));
    assertTrue(callback.keywords.contains("Office Space"));
    assertEquals(1, callback.foundArticles.size());

    Article article = callback.foundArticles.get(0);
    assertEquals(DESCRIPTION, article.getDescription());
    assertEquals(3, callback.foundUrls.size());
    assertEquals(LINK_URL_1, callback.foundUrls.get(0));
    assertEquals(LINK_URL_2, callback.foundUrls.get(1));
    assertEquals(LINK_URL_3, callback.foundUrls.get(2));
  }

  @Test
  public void handleMetaTagsNytimes() throws Exception {
    handler.setInterpretedData(
        new InterpretedData.Builder().setArticleNodes(ARTICLE_NODES).build());

    XMLReader reader = new LenientXMLReader();
    reader.setContentHandler(handler);
    reader.parse(new InputSource(new FileReader(
        "testdata/year-of-the-condo-in-new-york-city.html")));

    assertEquals(1, callback.foundArticles.size());

    Article article = callback.foundArticles.get(0);
    assertEquals("Twice as many new condominium units will hit the Manhattan " +
        "market this year as in 2014.", article.getDescription());
    assertEquals("Year of the Condo in New York City", article.getTitle());
    assertEquals("http://static01.nyt.com/images/2015/01/04/realestate/" +
        "04COV4/04COV4-facebookJumbo-v2.jpg", article.getImageUrl());
  }
}
