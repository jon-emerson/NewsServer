package com.janknspank.crawler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.nlp.KeywordFinder;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.ArticleKeyword.Source;

public class KeywordFinderTest {
  private static final KeywordFinder KEYWORD_FINDER = KeywordFinder.getInstance();

  private void assertNotContainsKeyword(
      String keywordStr, Iterable<ArticleKeyword> keywords) {
    for (ArticleKeyword keyword : keywords) {
      if (keywordStr.equals(keyword.getKeyword())) {
        fail("Forbidden keyword found: " + keywordStr);
      }
    }
  }

  private void assertContainsKeyword(
      String keywordStr, Source source, Iterable<ArticleKeyword> keywords) {
    for (ArticleKeyword keyword : keywords) {
      if (keywordStr.equals(keyword.getKeyword())) {
        assertEquals("Source for " + keyword.getKeyword() + " should be " + source.name(),
            source, keyword.getSource());
        return;
      }
    }
    fail("Keyword not found: " + keywordStr);
  }

  /**
   * Verifies that we can find keywords in an article.
   */
  @Test
  public void testFindKeywords() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://www.cnn.com/2015/01/08/foo.html",
        new StringReader("<html><head>"
            + "<meta name=\"keywords\" "
            + "    content=\"BBC, Capital,story,Wikipedia,Brangelina,  brad pitt\"/>"
            + "<title>Article title</title>"
            + "</head><body>"
            + "<div class=\"cnn_storyarea\"><p>"
            + "Brangelina (also called Bradgelina) is a celebrity supercouple "
            + "consisting of American actors "
            + "<a href=\"http://en.wikipedia.org/wiki/Brad_Pitt\">Brad Pitt</a> and "
            + "<a href=\"http://en.wikipedia.org/wiki/Angelina_Jolie\">Angelina Jolie</a>. "
            + "Mr. Pitt and Mrs. Jolie love their combined name. The full story is availble "
            + "on wikipedia."
            + "</p></div>"
            + "</body</html>"));
    Iterable<ArticleKeyword> keywords =
        KEYWORD_FINDER.findKeywords(
            "urlId", "Article title", documentNode, ImmutableList.<ArticleFeature>of());

    // While these are in the <meta> tag, they do not exist in the document,
    // so we filter them.
    assertNotContainsKeyword("BBC", keywords);
    assertNotContainsKeyword("Capital", keywords);

    // While "story" is in the meta tag, and it exists in the article body, it's
    // blacklisted for being too general, so it should not be a keyword.
    assertNotContainsKeyword("story", keywords);

    // These are the good ones!
    assertEquals(4, Iterables.size(keywords));
    assertContainsKeyword("Brangelina", Source.META_TAG, keywords);
    assertContainsKeyword("Wikipedia", Source.META_TAG, keywords);
    assertContainsKeyword("Brad Pitt", Source.TITLE, keywords);
    assertContainsKeyword("Angelina Jolie", Source.TITLE, keywords);
  }
}
