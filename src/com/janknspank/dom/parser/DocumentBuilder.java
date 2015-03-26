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
 * {@code Node#findAll(String)} and {@code Node#findFirst(String)}.
 */
public class DocumentBuilder {
  public static DocumentNode build(String url, Reader reader) throws ParserException {
    try {
      DomContentHandler domContentHandler = new DomContentHandler(url);
      new LenientSaxParser().parse(new InputSource(reader), domContentHandler);
      return domContentHandler.documentNode;
    } catch (SAXException | IOException e) {
      throw new ParserException(e.getMessage(), e);
    }
  }

  private static class DomContentHandler extends DefaultHandler {
    private String url;
    private DocumentNode documentNode;
    private LenientLocator locator = null;
    private Node currentNode;

    public DomContentHandler(String url) {
      this.url = url;
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
    public void startDocument() {
      documentNode = new DocumentNode(url);
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
      Node node = currentNode;
      while (!(node instanceof DocumentNode) &&
          !node.getTagName().equalsIgnoreCase(qName)) {
        node = node.getParent();
      }
      if (!(node instanceof DocumentNode)) {
        // Clean up text nodes.
        currentNode.condenseTextChildren();

        // If we bubbled all the way up to the document node, then it's a stray
        // end tag, and we should ignore it.  If not, it closes something, so
        // set the current node to the parent of what was closed.
        currentNode = node.getParent();
      }
    }

    @Override
    public void startElement(String namespaceURI,
        String localName,
        String qName,
        Attributes attrs)
        throws SAXException {
      // <p>s and <li>s self-end themselves.
      // NOTE(jonemerson): There's probably an argument to be made that we
      // should search up the tree for any <p> or <li> to close.  And there's
      // probably some better logic to use also, because you can totally nest
      // self-ending tags, in which case, if a new tag starts, we might need
      // to close multiple nested tags.  But, until we have bugs that require
      // this logic, it's probably not worth spending time on.
      if (("p".equalsIgnoreCase(qName) || "li".equalsIgnoreCase(qName))
          && qName.equalsIgnoreCase(currentNode.getTagName())) {
        endElement(namespaceURI, localName, qName);
      }

      currentNode = new Node(currentNode, qName, locator.getStartingOffset());
      currentNode.getParent().addChildNode(currentNode);
      for (int i = 0; i < attrs.getLength(); i++) {
        currentNode.addAttribute(attrs.getQName(i), attrs.getValue(i));
      }
    }
  }
}
