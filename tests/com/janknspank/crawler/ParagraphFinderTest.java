package com.janknspank.crawler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.janknspank.proto.CrawlerProto.SiteManifest;

public class ParagraphFinderTest {
  private static final String CNN_MONEY_PARAGRAPH_0 = "In a play to dominate messaging on "
      + "phones and the Web, Facebook has acquired WhatsApp for $19 billion.";
  private static final String CNN_MONEY_PARAGRAPH_1 = "That's a stunning sum for the five-year old "
      + "company. But WhatsApp has been able to hold its weight against messaging heavyweights "
      + "like Twitter (TWTR), Google (GOOG) and Microsoft's (MSFT) Skype. WhatsApp has upwards of "
      + "450 million users, and it is adding an additional million users every day.";
  private static final String CNN_MONEY_PARAGRAPH_2 = "Referring to WhatsApp's soaring growth, "
      + "Facebook CEO Mark Zuckerberg said on a conference call, \"No one in the history of the "
      + "world has done anything like that.\"";
  private static final String CNN_MONEY_PARAGRAPH_3 = "WhatsApp is the most popular messaging app "
      + "for smartphones, according to OnDevice Research";
  private static final String CNN_MONEY_PARAGRAPH_4 = "Buying WhatsApp will only bolster "
      + "Facebook's already strong position in the crowded messaging world. Messenger, Facebook's "
      + "a standalone messaging app for mobile devices, is second only to WhatsApp in its share of "
      + "the smartphone market.";
  private static final String CNN_MONEY_PARAGRAPH_5 = "Similar to traditional text messaging, "
      + "WhatsApp allows people to connect via their cellphone numbers. But instead of racking up "
      + "texting fees, WhatsApp sends the actual messages over mobile broadband. That makes "
      + "WhatsApp particularly cost effective for communicating with people overseas.";
  private static final String CNN_MONEY_PARAGRAPH_6 = "That kind of mobile messaging services have "
      + "become wildly popular, with twice as many messages sent over the mobile Internet than via "
      + "traditional texts, according to Deloitte. But most of the messaging industry's revenue is "
      + "still driven by text messaging.";
  private static final String CNN_MONEY_PARAGRAPH_7 = "On the conference call, Facebook said it is "
      + "not looking to drive revenue from WhatsApp in the near term, instead focusing on growth. "
      + "Zuckerberg said he doesn't anticipate trying to aggressively grow WhatsApp's revenue "
      + "until the service reaches \"billions\" of users.";
  private static final String CNN_MONEY_PARAGRAPH_8 = "WhatsApp currently charges a dollar a year "
      + "after giving customers their first year of use for free. WhatsApp CEO Jan Koum said on "
      + "the conference call that WhatsApp's business model is already successful.";
  private static final String CNN_MONEY_PARAGRAPH_9 = "That indicates Facebook bought WhatsApp to "
      + "add value to its existing messaging services, as well as for the long-term potential of "
      + "the company.";
  private static final String CNN_MONEY_PARAGRAPH_10 = "Facebook bought Instagram for $1 billion "
      + "in 2012 for similar reasons: As young social network users gravitated towards "
      + "photo-sharing, Facebook wanted to scoop up what could have eventually become a big rival.";
  private static final String CNN_MONEY_PARAGRAPH_11 = "Like Instagram, WhatsApp will function as "
      + "an autonomous unit within Facebook, with all the existing employees coming in as part of "
      + "the deal.";
  private static final String CNN_MONEY_PARAGRAPH_12 = "Facebook (FB) said it will pay WhatsApp "
      + "$4 billion in cash and $12 billion in stock. WhatsApp's founders and staff will be "
      + "eligible for for another $3 billion in stock grants to be paid out if they remain "
      + "employed by Facebook for four years. Koum will also join Facebook's board of directors.";

