package com.janknspank.dom;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class HtmlHandler extends DefaultHandler {
  private DocumentNode documentNode;
  private Node currentNode = documentNode;
  private int depth = 0;

  public HtmlHandler(InputStream inputStream) {
    reset();
    try {
      new LenientSaxParser().parse(inputStream, this);
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
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
    String s = String.copyValueOf(ch, start, length);
    if (s.trim().length() > 0) {
      printSpaces();
      System.out.println("TEXT: " + s.trim());
    }
    currentNode.addChildText(s);
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
    --depth;
    printSpaces();
    System.out.println("</" + qName + ">");

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
    printSpaces();
    System.out.println("<" + qName + ">");
    ++depth;

    currentNode = new Node(currentNode, qName);
    currentNode.getParent().addChildNode(currentNode);
    for (int i = 0; i < attrs.getLength(); i++) {
      currentNode.addAttribute(attrs.getQName(i), attrs.getValue(i));
    }
  }
  
  private void printSpaces() {
    for (int i = 0; i < depth; i++) {
      System.out.print("  ");
    }
  }
}
