package com.janknspank.crawler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Test;

import com.janknspank.proto.CrawlProto.ContentSite;
import com.janknspank.proto.CrawlProto.TestInstructions.ArticleUrlDetectorChecks;

public class ArticleUrlDetectorTest {
  @Test
  public void test() throws Exception {
    for (ContentSite site : UrlWhitelist.CRAWL_INSTRUCTIONS.getContentSiteList()) {
      assertTrue("Every ContentSite must have test instructions" + site.getRootDomain()
          + " does not.", site.hasTestInstructions());
      assertTrue(
          "Every ContentSite must have test definition checks for Article vs. non-Article URLs",
          site.getTestInstructions().hasArticleUrlDetectorChecks());
      assertTrue("Every ContentSite must have at least 5 Article URLs defined in "
          + "test_instructions.  " + site.getRootDomain() + " does not.",
          site.getTestInstructions().getArticleUrlDetectorChecks().getArticleUrlCount() >= 5);
      assertTrue("Every ContentSite must have at least 5 non-Article URLs defined in "
          + "test_instructions" + site.getRootDomain() + " does not.",
          site.getTestInstructions().getArticleUrlDetectorChecks().getNonArticleUrlCount() >= 5);

      ArticleUrlDetectorChecks checks = site.getTestInstructions().getArticleUrlDetectorChecks();
      for (String articleUrl : checks.getArticleUrlList()) {
        new URL(articleUrl);
        assertFalse(articleUrl.isEmpty());
        assertTrue(
            "For site " + site.getRootDomain() + ", \"" + articleUrl
                + "\" should be recognized as an article",
            ArticleUrlDetector.isArticle(articleUrl));
        assertTrue(
            "For site " + site.getRootDomain() + ", known article URL \"" + articleUrl
                + "\" should not be blacklisted",
            UrlWhitelist.isOkay(articleUrl));
      }

      for (String nonArticleUrl : checks.getNonArticleUrlList()) {
        new URL(nonArticleUrl);
        assertFalse(nonArticleUrl.isEmpty());
        assertFalse(
            "For site " + site.getRootDomain() + ", \"" + nonArticleUrl
                + "\" should not be recognized as an article",
            ArticleUrlDetector.isArticle(nonArticleUrl));
      }
    }
  }
}