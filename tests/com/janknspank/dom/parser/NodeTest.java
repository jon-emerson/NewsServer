package com.janknspank.dom.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.List;

import org.junit.Test;

public class NodeTest {
  private static final DocumentNode DOCUMENT_NODE;
  static {
    try {
      DOCUMENT_NODE = DocumentBuilder.build(
          "http://www.nytimes.com/2015/01/07/sports/baseball/randy-johnson-pedro-"
              + "martinez-john-smoltz-and-craig-biggio-to-enter-hall-of-fame.html",
          new StringReader(
              "<html>"
              + "<head>"
              + "<meta name=\"ptime\" content=\"20150106140535\">"  // NOTE: improperly terminated.
              + "<meta property=\"og:title\" content=\"Hall of Fame Class "
              + "Includes Randy Johnson, Pedro Martinez, John Smoltz and Craig Biggio\" />"
              + "<meta property=\"og:type\" content=\"article\" />"
              + "</head>"
              + "<body><div class=\"article-body\">"
              + "<p>Hello World</p>"
              + "<p>Paragraph 2</p>"
              + "<div class=\"nested\"><p>Nested Paragraph</p><div>"
              + "</div></body>"
              + "</html>"));
    } catch (ParserException e) {
      throw new Error(e);
    }
  }

  @Test
  public void testFindAll() throws Exception {
    // Find all the meta tags, including the nested ones!
    for (String metaNodeSpecifier : new String[] {
        "meta",
        "html head meta",
        "head meta",
        "html meta"}) {
      List<Node> metaNodes = DOCUMENT_NODE.findAll(metaNodeSpecifier);
      assertEquals("Specifier \"" + metaNodeSpecifier + "\" should find 3 meta tags",
          3, metaNodes.size());
      assertEquals("ptime", metaNodes.get(0).getAttributeValue("name"));
      assertEquals("og:title", metaNodes.get(1).getAttributeValue("property"));
      assertEquals("og:type", metaNodes.get(2).getAttributeValue("property"));
    }

    // Find the meta tags where they're supposed to be - on <head>!
    // (Note that the HTML we're parsing is deliberately malformed, so this
    // should really return the limited set.)
    for (String metaNodeSpecifier : new String[] {
        "html>head>meta",
        "html head > meta",
        "head >meta"}) {
      List<Node> metaNodes = DOCUMENT_NODE.findAll(metaNodeSpecifier);
      assertEquals("Specifier \"" + metaNodeSpecifier + "\" should find 1 meta tag",
          1, metaNodes.size());
      assertEquals("ptime", metaNodes.get(0).getAttributeValue("name"));
    }

    // Test direct descendency.
    assertTrue(DOCUMENT_NODE.findAll("html > meta").isEmpty());

    // Find all the paragraphs.
    List<Node> paragraphNodes = DOCUMENT_NODE.findAll(".article-body p");
    assertEquals(3, paragraphNodes.size());
    assertEquals("Hello World", paragraphNodes.get(0).getFlattenedText());
    assertEquals("Paragraph 2", paragraphNodes.get(1).getFlattenedText());
    assertEquals("Nested Paragraph", paragraphNodes.get(2).getFlattenedText());

    // Find all the paragraphs directly on the element with class "article-body".
    List<Node> directParagraphNodes = DOCUMENT_NODE.findAll(".article-body > p");
    assertEquals(2, directParagraphNodes.size());
    assertEquals("Hello World", directParagraphNodes.get(0).getFlattenedText());
    assertEquals("Paragraph 2", directParagraphNodes.get(1).getFlattenedText());
  }

  @Test
  public void testFindFirst() throws Exception {
    Node titleNode = DOCUMENT_NODE.findFirst("meta[property=og:title]");
    assertEquals("og:title", titleNode.getAttributeValue("property"));
    assertTrue(titleNode.getAttributeValue("content").startsWith("Hall of Fame "));
  }
}
