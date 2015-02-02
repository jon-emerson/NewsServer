package com.janknspank.interpreter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.InterpretedData;
import com.janknspank.proto.CoreProto.Url;

public class InterpreterTest {
  /**
   * This is an end-to-end test for crawling a NYTimes article.  This test is
   * interesting because NYTimes has a paywall that requires a cookie exchange
   * before they let our fetcher through.  It's also interesting to just make
   * sure our interpreter works. :)
   */
  @Test
  public void testNyTimes() throws Exception {
    InterpretedData data = Interpreter.interpret(Url.newBuilder()
        .setUrl("http://www.nytimes.com/2015/02/02/sports/"
            + "football/katy-perry-a-pop-champion-of-her-times-at-the-super-bowl.html")
        .build());

    Article article = data.getArticle();
    assertEquals("Katy Perry Shines at the Super Bowl", article.getTitle());
    assertEquals("Given the agonizing over air pressure that has racked the N.F.L. in "
        + "recent weeks, it was appropriate that the halftime entertainment at Super Bowl "
        + "XLIX was Katy Perry, a queen of our deflated pop times.",
        article.getParagraph(0));
  }
}
