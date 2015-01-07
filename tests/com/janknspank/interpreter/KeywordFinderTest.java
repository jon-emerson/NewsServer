package com.janknspank.interpreter;

import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import com.janknspank.data.ArticleKeywords;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.proto.Core.ArticleKeyword;

public class KeywordFinderTest {
  private void assertNotContainsKeyword(
      String keywordStr, List<ArticleKeyword> keywords) {
    for (ArticleKeyword keyword : keywords) {
      if (keywordStr.equals(keyword.getKeyword())) {
        fail("Forbidden keyword found: " + keywordStr);
      }
    }
  }

  private void assertContainsKeyword(
      String keywordStr, String type, List<ArticleKeyword> keywords) {
    for (ArticleKeyword keyword : keywords) {
      if (keywordStr.equals(keyword.getKeyword()) && type.equals(keyword.getType())) {
        return;
      }
    }
    fail("Keyword not found: " + keywordStr + " (type=" + type + ")");
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
            + "    content=\"BBC, Capital,story,Wikipedia,Brangelina,  Brad Pitt\"/>"
            + "<title>Article title</title>"
            + "</head><body>"
            + "<div class=\"cnn_storyarea\"><p>"
            + "Brangelina (also called Bradgelina) is a celebrity supercouple "
            + "consisting of American actors "
            + "<a href=\"http://en.wikipedia.org/wiki/Brad_Pitt\">Brad Pitt</a> and "
            + "<a href=\"http://en.wikipedia.org/wiki/Angelina_Jolie\">Angelina Jolie</a>. "
            + "The full story is availble on wikipedia."
            + "</p></div>"
            + "</body</html>"));
    List<ArticleKeyword> keywords = KeywordFinder.findKeywords("urlId", documentNode);

    // While these are in the <meta> tag, they do not exist in the document,
    // so we filter them.
    assertNotContainsKeyword("BBC", keywords);
    assertNotContainsKeyword("Capital", keywords);

    // While "story" is in the meta tag, and it exists in the article body, it's
    // blacklisted for being too general, so it should not be a keyword.
    assertNotContainsKeyword("story", keywords);

    // These are the good ones!
    assertContainsKeyword("Brangelina", ArticleKeywords.TYPE_META_TAG, keywords);
    assertContainsKeyword("Brad Pitt", ArticleKeywords.TYPE_META_TAG, keywords);
    assertContainsKeyword("Wikipedia", ArticleKeywords.TYPE_META_TAG, keywords);
    assertContainsKeyword("Brad Pitt", ArticleKeywords.TYPE_HYPERLINK, keywords);
    assertContainsKeyword("Angelina Jolie", ArticleKeywords.TYPE_HYPERLINK, keywords);
  }
}
