package com.janknspank.dom;

import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.util.List;

import org.junit.Test;

import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.interpreter.SiteParser;

/**
 * Tests for the site parser.
 */
public class SiteParserTest {
  @Test
  public void testGetParagraphNodes() throws Exception {
    DocumentNode documentNode = DocumentBuilder.build(
        "http://abcnews.go.com/blogs/politics/2015/01/sunday-on-this-week/",
        new FileReader("testdata/abcnews-sunday-on-this-week.html"));
    List<Node> paragraphNodes = SiteParser.getParagraphNodes(documentNode);

    // Yea, you're right, the first two paragraphs shouldn't be here.  But we're
    // basically checking for regressions in parsing here, not absolute
    // correctness.
    assertTrue(paragraphNodes.get(0).getFlattenedText().startsWith(
        "AP"));
    assertTrue(paragraphNodes.get(1).getFlattenedText().startsWith(
        "The latest breaking details on the AirAsia Flight QZ 8501 disaster"));
    assertTrue(paragraphNodes.get(2).getFlattenedText().startsWith(
        "Then, we talk to incoming members of Congress already"));
    assertTrue(paragraphNodes.get(3).getFlattenedText().startsWith(
        "Plus, the powerhouse roundtable debates all the"));
  }
}
