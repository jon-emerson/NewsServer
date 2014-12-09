package com.janknspank.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import com.janknspank.dom.LenientSaxParser.LenientElementInterpreter;

/**
 * Tests for the lenient SAX parser.
 */
public class LenientSaxParserTest {
  @Test
  public void testLenientElementInterpreter() throws Exception {
    LenientElementInterpreter interpreter = new LenientElementInterpreter("<div>");
    assertEquals("div", interpreter.getTag());
    assertEquals(0, interpreter.getAttributes().getLength());
    assertFalse(interpreter.isSelfClosing());

    interpreter = new LenientElementInterpreter("<span class=\"test\">");
    assertEquals("span", interpreter.getTag());
    assertEquals(1, interpreter.getAttributes().getLength());
    assertEquals("test", interpreter.getAttributes().getValue("class"));
    assertFalse(interpreter.isSelfClosing());

    interpreter = new LenientElementInterpreter(
        "<img src='image.jpg' width=500 height=600 title=\"Monster!\"/>");
    assertEquals("img", interpreter.getTag());
    assertEquals(4, interpreter.getAttributes().getLength());
    assertEquals("image.jpg", interpreter.getAttributes().getValue("src"));
    assertEquals("500", interpreter.getAttributes().getValue("width"));
    assertEquals("600", interpreter.getAttributes().getValue("height"));
    assertEquals("Monster!", interpreter.getAttributes().getValue("title"));
    assertTrue(interpreter.isSelfClosing());

    // This is a weird case... Let's just make sure we do something reasonable.
    interpreter = new LenientElementInterpreter("<span clas\"/>");
    assertEquals("span", interpreter.getTag());
    assertEquals(1, interpreter.getAttributes().getLength());
    assertEquals("clas", interpreter.getAttributes().getValue("clas"));
    assertTrue(interpreter.isSelfClosing());
  }

  @Test
  public void testLenientSaxParser() throws Exception {
    // This would be much better as a Mockito test...

    String html = "<!DOCTYPE html>" +
        "  <?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r" +
        "<html><head><title>Hello!</title>" +
        "<script type=\"text/javascript\">" +
        "  if (5<Number.MAX_VALUE && 2 > 3) alert('yup');" +
        "</script >" +
        "<STYLE type=\"text/css\">" +
        "  .let's let random < crap > be here for safety !!<<<" +
        "</STYLE>" +
        "</head>" +
        "<body onload='alert(\"moose!\")'>Get <b>ready</b>..." +
        "<!-- any comments and <tag>s inside should be ignored! --->" +
        "<img src='image.jpg' width=500 height=600 title=\"Monster!\"/>" +
        "</body></html>";
    LenientSaxParser parser = new LenientSaxParser();
    final Set<String> stringsToFind = new HashSet<String>();
    stringsToFind.add("  ");
    stringsToFind.add("\r");
    stringsToFind.add("Hello!");
    stringsToFind.add("  if (5<Number.MAX_VALUE && 2 > 3) alert('yup');");
    stringsToFind.add("  .let's let random < crap > be here for safety !!<<<");
    stringsToFind.add("Get ");
    stringsToFind.add("ready");
    stringsToFind.add("...");

    final Set<String> tagsToFind = new HashSet<String>();
    tagsToFind.add("html");
    tagsToFind.add("head");
    tagsToFind.add("title");
    tagsToFind.add("script");
    tagsToFind.add("STYLE");
    tagsToFind.add("body");
    tagsToFind.add("b");
    tagsToFind.add("img");

    final Set<String> endTagsToFind = new HashSet<String>();
    endTagsToFind.add("title");
    endTagsToFind.add("script");
    endTagsToFind.add("STYLE");
    endTagsToFind.add("head");
    endTagsToFind.add("b");
    endTagsToFind.add("img");
    endTagsToFind.add("body");
    endTagsToFind.add("html");

    parser.parse(new ByteArrayInputStream(html.getBytes()), new DefaultHandler() {
      @Override
      public void characters(char[] c, int start, int length) {
        String string = String.copyValueOf(c, start, length);
        assertTrue("Could not find characters: \"" + string + "\"", stringsToFind.contains(string));
        stringsToFind.remove(string);
      }

      @Override
      public void startElement(String uri, String localName, String qName,
          Attributes attributes) {
        assertTrue("Could not find tag: " + qName, tagsToFind.contains(qName));
        if ("img".equals(qName)) {
          assertEquals(4, attributes.getLength());
          assertEquals("image.jpg", attributes.getValue("src"));
          assertEquals("500", attributes.getValue("width"));
          assertEquals("600", attributes.getValue("height"));
          assertEquals("Monster!", attributes.getValue("title"));
        }
        tagsToFind.remove(qName);
      }

      @Override
      public void endElement(String uri, String localName, String qName) {
        assertTrue("Could not find end tag: " + qName, endTagsToFind.contains(qName));
        endTagsToFind.remove(qName);
      }

      @Override
      public void processingInstruction(String target, java.lang.String data) {
        assertEquals("xml", target);
      }
    });

    assertTrue(stringsToFind.isEmpty());
    assertTrue(tagsToFind.isEmpty());
    assertTrue(endTagsToFind.isEmpty());
  }
}
