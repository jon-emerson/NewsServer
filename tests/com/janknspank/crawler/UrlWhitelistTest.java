package com.janknspank.crawler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.janknspank.proto.CrawlProto.ContentSite;

public class UrlWhitelistTest {
  @Test
  public void test() throws Exception {
    for (ContentSite site : UrlWhitelist.CRAWL_INSTRUCTIONS.getContentSiteList()) {
      if (!site.hasTestInstructions()) {
        // TODO(jonemerson): Throw!  Every site should have test instructions!
        continue;
      }

      if (!site.getTestInstructions().hasUrlWhitelistChecks()) {
        // We'll allow these to be skipped, because some sites might have no
        // blacklisted URLs.  But we'll want to require article vs. non-article
        // tests to be filled in.
        continue;
      }

      for (String goodUrl : site.getTestInstructions().getUrlWhitelistChecks().getGoodUrlList()) {
        assertTrue(
            "For site " + site.getRootDomain() + ", \"" + goodUrl + "\" should be a valid URL",
            UrlWhitelist.isOkay(goodUrl));
      }

      for (String badUrl : site.getTestInstructions().getUrlWhitelistChecks().getBadUrlList()) {
        assertFalse(
            "For site " + site.getRootDomain() + ", \"" + badUrl + "\" should be an invalid URL",
            UrlWhitelist.isOkay(badUrl));
      }
    }
  }
}
