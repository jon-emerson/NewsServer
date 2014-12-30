package com.janknspank;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.Url;

public class ArticleHandlerTest {
  private static class TestArticleCallback implements ArticleHandler.ArticleCallback {
    List<String> foundUrls = Lists.newArrayList();
    List<Article> foundArticles = Lists.newArrayList();

    @Override
    public void foundUrl(String url) {
      foundUrls.add(url);
    }

    @Override
    public void foundArticle(Article article) {
      foundArticles.add(article);
    }
  }

  /**
   * Verifies that we pull dates out of article URLs.
   */
  @Test
  public void testFindDateInUrl() throws Exception {
    Url url = Url.newBuilder()
        .setUrl("http://www.cnn.com/2014/05/24/hello.html")
        .setId("test id")
        .setTweetCount(0)
        .setDiscoveryTime(500L)
        .build();
    ArticleHandler handler = new ArticleHandler(new TestArticleCallback(), url);

    // Pull the published time from the Handler and see what date it has set.
    Calendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(handler.articleBuilder.getPublishedTime());
    assertEquals(2014, calendar.get(Calendar.YEAR));
    assertEquals(4, calendar.get(Calendar.MONTH)); // 0-based months.
    assertEquals(24, calendar.get(Calendar.DAY_OF_MONTH));
  }
}
