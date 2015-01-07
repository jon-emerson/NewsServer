package com.janknspank.dom.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class SelectorTest {
  @Test
  public void testTokenizeDefinitionStack() throws Exception {
    List<String> tokens = Selector.tokenizeDefinitionStack("html body div.article-body p");
    assertEquals(4, tokens.size());
    assertEquals("html", tokens.get(0));
    assertEquals("body", tokens.get(1));
    assertEquals("div.article-body", tokens.get(2));
    assertEquals("p", tokens.get(3));

    tokens = Selector.tokenizeDefinitionStack("div[href=\" .moo#hello[].class#id");
    assertEquals(1, tokens.size());
    assertEquals("div[href=\" .moo#hello[].class#id", tokens.get(0));

    tokens = Selector.tokenizeDefinitionStack("div#id  * a.class[href][target = \"#he' llo\"]>b");
    assertEquals(5, tokens.size());
    assertEquals("div#id", tokens.get(0));
    assertEquals("*", tokens.get(1));
    assertEquals("a.class[href][target = \"#he' llo\"]", tokens.get(2));
    assertEquals(">", tokens.get(3));
    assertEquals("b", tokens.get(4));

    tokens = Selector.tokenizeDefinitionStack("a >[x=y] [z=\" .yes\"][ t = 'f' ] .b> #c  [href] [z1 ][ z2]");
    assertEquals(9, tokens.size());
    assertEquals("a", tokens.get(0));
    assertEquals(">", tokens.get(1));
    assertEquals("[x=y]", tokens.get(2));
    assertEquals("[z=\" .yes\"][ t = 'f' ]", tokens.get(3));
    assertEquals(".b", tokens.get(4));
    assertEquals(">", tokens.get(5));
    assertEquals("#c", tokens.get(6));
    assertEquals("[href]", tokens.get(7));
    assertEquals("[z1 ][ z2]", tokens.get(8));
  }

  @Test
  public void testTokenizeDefinition() {
    List<String> tokens = Selector.tokenizeDefinition(
        "a.class#id[href][target=\"_blank\"][value=\" [] \"]");
    assertEquals(6, tokens.size());
    assertEquals("a", tokens.get(0));
    assertEquals(".class", tokens.get(1));
    assertEquals("#id", tokens.get(2));
    assertEquals("[href]", tokens.get(3));
    assertEquals("[target=\"_blank\"]", tokens.get(4));
    assertEquals("[value=\" [] \"]", tokens.get(5));
  }

  //@Test
  public void XtestParseSelectors() {
    List<Selector> selectors = Selector.parseSelectors(
        "a >[x=y] [z=\" .yes\"][ t = 'f' ] .b> #c  [href] [z1 ][ z2]");
    assertEquals(7, selectors.size());

    // a
    assertEquals("a", selectors.get(0).tagName);
    assertTrue(selectors.get(0).classes.isEmpty());
    assertNull(selectors.get(0).id);
    assertTrue(selectors.get(0).attributes.isEmpty());
    assertTrue(selectors.get(0).attributeValues.isEmpty());
    assertFalse(selectors.get(0).isDirectDescendant());

    // *
    assertEquals("*", selectors.get(0).tagName);
    assertTrue(selectors.get(0).classes.isEmpty());
    assertNull(selectors.get(0).id);
    assertTrue(selectors.get(0).attributes.isEmpty());
    assertTrue(selectors.get(0).attributeValues.isEmpty());
    assertFalse(selectors.get(0).isDirectDescendant());

    // >[x=y]
    assertNull(selectors.get(1).tagName);
    assertTrue(selectors.get(1).classes.isEmpty());
    assertNull(selectors.get(1).id);
    assertTrue(selectors.get(1).attributes.isEmpty());
    assertEquals(1, selectors.get(1).attributeValues.size());
    assertEquals("y", selectors.get(1).attributeValues.get("x"));
    assertTrue(selectors.get(1).isDirectDescendant());

    // [z=\" .yes\"][ t = 'f' ]
    assertNull(selectors.get(2).tagName);
    assertTrue(selectors.get(2).classes.isEmpty());
    assertNull(selectors.get(2).id);
    assertTrue(selectors.get(2).attributes.isEmpty());
    assertEquals(2, selectors.get(2).attributeValues.size());
    assertEquals(" .yes", selectors.get(2).attributeValues.get("z"));
    assertEquals("f", selectors.get(2).attributeValues.get("t"));
    assertFalse(selectors.get(2).isDirectDescendant());

    // .b
    assertNull(selectors.get(3).tagName);
    assertEquals(1, selectors.get(3).classes.size());
    assertTrue(selectors.get(3).classes.contains("b"));
    assertNull(selectors.get(3).id);
    assertTrue(selectors.get(3).attributes.isEmpty());
    assertTrue(selectors.get(3).attributeValues.isEmpty());
    assertFalse(selectors.get(3).isDirectDescendant());

    // > #c
    assertNull(selectors.get(4).tagName);
    assertTrue(selectors.get(4).classes.isEmpty());
    assertNull(selectors.get(4).id);
    assertTrue(selectors.get(4).attributes.isEmpty());
    assertTrue(selectors.get(4).attributeValues.isEmpty());
    assertTrue(selectors.get(4).isDirectDescendant());

    // [href]
    assertNull(selectors.get(5).tagName);
    assertTrue(selectors.get(5).classes.isEmpty());
    assertNull(selectors.get(5).id);
    assertEquals(1, selectors.get(5).attributes.size());
    assertTrue(selectors.get(5).attributes.contains("href"));
    assertTrue(selectors.get(5).attributeValues.isEmpty());
    assertFalse(selectors.get(5).isDirectDescendant());

    // [z1 ][ z2]
    assertNull(selectors.get(6).tagName);
    assertTrue(selectors.get(6).classes.isEmpty());
    assertNull(selectors.get(6).id);
    assertEquals(2, selectors.get(6).attributes.size());
    assertTrue(selectors.get(6).attributes.contains("z1"));
    assertTrue(selectors.get(6).attributes.contains("z2"));
    assertTrue(selectors.get(6).attributeValues.isEmpty());
    assertFalse(selectors.get(6).isDirectDescendant());
  }

  @Test
  public void testMatches() {
    Selector selector = new Selector("div");
    Node node = new Node(null, "div", 0);
    assertTrue(selector.matches(node));

    selector = new Selector("a[href]");
    node = new Node(null, "a", 0);
    assertFalse(selector.matches(node));
    node.addAttribute("href", "http://www.cnn.com/");
    assertTrue(selector.matches(node));
    node.addAttribute("target", "_blank");
    assertTrue(selector.matches(node));

    selector = new Selector("a[t=f]");
    node = new Node(null, "a", 0);
    assertFalse(selector.matches(node));
    node.addAttribute("t", "f");
    assertTrue(selector.matches(node));

    selector = new Selector("a#foo");
    node = new Node(null, "a", 0);
    assertFalse(selector.matches(node));
    node.addAttribute("id", "foo");
    assertTrue(selector.matches(node));

    selector = new Selector("a[t=f][z=\"[ .yes\"]#id.class1.class2");
    node = new Node(null, "a", 0);
    assertFalse(selector.matches(node));
    node.addAttribute("t", "f");
    assertFalse(selector.matches(node));
    node.addAttribute("z", "[ .yes");
    assertFalse(selector.matches(node));
    node.addAttribute("id", "id");
    assertFalse(selector.matches(node));
    node.addAttribute("class", "class1  class2");
    assertTrue(selector.matches(node));
  }
}