  private static final String CNN_MONEY_PARAGRAPH_HTML = new StringBuilder()
      .append("<div id=\"storytext\">")
      .append("<script type=\"text/javascript\"> ")
      .append("vidConfig.push({ videoArray : [ {id : \"/video/technology/2014/02/19/t-facebook-"
          + "whatsapp-19-billion.cnnmoney\", hed: \"WhatsApp with Facebook\'s $19B offer?\"} ], "
          + "loc : 'top', playerprofile : 'story', playerid : 'cvp_story_0', divid : 'vid0', "
          + "hedtarget : '#cnnplayer0 .cnnHeadline' }); </script> ")
      .append("<div class=\"cnnplayer\" id=\"cnnplayer0\"> ")
      .append("<div class=\"cnnVidplayer\"> ")
      .append("<a class=\"summaryImg\" id=\"vid0\" href=\"/video/technology/2014/02/19/t-"
          + "facebook-whatsapp-19-billion.cnnmoney\" onclick=\"javascript:VideoPlayerManager."
          + "playVideos('cvp_story_0'); return false;\"> ")
      .append("<img src=\"http://i2.cdn.turner.com/money/dam/assets/140219215400-t-facebook-"
          + "whatsapp-19-billion-00000515-1024x576.jpg\" width=\"780\" height=\"439\" "
          + "alt=\"WhatsApp with Facebook's $19B offer?\" border=\"0\" /> ")
      .append("<span class=\"button video-play\"><i class=\"icon icon--type-video\"></i></span> ")
      .append("</a> ")
      .append("</div> ")
      .append("<div class=\"cnnVidFooter\"> ")
      .append("<div class=\"js-vid-hed-cvp_story_0 cnnHeadline\">")
      .append("WhatsApp with Facebook's $19B offer?")
      .append("</div> ")
      .append("<div class=\"js-vid-countdown-cvp_story_0 countdown\"></div> ")
      .append("</div> ")
      .append("</div> ")
      .append("<div class=\"share-tools share-tools--floater\" id=\"js-sharebar-floater\"></div>")
      .append("<h2>" + CNN_MONEY_PARAGRAPH_0 + "</h2> ")
      .append("<p> That's a stunning sum for the five-year old "
          + "company. But WhatsApp has been able to hold its weight against messaging heavyweights "
          + "like <span>Twitter</span> <span>(<span class='inlink_chart'>"
          + "<a href='http://money.cnn.com/quote/quote.html?symb=TWTR&amp;source=story_quote_link' "
          + "class='inlink'>TWTR</a></span>)</span>, <span>Google</span> <span>(<span class="
          + "'inlink_chart'><a href='http://money.cnn.com/quote/quote.html?symb=GOOG&amp;source="
          + "story_quote_link' class='inlink'>GOOG</a></span>)</span> and <span>Microsoft's</span> "
          + "<span>(<span class='inlink_chart'><a href='http://money.cnn.com/quote/quote.html?"
          + "symb=MSFT&amp;source=story_quote_link' class='inlink'>MSFT</a></span>)</span> Skype. "
          + "WhatsApp has upwards of 450 million users, and it is adding an additional million "
          + "users every day. </p> ")
      .append("<div id=\"ie_column\"> <div id=\"ie_column\"> <div id=\"quigo220\">"
          + "<!-- ADSPACE: technology/quigo/ctr.220x200 -->"
          + "<div id=\"ad_ns_btf_03\"></div>"
          + "<!-- ADSPACE-END --></div> </div> </div>")
      .append("<p> " + CNN_MONEY_PARAGRAPH_2 + " </p> ")
      .append("<p> " + CNN_MONEY_PARAGRAPH_3 + " </p> ")
      .append("<p> " + CNN_MONEY_PARAGRAPH_4 + " </p> ")
      .append("<p> <a href='http://money.cnn.com/gallery/technology/social/2014/02/03/facebook-"
          + "changes/'><span class='inStoryHeading'>Related: 5 key moments that changed Facebook"
          + "</span></a> </p> ")
      .append("<p> " + CNN_MONEY_PARAGRAPH_5 + " </p> ")
      .append("<p> " + CNN_MONEY_PARAGRAPH_6 + " </p> ")
      .append("<p> " + CNN_MONEY_PARAGRAPH_7 + " </p> ")
      .append("<p> " + CNN_MONEY_PARAGRAPH_8 + " </p> ")
      .append("<p> " + CNN_MONEY_PARAGRAPH_9 + " </p> ")
      .append("<p> <a href='http://money.cnn.com/2012/04/09/technology/facebook_acquires_"
          + "instagram/'>Facebook bought Instagram</a> for $1 billion in 2012 for similar "
          + "reasons: As young social network users gravitated towards photo-sharing, Facebook "
          + "wanted to scoop up what could have eventually become a big rival. </p> ")
      .append("<p> " + CNN_MONEY_PARAGRAPH_11 + " </p> ")
      .append("<p> <span>Facebook</span> <span>(<span class='inlink_chart'>"
          + "<a href='http://money.cnn.com/quote/quote.html?symb=FB&amp;source=story_quote_link' "
          + "class='inlink'>FB</a></span>)</span> said it will pay WhatsApp $4 billion in cash "
          + "and $12 billion in stock. WhatsApp's founders and staff will be eligible for for "
          + "another $3 billion in stock grants to be paid out if they remain employed by "
          + "Facebook for four years. Koum will also join Facebook's board of directors. "
          + "<a href=\"#TOP\" class=\"story_endoftext\"></a> </p> ")
      .append("<div id=\"storyFooter\"></div> ")
      .append("<div class=\"clearfix\"></div> ")
      .append("<div class=\"storytimestamp\"> ")
      .append("<span class=\"cnnStorySource\"> CNNMoney (New York) </span> ")
      .append("<span class=\"cnnDateStamp\">February 19, 2014: 6:54 PM ET</span> ")
      .append("</div> </div>")
      .toString();

