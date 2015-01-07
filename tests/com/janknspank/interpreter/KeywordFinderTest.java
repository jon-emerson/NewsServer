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
            + "<meta name=\"keywords\" content=\"BBC, Capital,story,STORY-VIDEO,Office Space\"/>"
            + "<title>Article title</title>"
            + "</head><body>"
            + "<div class=\"cnn_storyarea\"><p>"
            + "Brangelina (also called Bradgelina) is a celebrity supercouple "
            + "consisting of American actors "
            + "<a href=\"http://en.wikipedia.org/wiki/Brad_Pitt\">Brad Pitt</a> and "
            + "<a href=\"http://en.wikipedia.org/wiki/Angelina_Jolie\">Angelina Jolie</a>."
            + "</p></div>"
            + "</body</html>"));
    List<ArticleKeyword> keywords = KeywordFinder.findKeywords("urlId", documentNode);
    assertContainsKeyword("BBC", ArticleKeywords.TYPE_META_TAG, keywords);
    assertContainsKeyword("Capital", ArticleKeywords.TYPE_META_TAG, keywords);
    assertContainsKeyword("Office Space", ArticleKeywords.TYPE_META_TAG, keywords);
    assertContainsKeyword("Brad Pitt", ArticleKeywords.TYPE_HYPERLINK, keywords);
    assertContainsKeyword("Angelina Jolie", ArticleKeywords.TYPE_HYPERLINK, keywords);
  }
}
