package com.janknspank.crawler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.janknspank.crawler.UrlWhitelist;

public class UrlWhitelistTest {
  @Test
  public void testIsOkayBbc() throws Exception {
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.com/news/world-asia-30573040"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.co.uk/news/10725415"));
    assertTrue(UrlWhitelist.isOkay("http://bbc.co.uk/news/10725415"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.com/future/story/20140629-how-pickpockets-trick-your-mind"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.com/culture/story/20141223-the-10-best-books-of-2014"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.com/earth/story/20141210-astounding-microscope-images"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.com/capital/story/20141113-surprising-career-killing-habits"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.com/earth/story/20141117-why-seals-have-sex-with-penguins"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.com/autos/story/20141223-most-fascinating-green-car-of-2014-local-motors-strati"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.co.uk/capital/story/20141113-surprising-career-killing-habits"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.co.uk/earth/story/20141117-why-seals-have-sex-with-penguins"));
    assertTrue(UrlWhitelist.isOkay("http://www.bbc.co.uk/autos/story/20141223-most-fascinating-green-car-of-2014-local-motors-strati"));

    assertFalse(UrlWhitelist.isOkay("http://bbc.co.uk/"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/news/"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/news10725415"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/news/0"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/webwise/0/22717887"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.com/news/world-asia-30573040/"));
    assertFalse(UrlWhitelist.isOkay("http://newsvote.bbc.co.uk/go/news/int/story/services/-/email/news"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/iplayer/episode/b00wv8gy/horrible-histories-horrible-christmas"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/guides/zttpn39"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.com/sport/0/30603067"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/iplayer/categories/comedy/highlights"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.com/travel/slideshow/20141211-father-christmas-mediterranean-island-escape"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/programmes/articles/s790szKLSc1l3n9Fqwxl9F/6-musics-albums-of-the-year-2014"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/portuguese/noticias/2014/12/141226_brasileira_tailandia_tsunami_gerardo_rw"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.co.uk/mundo/noticias/2014/12/141119_cosas_sorprendentes_finde2014_yv"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.com/programmes/articles/s790szKLSc1l3n9Fqwxl9F/6-musics-albums-of-the-year-2014"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.com/portuguese/noticias/2014/12/141226_brasileira_tailandia_tsunami_gerardo_rw"));
    assertFalse(UrlWhitelist.isOkay("http://www.bbc.com/mundo/noticias/2014/12/141119_cosas_sorprendentes_finde2014_yv"));
    assertFalse(UrlWhitelist.isOkay("http://eatocracy.cnn.com/2010/06/18/are-you-still-working-on-that/comment-page-9/"));
    assertFalse(UrlWhitelist.isOkay("http://www.abc.net.au/news/2015-01-08/image/6006554"));
    assertFalse(UrlWhitelist.isOkay("http://www.forbes.com/sites/nickmorgan/2015/01/08/which-is-more-important-authenticity-or-charisma/2/"));
  }

  @Test
  public void testIsOkayNytimes() throws Exception {
    assertTrue(UrlWhitelist.isOkay("http://www.nytimes.com/2014/12/27/your-money/what-annie-can-tell-us-about-money.html"));
    assertTrue(UrlWhitelist.isOkay("http://www.nytimes.com/2014/12/26/your-money/affordable-care-acts-tax-effects-now-loom-for-filers.html"));
    assertTrue(UrlWhitelist.isOkay("http://www.nytimes.com/2014/12/26/opinion/david-brooks-the-sidney-awards-part-i.html"));
    assertTrue(UrlWhitelist.isOkay("http://www.nytimes.com/2014/12/21/business/corner-office-robert-reid-of-intacct-the-culture-always-comes-first.html"));
    assertTrue(UrlWhitelist.isOkay("http://www.nytimes.com/2004/03/07/magazine/our-correspondent-in-cape-town-south-africa-geology-is-destiny.html"));
    assertTrue(UrlWhitelist.isOkay("http://www.nytimes.com/2014/12/28/realestate/selling-condos-with-holograms.html"));
    assertTrue(UrlWhitelist.isOkay("http://dealbook.nytimes.com/2014/12/25/unsolved-shooting-accentuates-problems-at-doral-one-of-puerto-ricos-biggest-lenders/"));
    assertTrue(UrlWhitelist.isOkay("http://www.nytimes.com/2004/04/30/business/the-google-ipo-wall-street-an-egalitarian-auction-bankers-are-not-amused.html"));
    assertTrue(UrlWhitelist.isOkay("http://nytimes.com/2014/12/27/your-money/what-annie-can-tell-us-about-money.html"));
    assertTrue(UrlWhitelist.isOkay("http://nytimes.com/2014/12/26/your-money/affordable-care-acts-tax-effects-now-loom-for-filers.html"));
    assertTrue(UrlWhitelist.isOkay("http://nytimes.com/2014/12/26/opinion/david-brooks-the-sidney-awards-part-i.html"));
    assertTrue(UrlWhitelist.isOkay("http://www.nytimes.com/1992/11/04/us/1992-elections-president-overview-clinton-captures-presidency-with-huge.html"));
    assertTrue(UrlWhitelist.isOkay("http://www.nytimes.com/1865/04/15/news/president-lincoln-shot-assassin-deed-done-ford-s-theatre-last-night-act.html"));

    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/pages/business/index.html"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/pages/opinion/index.html"));
    assertFalse(UrlWhitelist.isOkay("http://jobmarket.nytimes.com/pages/jobs/index.html"));
    assertFalse(UrlWhitelist.isOkay("http://publiceditor.blogs.nytimes.com/2014/12/22/sony-emails-free-speech-and-trying-a-little-f2f/"));
    assertFalse(UrlWhitelist.isOkay("http://autos.nytimes.com/15229-19UUA8F59CA030453/Acura/TL/listingOverview.aspx"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/times-journeys/travel/follow-silk-route-uzbekistan-turkmenistan/terms/"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/times-journeys/travel/colorful-spirit-columbia/"));
    assertFalse(UrlWhitelist.isOkay("http://homedelivery.nytimes.com"));
    assertFalse(UrlWhitelist.isOkay("http://mobile.nytimes.com"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/content/help/rights/copyright/copyright-notice.html"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/rss"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/services/xml/rss/nyt/index.opml"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/ref/multimedia/podcasts.html"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/adx/bin/adx_click.html"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/content/help/contact/directory.html"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/register"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/slideshow/2014/12/14/fashion/NOTEWORTHY-STYLE-2014.html"));
    assertFalse(UrlWhitelist.isOkay("http://www.nytimes.com/subscriptions/Multiproduct/lp5558.html"));
  }
}
