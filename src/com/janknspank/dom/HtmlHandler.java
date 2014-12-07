package com.janknspank.dom;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class HtmlHandler extends DefaultHandler {
  private DocumentNode documentNode;
  private Node currentNode = documentNode;

  public HtmlHandler(InputStream inputStream) {
    reset();
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setValidating(false);
    spf.setNamespaceAware(true);
    try {
      spf.newSAXParser().parse(inputStream, this);
    } catch (SAXException | IOException | ParserConfigurationException e) {
      System.err.println("BAD HTML - FALLING BACK TO TAGSOUP!");
      e.printStackTrace();
      try {
        reset();
        SAXParserImpl.newInstance(null).parse(inputStream, this);
      } catch (SAXException | IOException e2) {
        throw new RuntimeException(e2);
      }
    }
  }

  private void reset() {
    documentNode = new DocumentNode();
    currentNode = documentNode;
  }

  public DocumentNode getDocumentNode() {
    return documentNode;
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    System.out.println("TEXT: " + String.copyValueOf(ch, start, length));
    currentNode.addChildText(String.copyValueOf(ch, start, length));
  }

  @Override
  public void endDocument() {
    if (!(currentNode instanceof DocumentNode)) {
      throw new IllegalStateException("Document ended without closing its body");
    }
  }

  @Override
  public void endElement(String namespaceURI,
      String localName,
      String qName) throws SAXException {
    System.out.println("END: " + localName + ", " + qName);

    // Clean up text nodes.
    currentNode.condenseTextChildren();

    while (!(currentNode instanceof DocumentNode) &&
        !currentNode.getTagName().equalsIgnoreCase(localName)) {
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
    System.out.println("START: " + localName + ", " + qName);

    currentNode = new Node(currentNode, localName);
    currentNode.getParent().addChildNode(currentNode);
    for (int i = 0; i < attrs.getLength(); i++) {
      currentNode.addAttribute(attrs.getLocalName(i), attrs.getValue(i));
    }
  }
}