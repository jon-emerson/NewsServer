package com.janknspank.dom.parser;

import java.io.IOException;
import java.io.InputStream;

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
public class DomBuilder {
  private DocumentNode documentNode;
  private DomContentHandler domContentHandler = new DomContentHandler();

  public DomBuilder(InputStream inputStream) throws ParserException {
    reset();
    try {
      new LenientSaxParser().parse(
          new InputSource(new CharsetDetectingReader(inputStream)), domContentHandler);
    } catch (SAXException | IOException e) {
      throw new ParserException(e.getMessage(), e);
    }
  }

  private class DomContentHandler extends DefaultHandler {
    private LenientLocator locator = null;
    private Node currentNode = documentNode;

    private void reset() {
      locator = null;
      currentNode = null;
    }

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

  private void reset() {
    documentNode = new DocumentNode();
    domContentHandler = new DomContentHandler();
    domContentHandler.reset();
  }

  public DocumentNode getDocumentNode() {
    return documentNode;
  }
}
