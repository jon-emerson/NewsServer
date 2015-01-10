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
        new StringReader("<html><head>"
            + "<meta name=\"keywords\" content=\"BBC, Capital,story,STORY-VIDEO,Office Space\"/>"
            + "<meta name=\"description\" content=\"" + DESCRIPTION + "\"/>"
            + "<title>&#8203;National Society of Film Critics goes for Godard - CBS News</title>"
            + "</head><body>"
            + "<div class=\"cnn_storyarea\"><p>Super article man!!!</p></div>"
            + "</body</html>"));
    Article article = ArticleCreator.create(URL.getUrl(), documentNode);
    assertEquals("National Society of Film Critics goes for Godard", article.getTitle());
    assertEquals(DESCRIPTION, article.getDescription());

    // Verify that we pull dates out of article URLs.
    Calendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(article.getPublishedTime());
    assertEquals(2014, calendar.get(Calendar.YEAR));
    assertEquals(4, calendar.get(Calendar.MONTH)); // 0-based months.
    assertEquals(24, calendar.get(Calendar.DAY_OF_MONTH));
  }

  private DocumentNode getDocumentWithTitle(String title) throws Exception {
    return DocumentBuilder.build("url",
        new StringReader("<html><head><title>" + title + "</title></head></html>"));
  }

  @Test
  public void testGetTitle() throws Exception {
    assertEquals("National Society of Film Critics goes for Godard",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "&#8203;National Society of Film Critics goes for Godard - CBS News")));
    assertEquals("Cops turn backs on deBlasio at officer's funeral",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Cops turn backs on deBlasio at officer's funeral | Al Jazeera America")));
    assertEquals("Ten years on: Oxfam digests lessons from 2004 Indian Ocean tsunami",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Ten years on: Oxfam digests lessons from 2004 Indian Ocean tsunami - CNN.com")));
    assertEquals("People don’t work as hard on hot days – or on a warming planet",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "People don’t work as hard on hot days – or on a warming planet")));
    assertEquals("3-Bed, 2.5-Bath in Washington West Gets $50K Price Bump",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "3-Bed, 2.5-Bath in Washington West Gets $50K Price Bump")));
    assertEquals("From the Funeral Home to East Boston, Parlor is Custom Skiing to the Core",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "From the Funeral Home to East Boston, Parlor is Custom Skiing to the "
            + "Core - Eric Wilbur's Sports Blog - Boston.com")));
    assertEquals("Happy New Year.",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Happy New Year. - Love Letters - Boston.com")));
    assertEquals("Sydney hostage-taker called himself a cleric -- and had a criminal record",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Sydney hostage-taker called himself a cleric -- and had a criminal "
            + "record - CNN.com")));
    assertEquals("Good Times From Texas to North Dakota May Turn Bad on Oil-Price Drop",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Good Times From Texas to North Dakota May Turn Bad on Oil-Price Drop")));
    assertEquals("Why didn’t Rolling Stone tell readers about U-Va. denial?",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Why didn’t Rolling Stone tell readers about U-Va. denial?")));
    assertEquals("The gorgeously-situated weather radar that’s out of this world",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "The gorgeously-situated weather radar that’s out of this world (VIDEO)")));
    assertEquals("Terry's Talkin' about Cleveland Browns, Justin Gilbert and the draft",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Terry's Talkin' about Cleveland Browns, Justin Gilbert and the "
            + "draft -- Terry Pluto (video)")));
    assertEquals("Bloomberg Best: From Our Bureaus Worldwide – August 14",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Bloomberg Best: From Our Bureaus Worldwide – August 14 (Audio)")));
    assertEquals("Cleveland Browns' 2014: Mike Pettine's season was \"solid\"",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Cleveland Browns' 2014: Mike Pettine's season was "
            + "\"solid\" -- Bud Shaw's Sports Spin (videos)")));
    assertEquals("The legitimacy of Israel’s nation-state bill (II): diplomatic considerations",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "The legitimacy of Israel’s nation-state bill (II): diplomatic considerations")));
    assertEquals("Gogo plane Wi-Fi blocks YouTube (and can read your email)",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Gogo plane Wi-Fi blocks YouTube (and can read your email)")));
    assertEquals("Defenseman Mike Green (upper-body) missing Tampa Bay trip",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Defenseman Mike Green (upper-body) missing Tampa Bay trip")));
    assertEquals("Why Brad Paisley’s self-deprecating ‘black-ish’/’white-ish’ joke at the "
            + "CMAs was a bad idea. (But not racist.)",
        ArticleCreator.getTitle(getDocumentWithTitle(
            "Why Brad Paisley’s self-deprecating ‘black-ish’/’white-ish’ joke at the "
            + "CMAs was a bad idea. (But not racist.)")));
  }

  @Test
  public void testGetPublishedTime() throws Exception {
    // From Cbc.ca.
    DocumentNode documentNode = DocumentBuilder.build("url",
        new StringReader("<html><head><title><meta name=\"date\" content=\"2015/01/07\" />"
            + "</title></head></html>"));
    DateParserTest.assertSameTime("20150107000000", ArticleCreator.getPublishedTime(documentNode));

    // From Cbsnews.com.
    documentNode = DocumentBuilder.build("url",
        new StringReader("<html><head><title>"
            + "<meta itemprop=\"datePublished\" content=\"January 9, 2015, 3:43 AM\">"
            + "</title></head></html>"));
    DateParserTest.assertSameTime("20150109034300", ArticleCreator.getPublishedTime(documentNode));

    // From abc.net.au: Get the date from the URL.
    documentNode = DocumentBuilder.build(
        "http://www.abc.net.au/news/2015-01-01/victims-of-sydney-to-hobart-yacht-"
        + "race-plane-crash/5995656",
        new StringReader("<html><head></head></html>"));
    DateParserTest.assertSameTime("20150101000000", ArticleCreator.getPublishedTime(documentNode));

    // From http://advice.careerbuilder.com/: Get the date from the copyright notice.
    documentNode = DocumentBuilder.build(
        "http://advice.careerbuilder.com/posts/how-to-be-a-great-career-wingman",
        new StringReader("<html><body><div id=\"post-content\">"
            + "<div class=\"copyright\">© 2014 CareerBuilder, LLC. "
            + "Original publish date: 12.26.2014</div>"
            + "</body></html>"));
    DateParserTest.assertSameTime("20141226000000", ArticleCreator.getPublishedTime(documentNode));
  }

  @Test
  public void testNytimesArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://www.nytimes.com/2015/01/04/realestate/year-of-the-condo-in-new-york-city.html",
        new FileReader("testdata/year-of-the-condo-in-new-york-city.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertEquals("Twice as many new condominium units will hit the Manhattan "
        + "market this year as in 2014.", article.getDescription());
    assertEquals("Year of the Condo in New York City", article.getTitle());
    assertEquals("http://static01.nyt.com/images/2015/01/04/realestate/"
        + "04COV4/04COV4-facebookJumbo-v2.jpg", article.getImageUrl());
  }

  @Test
  public void testTechCrunchArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://techcrunch.com/2015/01/03/the-sharing-economy-and-the-future-of-finance/",
        new FileReader("testdata/techcrunch-the-sharing-economy-and-the-future-of-finance.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertEquals("Banking has gone from somewhere you go to something you "
        + "do. If we are to believe that the sharing economy will shape our "
        + "future, banking and all financial services will become something "
        + "that merely exists in the background, similar to other basic "
        + "utilities.", article.getDescription());
    assertEquals("The Sharing Economy And The Future Of Finance", article.getTitle());
    assertEquals("http://tctechcrunch2011.files.wordpress.com/2015/01/shared.jpg",
        article.getImageUrl());
  }

  @Test
  public void testSfgateArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://www.sfgate.com/nation/article/"
        + "News-of-the-day-from-across-the-nation-Jan-7-5997832.php",
        new FileReader("testdata/sfgate-news-of-the-day-jan-7.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertTrue(article.getDescription().startsWith("The launch countdown of "
        + "a rocket carrying equipment and supplies for the International Space "
        + "Station was called off just minutes before it was to lift off from "
        + "Cape Canaveral on Tuesday."));
    assertEquals("News of the day from across the nation, Jan. 7", article.getTitle());
    assertFalse(article.hasImageUrl()); // No decent images.
    assertEquals(6, article.getParagraphCount());
    assertTrue("Unexpected first paragraph: " + article.getParagraph(0),
        article.getParagraph(0).contains("The launch countdown of a rocket carrying"));
  }

  @Test
  public void testChronArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://www.chron.com/news/article/Hubble-celebrates-25-years-in-space-5997685.php",
        new FileReader("testdata/chron-hubble-celebrates-25-years.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertEquals(
        "This year makes 25 years since the Hubble telescope was "
        + "sent into outer space to explore the unknown.",
        article.getDescription());
    assertEquals("Hubble celebrates 25 years in space", article.getTitle());
    assertEquals("http://ww1.hdnux.com/photos/34/03/54/7355140/6/rawImage.jpg",
        article.getImageUrl());
    assertEquals(3, article.getParagraphCount());
    assertTrue("Unexpected first paragraph: " + article.getParagraph(0),
        article.getParagraph(0).startsWith("This year makes 25 years since"));
    DateParserTest.assertSameTime("2015/06/01", article.getPublishedTime());
  }

  @Test
  public void testBbcArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://www.bbc.com/future/story/20141219-why-does-guilt-increase-pleasure",
        new FileReader("testdata/bbc-why-does-guilt-increase-pleasure.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertEquals(
        "Feelings of guilt can make a temptations feel even more seductive. "
        + "So could we be healthier if we just embraced a little bit of vice, "
        + "asks David Robson.",
        article.getDescription());
    assertEquals("Psychology: Why does guilt increase pleasure?", article.getTitle());
    assertEquals("http://ichef.bbci.co.uk/wwfeatures/624_351/images/live/p0/2f/l8/p02fl8qx.jpg",
        article.getImageUrl());
    assertEquals(22, article.getParagraphCount());
    assertTrue("Unexpected first paragraph: " + article.getParagraph(0),
        article.getParagraph(0).startsWith(
            "This year, my New Year’s Resolutions are going to take a somewhat different "
            + "form to those of previous Januaries."));
  }

  @Test
  public void testBloombergArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://www.bloomberg.com/politics/articles/2014-12-30/the-new-york-times-joins-"
        + "the-nypd-funeral-protest-backlash",
        new FileReader("testdata/bloomberg-nypd-funeral-protest-backlash.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertEquals(
        "The editorial board criticized what it called one of several acts of "
        + "“passive-aggressive contempt and self-pity.”",
        article.getDescription());
    assertEquals("The New York Times Joins the NYPD Funeral Protest Backlash", article.getTitle());
    assertEquals("http://media.gotraffic.net/images/iqh7RbW8gmWo/v6/-1x-1.jpg",
        article.getImageUrl());
    assertTrue("Unexpected first paragraph: " + article.getParagraph(0),
        article.getParagraph(0).startsWith(
            "Before NYPD Officers Rafael Ramos and Wenjian Liu were ambushed while on "
            + "patrol in Brooklyn, the Patrolmen’s Benevolent Association"));
  }

  @Test
  public void testFortuneArticle() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://fortune.com/2012/04/06/gm-sees-self-driving-cars-sooner-not-later/",
        new FileReader("testdata/fortune-gm-sees-self-driving-cars-sooner-not-later.html"));
    Article article = ArticleCreator.create("urlId", documentNode);
    assertEquals(
        "An array of new sensors, warnings and automatic controls can already help drivers "
        + "detect hazardous situations and avoid accidents. More advanced cars aren't that "
        + "far away, the company says.",
        article.getDescription());
    assertEquals("GM sees self-driving cars sooner, not later", article.getTitle());
    assertEquals("http://subscription-assets.timeinc.com/current/510_top1_150_thumb.jpg",
        article.getImageUrl());
    assertTrue("Unexpected first paragraph: " + article.getParagraph(0),
        article.getParagraph(0).startsWith(
            "FORTUNE — Self-driving cars may be closer than anybody realizes."));
  }
}