  private static final String SKYSCRAPER_NEWS_PARAGRAPH_0 = "It may sound like the victor of a "
      + "Highlander movie, but The One is actually a new skyscraper proposal from Foster + "
      + "Partners, with the help of local firm Core Architects, that if realised will be the "
      + "tallest building in Canada.";
  private static final String SKYSCRAPER_NEWS_PARAGRAPH_1 = "Proposed for the city of Toronto "
      + "by Mizrahi Development, the scheme sits on a busy intersection in the middle of Toronto "
      + "overlooking Bloor Street and Young Street. The former may be familiar thanks to a number "
      + "of other tall building proposals that have recently surfaced which may one day stand "
      + "along it.";
  private static final String SKYSCRAPER_NEWS_PARAGRAPH_2 = "In this case the tower has a proposed "
      + "height of 318 metres with a total of 80-storeys. At ground level will be the predictable "
      + "retail podium rising to 8 storeys complete with a green wall. A central atrium will cut "
      + "through this part of the building opening it up internally to shoppers. Above the "
      + "remainder will consist of upscale condos.";
  private static final String SKYSCRAPER_NEWS_PARAGRAPH_3 = "The project is dominated by one of "
      + "the architecturalsignatures of Foster + Partners, a structural diagrid of crossbracing, "
      + "which in this case appears to be bronze. This structural expression, which is set over "
      + "floor to ceiling glazing, allows the tower to be recessed back at intervals including the "
      + "ground level where it provides a modern take on a colonnaded entrance, and further up a "
      + "series of sky terraces.";
  private static final String SKYSCRAPER_NEWS_PARAGRAPH_4 = "Proposals are still at a relatively "
      + "early stage, and could be refused by local planning officials, but illustrate that "
      + "Toronto is showing the sort of ambition being displayed in many of the leading cities "
      + "around the world.";
  private static final String SKYSCRAPER_NEWS_PARAGRAPH_HTML = new StringBuilder()
      .append("<html><body><table><tr>")
      .append("<td valign=\"top\" class=\"newsbody1\">")
      .append("<div align=\"justify\">")
      .append("    </div>")
      .append("<div> ")
      .append(SKYSCRAPER_NEWS_PARAGRAPH_0 + "<br />")
      .append("<br />")
      .append(SKYSCRAPER_NEWS_PARAGRAPH_1 + "<br />")
      .append("<br />")
      .append(SKYSCRAPER_NEWS_PARAGRAPH_2 + "<br />")
      .append("<br />")
      .append(SKYSCRAPER_NEWS_PARAGRAPH_3 + "<br />")
      .append("<br />")
      .append(SKYSCRAPER_NEWS_PARAGRAPH_4 + "")
      .append("</div>")
      .append("</td>")
      .append("</tr></table></body></html>")
      .toString();

