package com.janknspank;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NewsSiteWhitelistTest {
  @Test
  public void testIsOkayBbc() throws Exception {
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.com/news/world-asia-30573040"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/news/10725415"));
    assertTrue(NewsSiteWhitelist.isOkay("http://bbc.co.uk/news/10725415"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.com/future/story/20140629-how-pickpockets-trick-your-mind"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.com/culture/story/20141223-the-10-best-books-of-2014"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.com/earth/story/20141210-astounding-microscope-images"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.com/capital/story/20141113-surprising-career-killing-habits"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.com/earth/story/20141117-why-seals-have-sex-with-penguins"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.com/autos/story/20141223-most-fascinating-green-car-of-2014-local-motors-strati"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/capital/story/20141113-surprising-career-killing-habits"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/earth/story/20141117-why-seals-have-sex-with-penguins"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/autos/story/20141223-most-fascinating-green-car-of-2014-local-motors-strati"));

    assertFalse(NewsSiteWhitelist.isOkay("http://bbc.co.uk/"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/news/"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/news10725415"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/news/0"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/webwise/0/22717887"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.com/news/world-asia-30573040/"));
    assertFalse(NewsSiteWhitelist.isOkay("http://newsvote.bbc.co.uk/go/news/int/story/services/-/email/news"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/iplayer/episode/b00wv8gy/horrible-histories-horrible-christmas"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/guides/zttpn39"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.com/sport/0/30603067"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/iplayer/categories/comedy/highlights"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.com/travel/slideshow/20141211-father-christmas-mediterranean-island-escape"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/programmes/articles/s790szKLSc1l3n9Fqwxl9F/6-musics-albums-of-the-year-2014"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/portuguese/noticias/2014/12/141226_brasileira_tailandia_tsunami_gerardo_rw"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.co.uk/mundo/noticias/2014/12/141119_cosas_sorprendentes_finde2014_yv"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.com/programmes/articles/s790szKLSc1l3n9Fqwxl9F/6-musics-albums-of-the-year-2014"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.com/portuguese/noticias/2014/12/141226_brasileira_tailandia_tsunami_gerardo_rw"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.bbc.com/mundo/noticias/2014/12/141119_cosas_sorprendentes_finde2014_yv"));
  }

  @Test
  public void testIsOkayNytimes() throws Exception {
    assertTrue(NewsSiteWhitelist.isOkay("http://www.nytimes.com/2014/12/27/your-money/what-annie-can-tell-us-about-money.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.nytimes.com/2014/12/26/your-money/affordable-care-acts-tax-effects-now-loom-for-filers.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.nytimes.com/2014/12/26/opinion/david-brooks-the-sidney-awards-part-i.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.nytimes.com/2014/12/21/business/corner-office-robert-reid-of-intacct-the-culture-always-comes-first.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.nytimes.com/2004/03/07/magazine/our-correspondent-in-cape-town-south-africa-geology-is-destiny.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.nytimes.com/2014/12/28/realestate/selling-condos-with-holograms.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://dealbook.nytimes.com/2014/12/25/unsolved-shooting-accentuates-problems-at-doral-one-of-puerto-ricos-biggest-lenders/"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.nytimes.com/2004/04/30/business/the-google-ipo-wall-street-an-egalitarian-auction-bankers-are-not-amused.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://nytimes.com/2014/12/27/your-money/what-annie-can-tell-us-about-money.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://nytimes.com/2014/12/26/your-money/affordable-care-acts-tax-effects-now-loom-for-filers.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://nytimes.com/2014/12/26/opinion/david-brooks-the-sidney-awards-part-i.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.nytimes.com/1992/11/04/us/1992-elections-president-overview-clinton-captures-presidency-with-huge.html"));
    assertTrue(NewsSiteWhitelist.isOkay("http://www.nytimes.com/1865/04/15/news/president-lincoln-shot-assassin-deed-done-ford-s-theatre-last-night-act.html"));

    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/pages/business/index.html"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/pages/opinion/index.html"));
    assertFalse(NewsSiteWhitelist.isOkay("http://jobmarket.nytimes.com/pages/jobs/index.html"));
    assertFalse(NewsSiteWhitelist.isOkay("http://publiceditor.blogs.nytimes.com/2014/12/22/sony-emails-free-speech-and-trying-a-little-f2f/"));
    assertFalse(NewsSiteWhitelist.isOkay("http://autos.nytimes.com/15229-19UUA8F59CA030453/Acura/TL/listingOverview.aspx"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/times-journeys/travel/follow-silk-route-uzbekistan-turkmenistan/terms/"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/times-journeys/travel/colorful-spirit-columbia/"));
    assertFalse(NewsSiteWhitelist.isOkay("http://homedelivery.nytimes.com"));
    assertFalse(NewsSiteWhitelist.isOkay("http://mobile.nytimes.com"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/content/help/rights/copyright/copyright-notice.html"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/rss"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/services/xml/rss/nyt/index.opml"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/ref/multimedia/podcasts.html"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/adx/bin/adx_click.html"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/content/help/contact/directory.html"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/register"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/slideshow/2014/12/14/fashion/NOTEWORTHY-STYLE-2014.html"));
    assertFalse(NewsSiteWhitelist.isOkay("http://www.nytimes.com/subscriptions/Multiproduct/lp5558.html"));
  }
}
