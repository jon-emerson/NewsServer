package com.janknspank.utils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.api.client.util.Lists;
import java.util.concurrent.TimeUnit;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.utils.ShareNormalizationTableBuilder.ShareNormalizationTable;

public class ShareNormalizationTableBuilderTest {
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
        (long) ShareNormalizationTableBuilder.getAgeInMillis(
            article,
            SocialEngagement.newBuilder()
                .setCreateTime(startMillis + offsetMillis)
                .build()));

    // Some articles claim they're published in the future - make sure we
    // deal with this elegantly by returning a non-zero age!
    assertTrue(
        ShareNormalizationTableBuilder.getAgeInMillis(
            article,
            SocialEngagement.newBuilder()
                .setCreateTime(startMillis - offsetMillis)
                .build()) > 0);
  }

  @Test
  public void testBucketing() throws Exception {
    final List<Article> articles = Lists.newArrayList();
    final String domain = "nytimes.com";
    final long startMillis = TimeUnit.DAYS.toMillis(1013);
    final long sharesPerArticle = 13;
    long[] offsetMillisArray = new long[] {
        -TimeUnit.HOURS.toMillis(3),     // 3 hours before "publish date" (liars!:).
        TimeUnit.HOURS.toMillis(1),          // 1 hour.
        TimeUnit.HOURS.toMillis(12),     // 12 hours.
        TimeUnit.HOURS.toMillis(36), // 1 1/2 days.
        TimeUnit.DAYS.toMillis(4), // 4 days.
        TimeUnit.DAYS.toMillis(5), // 5 days.
        TimeUnit.DAYS.toMillis(25) // 25 days.
    };
    for (Long offsetMillis : offsetMillisArray) {
      articles.add(Article.newBuilder()
          .setUrl("http://" + domain + "/published-" + offsetMillis + "-ago.html")
          .setPublishedTime(startMillis)
          .addSocialEngagement(SocialEngagement.newBuilder()
              .setShareCount(sharesPerArticle)
              .setSite(Site.FACEBOOK)
              .setCreateTime(startMillis + offsetMillis))
          .build());
    }
    ShareNormalizationTable table = new ShareNormalizationTable(articles);
    assertEquals(2, table.articlesInThreeHours.count(domain));
    assertEquals(2 * sharesPerArticle, table.sharesInThreeHours.count(domain));
    assertEquals(3, table.articlesInOneDay.count(domain));
    assertEquals(3 * sharesPerArticle, table.sharesInOneDay.count(domain));
    assertEquals(4, table.articlesInThreeDays.count(domain));
    assertEquals(4 * sharesPerArticle, table.sharesInThreeDays.count(domain));
    assertEquals(6, table.articlesInSevenDays.count(domain));
    assertEquals(6 * sharesPerArticle, table.sharesInSevenDays.count(domain));
    assertEquals(offsetMillisArray.length, table.articlesAllTime.count(domain));
    assertEquals(offsetMillisArray.length * sharesPerArticle, table.sharesAllTime.count(domain));
  }
}
