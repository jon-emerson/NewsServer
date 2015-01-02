package com.janknspank.dom.parser;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * A class that implements Java's SAXParser pattern in a way that doesn't fail
 * due to malformed HTML/XML.
 */
@SuppressWarnings("deprecation")
public class LenientSaxParser extends SAXParser {
  private final LenientXMLReader xmlReader;

  public LenientSaxParser() {
    super();
    this.xmlReader = new LenientXMLReader();
  }

  @Override
  public Parser getParser() throws SAXException {
    return null;
  }

  @Override
  public XMLReader getXMLReader() throws SAXException {
    return xmlReader;
  }

  @Override
  public boolean isNamespaceAware() {
    return false;
  }

  @Override
  public boolean isValidating() {
    return false;
  }

  @Override
  public boolean isXIncludeAware() {
    return false;
  }

  @Override
  public void setProperty(String name, Object value)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    xmlReader.setProperty(name, value);
  }

  @Override
  public Object getProperty(String name) throws SAXNotRecognizedException,
      SAXNotSupportedException {
    return xmlReader.getProperty(name);
  }

  @Override
  public void reset() {
    xmlReader.reset();
  }
}
