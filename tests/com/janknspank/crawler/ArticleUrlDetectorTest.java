package com.janknspank.crawler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Test;

import com.janknspank.proto.CrawlerProto.SiteManifest;
import com.janknspank.proto.CrawlerProto.TestInstructions.ArticleUrlDetectorChecks;

public class ArticleUrlDetectorTest {
  @Test
  public void test() throws Exception {
    for (SiteManifest site : SiteManifests.getList()) {
      ArticleUrlDetectorChecks checks = site.getTestInstructions().getArticleUrlDetectorChecks();
      for (String startUrl : site.getStartUrlList()) {
        assertFalse("Start URLs cannot be articles: " + startUrl,
            ArticleUrlDetector.isArticle(startUrl));
      }

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
