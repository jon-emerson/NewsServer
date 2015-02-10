package com.janknspank.utils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;

public class ShareNormalizationTableTest {
  @Test
  public void testGetDomainForArticle() throws Exception {
    assertEquals("nytimes.com", ShareNormalizationTable.getDomainForArticle(
        Article.newBuilder()
            .setUrl("http://www.nytimes.com/2015/02/08/education/edlife/"
                + "is-your-first-grader-college-ready.html")
            .build()));
    assertEquals("sf.curbed.com", ShareNormalizationTable.getDomainForArticle(
        Article.newBuilder()
            .setUrl("http://sf.curbed.com/archives/2015/02/03/did_sfs_accused_"
                + "embezzler_just_relist_his_allegedly_illgotten_home_for_a_profit.php")
            .build()));
  }

  @Test
  public void testGetAgeInMillis() throws Exception {
    final long startMillis = TimeUnit.DAYS.toMillis(1000);
    final long offsetMillis = TimeUnit.DAYS.toMillis(2);
    final Article article = Article.newBuilder()
        .setUrl("http://www.nytimes.com/2015/02/08/education/edlife/"
            + "is-your-first-grader-college-ready.html")
        .setPublishedTime(startMillis)
        .build();
    assertEquals(
        offsetMillis,
        (long) ShareNormalizationTable.getAgeInMillis(
            article,
            SocialEngagement.newBuilder()
                .setCreateTime(startMillis + offsetMillis)
                .build()));

    // Some articles claim they're published in the future - make sure we
    // deal with this elegantly by returning a non-zero age!
    assertTrue(
        ShareNormalizationTable.getAgeInMillis(
            article,
            SocialEngagement.newBuilder()
                .setCreateTime(startMillis - offsetMillis)
                .build()) > 0);
  }
}
