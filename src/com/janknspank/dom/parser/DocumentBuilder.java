package com.janknspank.dom.parser;

import java.io.IOException;
import java.io.Reader;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Constructs a DOM from an HTML or XML page.  The DOM is a hierarchy of {@code
 * Node} objects, the topmost of which is a {@code DocumentNode}.  Using the
 * returned DocumentNode, you can then dig into the website or find nodes using
 * {@code Node#findAll(String)}.
 */
public class DocumentBuilder {
  public static DocumentNode build(Reader reader) throws ParserException {
    try {
      DomContentHandler domContentHandler = new DomContentHandler();
      new LenientSaxParser().parse(new InputSource(reader), domContentHandler);
      return domContentHandler.documentNode;
    } catch (SAXException | IOException e) {
      throw new ParserException(e.getMessage(), e);
    }
  }

  private static class DomContentHandler extends DefaultHandler {
    private DocumentNode documentNode;
    private LenientLocator locator = null;
    private Node currentNode;

    @Override
    public void setDocumentLocator(Locator locator) {
      this.locator = (LenientLocator) locator;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      String s = String.copyValueOf(ch, start, length);
      currentNode.addChildText(s);
    }

    @Override
    public void startDocument() {
      documentNode = new DocumentNode();
      currentNode = documentNode;
    }

    @Override
    public void endDocument() {
      if (!(currentNode instanceof DocumentNode)) {
        System.out.println("WARNING: Document ended without closing its body");
      }
    }

    @Override
    public void endElement(String namespaceURI,
        String localName,
        String qName) throws SAXException {
      // Clean up text nodes.
      currentNode.condenseTextChildren();

      while (!(currentNode instanceof DocumentNode) &&
          !currentNode.getTagName().equalsIgnoreCase(qName)) {
        currentNode = currentNode.getParent();
      }
      if (!(currentNode instanceof DocumentNode)) {
        currentNode = currentNode.getParent();
      }
    }

    @Override
    public void startElement(String namespaceURI,
        String localName,
        String qName,
        Attributes attrs)
        throws SAXException {
      currentNode = new Node(currentNode, qName, locator.getStartingOffset());
      currentNode.getParent().addChildNode(currentNode);
      for (int i = 0; i < attrs.getLength(); i++) {
        currentNode.addAttribute(attrs.getQName(i), attrs.getValue(i));
      }
    }
  }
}
