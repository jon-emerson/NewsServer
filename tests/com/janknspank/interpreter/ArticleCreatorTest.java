package com.janknspank.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;

import com.janknspank.common.DateParserTest;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;

public class ArticleCreatorTest {
  private static final String TITLE = "Article of the century";
  private static final String DESCRIPTION = "Hello there boys and girls";
  private static final Url URL = Url.newBuilder()
      .setUrl("http://www.cnn.com/2014/05/24/hello.html")
      .setId("test id")
      .setTweetCount(0)
      .setDiscoveryTime(500L)
      .build();

  /**
   * Verifies that we handle meta tags.
   */
  @Test
  public void testCreate() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        URL.getUrl(),
        new StringReader("<html><head>" +
            "<meta name=\"keywords\" content=\"BBC, Capital,story,STORY-VIDEO,Office Space\"/>" +
            "<meta name=\"description\" content=\"" + DESCRIPTION + "\"/>" +
            "<title>" + TITLE + "</title>" +
            "</head><body>" +
            "<div class=\"cnn_storyarea\"><p>Super article man!!!</p></div>" +
            "</body</html>"));
    Article article = ArticleCreator.create(URL.getUrl(), documentNode);
    assertEquals(TITLE, article.getTitle());
    assertEquals(DESCRIPTION, article.getDescription());

    // Verify that we pull dates out of article URLs.
    Calendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(article.getPublishedTime());
    assertEquals(2014, calendar.get(Calendar.YEAR));
    assertEquals(4, calendar.get(Calendar.MONTH)); // 0-based months.
    assertEquals(24, calendar.get(Calendar.DAY_OF_MONTH));
  }

  @Test
  public void testNytimesArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://www.nytimes.com/2015/01/04/realestate/year-of-the-condo-in-new-york-city.html",
        new FileReader("testdata/year-of-the-condo-in-new-york-city.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertEquals("Twice as many new condominium units will hit the Manhattan " +
        "market this year as in 2014.", article.getDescription());
    assertEquals("Year of the Condo in New York City", article.getTitle());
    assertEquals("http://static01.nyt.com/images/2015/01/04/realestate/" +
        "04COV4/04COV4-facebookJumbo-v2.jpg", article.getImageUrl());
  }

  @Test
  public void testTechCrunchArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://techcrunch.com/2015/01/03/the-sharing-economy-and-the-future-of-finance/",
        new FileReader("testdata/techcrunch-the-sharing-economy-and-the-future-of-finance.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertEquals("Banking has gone from somewhere you go to something you " +
        "do. If we are to believe that the sharing economy will shape our " +
        "future, banking and all financial services will become something " +
        "that merely exists in the background, similar to other basic " +
        "utilities.", article.getDescription());
    assertEquals("The Sharing Economy And The Future Of Finance", article.getTitle());
    assertEquals("http://tctechcrunch2011.files.wordpress.com/2015/01/shared.jpg",
        article.getImageUrl());
  }

  @Test
  public void testSfgateArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://www.sfgate.com/nation/article/" +
            "News-of-the-day-from-across-the-nation-Jan-7-5997832.php",
        new FileReader("testdata/sfgate-news-of-the-day-jan-7.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertTrue(article.getDescription().startsWith("The launch countdown of " +
        "a rocket carrying equipment and supplies for the International Space " +
        "Station was called off just minutes before it was to lift off from " +
        "Cape Canaveral on Tuesday."));
    assertEquals("News of the day from across the nation, Jan. 7", article.getTitle());
    assertFalse(article.hasImageUrl()); // No decent images.
    assertEquals(6, article.getParagraphCount());
    assertTrue(article.getParagraph(0).contains("The launch countdown of a rocket carrying"));
  }

  @Test
  public void testChronArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://www.chron.com/news/article/Hubble-celebrates-25-years-in-space-5997685.php",
        new FileReader("testdata/chron-hubble-celebrates-25-years.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertEquals(
        "This year makes 25 years since the Hubble telescope was " +
            "sent into outer space to explore the unknown.",
        article.getDescription());
    assertEquals("Hubble celebrates 25 years in space", article.getTitle());
    assertEquals("http://ww1.hdnux.com/photos/34/03/54/7355140/6/rawImage.jpg",
        article.getImageUrl());
    assertEquals(3, article.getParagraphCount());
    assertTrue(article.getParagraph(0).startsWith("This year makes 25 years since"));
    DateParserTest.assertSameTime("2015/06/01", article.getPublishedTime());
  }
}
