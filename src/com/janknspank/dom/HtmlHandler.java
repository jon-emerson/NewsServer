package com.janknspank.dom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;

import org.mozilla.universalchardet.UniversalDetector;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Charsets;

public class HtmlHandler extends DefaultHandler {
  private static final boolean VERBOSE = false;
  private LenientLocator locator = null;
  private DocumentNode documentNode;
  private Node currentNode = documentNode;
  private int depth = 0;

  public HtmlHandler(InputStream inputStream) throws ParseException {
    reset();
    try {
      new LenientSaxParser().parse(new InputSource(getReader(inputStream)), this);
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets a Reader that auto-detects the Charset of the HTML page, since some
   * web sites (NYTimes from 8 years ago, etc) don't accurately tell us what
   * charset their web pages are in.
   * 
   * NOTE(jonemerson): My initial attempt at this method used IBM's ICU4J, but
   * that FAILED to detect Windows CP1252 on a fairly trivial document.
   * Mozilla's implementation works flawlessly, though.
   */
  public Reader getReader(InputStream inputStream) throws IOException {
    UniversalDetector detector = new UniversalDetector(null);

    // Read data until the UniversalDetector says it's had enough (isDone()
    // returns true).  Store whatever we get into a ByteArrayOutputStream so we
    // can include it in our response.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int readBytes = inputStream.read(buffer, 0, buffer.length);
    while (readBytes >= 0) {
      baos.write(buffer, 0, readBytes);
      detector.handleData(buffer, 0, readBytes);
      if (detector.isDone()) {
        break;
      }
      readBytes = inputStream.read(buffer, 0, buffer.length);
    }
    detector.dataEnd();

    // Get out detected encoding, falling back to UTF-8.
    String encoding = detector.getDetectedCharset();
    if (encoding == null) {
      encoding = Charsets.UTF_8.name();
    }
    System.err.println("Detected encoding: " + encoding);

    // Return a Reader for the detected Charset, using the combined data from
    // what we read to detect the charset and what we haven't yet read.
    return new InputStreamReader(
        new SequenceInputStream(
            new ByteArrayInputStream(baos.toByteArray()),
            inputStream),
        encoding);
  }

  @Override
  public void setDocumentLocator(Locator locator) {
    this.locator = (LenientLocator) locator;
  }

  private void reset() {
    documentNode = new DocumentNode();
    currentNode = documentNode;
  }

  public DocumentNode getDocumentNode() {
    return documentNode;
  }

  @SuppressWarnings("unused")
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    String s = String.copyValueOf(ch, start, length);
    if (VERBOSE && s.trim().length() > 0) {
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
    if (VERBOSE) {
      printSpaces();
      System.out.println("</" + qName + ">");
    }

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
    if (VERBOSE) {
      printSpaces();
      System.out.println("<" + qName + ">");
    }
    ++depth;

    currentNode = new Node(currentNode, qName, locator.getStartingOffset());
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
