package com.janknspank.dom;

import java.io.IOException;

import javax.xml.parsers.SAXParser;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

public class LenientSaxParser extends SAXParser {

  @SuppressWarnings("deprecation")
  @Override
  public Parser getParser() throws SAXException {
    return null;
  }

  @Override
  public XMLReader getXMLReader() throws SAXException {
    return new XMLReader() {

      @Override
      public boolean getFeature(String name) throws SAXNotRecognizedException,
          SAXNotSupportedException {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public void setFeature(String name, boolean value)
          throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        
      }

      @Override
      public Object getProperty(String name) throws SAXNotRecognizedException,
          SAXNotSupportedException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setProperty(String name, Object value)
          throws SAXNotRecognizedException, SAXNotSupportedException {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void setEntityResolver(EntityResolver resolver) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public EntityResolver getEntityResolver() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setDTDHandler(DTDHandler handler) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public DTDHandler getDTDHandler() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setContentHandler(ContentHandler handler) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public ContentHandler getContentHandler() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setErrorHandler(ErrorHandler handler) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public ErrorHandler getErrorHandler() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void parse(InputSource input) throws IOException, SAXException {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void parse(String systemId) throws IOException, SAXException {
        // TODO Auto-generated method stub
        
      }
    };
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
  public void setProperty(String name, Object value)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    // No properties are currently supported.
  }

  @Override
  public Object getProperty(String name) throws SAXNotRecognizedException,
      SAXNotSupportedException {
    return null;
  }
}