  @Test
  public void testCnnMoney() throws Exception {
    Document article = Jsoup.parse(
        CNN_MONEY_PARAGRAPH_HTML,
        "http://money.cnn.com/2014/02/19/technology/social/facebook-whatsapp/");
    List<String> paragraphs = ImmutableList.copyOf(ParagraphFinder.getParagraphs(article));
    assertEquals(13, paragraphs.size());
    assertEquals(CNN_MONEY_PARAGRAPH_0, paragraphs.get(0));
    assertEquals(CNN_MONEY_PARAGRAPH_1, paragraphs.get(1));
    assertEquals(CNN_MONEY_PARAGRAPH_2, paragraphs.get(2));
    assertEquals(CNN_MONEY_PARAGRAPH_3, paragraphs.get(3));
    assertEquals(CNN_MONEY_PARAGRAPH_4, paragraphs.get(4));
    assertEquals(CNN_MONEY_PARAGRAPH_5, paragraphs.get(5));
    assertEquals(CNN_MONEY_PARAGRAPH_6, paragraphs.get(6));
    assertEquals(CNN_MONEY_PARAGRAPH_7, paragraphs.get(7));
    assertEquals(CNN_MONEY_PARAGRAPH_8, paragraphs.get(8));
    assertEquals(CNN_MONEY_PARAGRAPH_9, paragraphs.get(9));
    assertEquals(CNN_MONEY_PARAGRAPH_10, paragraphs.get(10));
    assertEquals(CNN_MONEY_PARAGRAPH_11, paragraphs.get(11));
    assertEquals(CNN_MONEY_PARAGRAPH_12, paragraphs.get(12));
  }

  @Test
  public void testSkyscraperNews() throws Exception {
    Document article = Jsoup.parse(
        SKYSCRAPER_NEWS_PARAGRAPH_HTML,
        "http://www.skyscrapernews.com/news.php?ref=3520");
    List<String> paragraphs = ImmutableList.copyOf(ParagraphFinder.getParagraphs(article));
    assertEquals(5, paragraphs.size());
    assertEquals(SKYSCRAPER_NEWS_PARAGRAPH_0, paragraphs.get(0));
    assertEquals(SKYSCRAPER_NEWS_PARAGRAPH_1, paragraphs.get(1));
    assertEquals(SKYSCRAPER_NEWS_PARAGRAPH_2, paragraphs.get(2));
    assertEquals(SKYSCRAPER_NEWS_PARAGRAPH_3, paragraphs.get(3));
    assertEquals(SKYSCRAPER_NEWS_PARAGRAPH_4, paragraphs.get(4));
  }

  @Test
  public void testGetParagraphNodes() throws Exception {
    Document documentNode = Jsoup.parse(
        new File("testdata/abcnews-sunday-on-this-week.html"),
        "UTF-8",
        "http://abcnews.go.com/blogs/politics/2015/01/sunday-on-this-week/");
    Elements paragraphEls = ParagraphFinder.getParagraphEls(documentNode);

    // Yea, you're right, the first two paragraphs shouldn't be here.  But we're
    // basically checking for regressions in parsing here, not absolute
    // correctness.
    assertTrue(paragraphEls.get(0).text().startsWith(
        "AP"));
    assertTrue(paragraphEls.get(1).text().startsWith(
        "The latest breaking details on the AirAsia Flight QZ 8501 disaster"));
    assertTrue(paragraphEls.get(2).text().startsWith(
        "Then, we talk to incoming members of Congress already"));
    assertTrue(paragraphEls.get(3).text().startsWith(
        "Plus, the powerhouse roundtable debates all the"));
  }

  @Test
  public void testNestedParagraphs() throws Exception {
    // This appeared as an issue when parsing:
    // http://www.bizbash.com/march-25-2015-boston-seeks-vote-on-olympics-bid-
    //     major-convention-threatens-to-move-over-indiana-bill-southwest-kicks-
    //     man/toronto/story/30210
    // The paragraphs were all included twice due to the initial empty <p>.
    String html = "<html><body>"
        + "<p>"
        + "<p>Paragraph 1"
        + "<p>"
        + "<p>Paragraph 2"
        + "<p>"
        + "<p>Paragraph 3"
        + "</body></html>";
    SiteManifests.addSiteManifest(SiteManifest.newBuilder()
        .setRootDomain("testsite.com")
        .addParagraphSelector("p")
        .build());
    Document document = Jsoup.parse(html, "http://testsite.com/article");
    Elements paragraphEls = ParagraphFinder.getParagraphEls(document);

    assertEquals(3, paragraphEls.size());
    assertEquals("Paragraph 1", paragraphEls.get(0).text());
    assertEquals("Paragraph 2", paragraphEls.get(1).text());
    assertEquals("Paragraph 3", paragraphEls.get(2).text());
  }
}
