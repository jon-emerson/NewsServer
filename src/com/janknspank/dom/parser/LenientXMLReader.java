package com.janknspank.dom.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

class LenientXMLReader implements XMLReader {
  private ContentHandler handler = null;
  private CountingReader reader = null;
  private LenientLocator locator = new LenientLocator();
  private ErrorHandler errorHandler = null;
  private EntityResolver entityResolver = null;
  private DTDHandler dtdHandler = null;
  private Set<String> features = new HashSet<>();
  private Map<String, Object> properties = new HashMap<>();

  @Override
  public boolean getFeature(String name) throws SAXNotRecognizedException,
      SAXNotSupportedException {
    return features.contains(name);
  }

  @Override
  public void setFeature(String name, boolean value)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    if (value) {
      features.add(name);
    } else {
      features.remove(name);
    }
  }

  @Override
  public Object getProperty(String name)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    return properties.get(name);
  }

  @Override
  public void setProperty(String name, Object value)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    properties.put(name, value);
  }

  @Override
  public void setEntityResolver(EntityResolver resolver) {
    this.entityResolver = resolver;
  }

  @Override
  public EntityResolver getEntityResolver() {
    return entityResolver;
  }

  @Override
  public void setDTDHandler(DTDHandler handler) {
    this.dtdHandler = handler;
  }

  @Override
  public DTDHandler getDTDHandler() {
    return dtdHandler;
  }

  @Override
  public void setContentHandler(final ContentHandler newHandler) {
    /**
     * Wraps the given ContentHandler in an anonymous handler that updates the
     * SAX Locator with the current parsing position before calling the
     * caller's Handler.  This makes it so that the caller's Handler can
     * know when tags start and end.
     */
    this.handler = new ContentHandler() {
      private void updateLocator() {
        LenientXMLReader.this.locator.setColumnNumber(
            LenientXMLReader.this.reader.getColumnNumber());
        LenientXMLReader.this.locator.setLineNumber(
            LenientXMLReader.this.reader.getLineNumber());
      }

      @Override
      public void setDocumentLocator(Locator locator) {
        newHandler.setDocumentLocator(locator);
      }

      @Override
      public void startDocument() throws SAXException {
        updateLocator();
        newHandler.startDocument();
      }

      @Override
      public void endDocument() throws SAXException {
        updateLocator();
        newHandler.endDocument();
      }

      @Override
      public void startPrefixMapping(String prefix, String uri) throws SAXException {
        updateLocator();
        newHandler.startPrefixMapping(prefix, uri);
      }

      @Override
      public void endPrefixMapping(String prefix) throws SAXException {
        updateLocator();
        newHandler.endPrefixMapping(prefix);
      }

      @Override
      public void startElement(String uri, String localName, String qName, Attributes atts)
          throws SAXException {
        updateLocator();
        newHandler.startElement(uri, localName, qName, atts);
      }

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
        updateLocator();
        newHandler.endElement(uri, localName, qName);
      }

      @Override
      public void characters(char[] ch, int start, int length) throws SAXException {
        updateLocator();
        newHandler.characters(ch, start, length);
      }

      @Override
      public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        updateLocator();
        newHandler.ignorableWhitespace(ch, start, length);
      }

      @Override
      public void processingInstruction(String target, String data) throws SAXException {
        updateLocator();
        newHandler.processingInstruction(target, data);
      }

      @Override
      public void skippedEntity(String name) throws SAXException {
        updateLocator();
        newHandler.skippedEntity(name);
      }
    };
  }

  @Override
  public ContentHandler getContentHandler() {
    return handler;
  }

  @Override
  public void setErrorHandler(ErrorHandler handler) {
    this.errorHandler = handler;
  }

  @Override
  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  private enum ParserState {
    DEFAULT,
    INSIDE_ELEMENT,
    INSIDE_ELEMENT_INSIDE_SINGLE_QUOTE,
    INSIDE_ELEMENT_INSIDE_DOUBLE_QUOTE;
  }

  @Override
  public void parse(InputSource input) throws IOException, SAXException {
    // We could be passed either a character stream Reader or an InputStream,
    // so handle both.
    if (input.getCharacterStream() != null) {
      reader = new CountingReader(new BufferedReader(input.getCharacterStream()));
    } else {
      reader = new CountingReader(
          new BufferedReader(new InputStreamReader(input.getByteStream(), "UTF-8")));
    }

    // Let the handler know we've started.
    handler.setDocumentLocator(locator);
    handler.startDocument();

    // And let's go, finite state machine!  Go!
    ParserState state = ParserState.DEFAULT;
    int characterInt = reader.read();
    StringBuilder currentBlock = new StringBuilder();
    while (characterInt != -1) {
      char c = (char) characterInt;
      boolean append = true;
      switch (c) {
        case '<':
          if (state == ParserState.DEFAULT) {
            if (currentBlock.length() > 0) {
              String currentBlockString =
                  StringEscapeUtils.unescapeHtml4(currentBlock.toString());
              handler.characters(currentBlockString.toCharArray(), 0,
                  currentBlockString.length());
              currentBlock.setLength(0);
            }
            locator.setStartingOffset(reader.getOffset());
            state = ParserState.INSIDE_ELEMENT;
          }
          break;
        case '"':
          if (state == ParserState.INSIDE_ELEMENT) {
            state = ParserState.INSIDE_ELEMENT_INSIDE_DOUBLE_QUOTE;
          } else if (state == ParserState.INSIDE_ELEMENT_INSIDE_DOUBLE_QUOTE) {
            state = ParserState.INSIDE_ELEMENT;
          }
          break;
        case '\'':
          if (state == ParserState.INSIDE_ELEMENT) {
            state = ParserState.INSIDE_ELEMENT_INSIDE_SINGLE_QUOTE;
          } else if (state == ParserState.INSIDE_ELEMENT_INSIDE_SINGLE_QUOTE) {
            state = ParserState.INSIDE_ELEMENT;
          }
          break;
        case '>':
          if (state == ParserState.INSIDE_ELEMENT) {
            // Append the > now, so it's included in our call to handleElement.
            currentBlock.append(c);
            append = false;
            String tagName = handleElement(currentBlock.toString());
            currentBlock.setLength(0);
            state = ParserState.DEFAULT;

            // In the case of <script> and <style> tags, due the the allowed
            // complexity within their bodies, we must look explicitly for
            // </script> before we continue processing the document.
            if ("script".equalsIgnoreCase(tagName) || "style".equalsIgnoreCase(tagName)) {
              StringBuilder scriptBuilder = new StringBuilder();
              characterInt = reader.read();
              char[] scriptEndTrigger = ("</" + tagName).toCharArray();
              char[] scriptEndHelperBuffer = new char[scriptEndTrigger.length];
              while (characterInt >= 0) {
                scriptBuilder.append((char) characterInt);
                scriptBuilder.getChars(
                    /* srcBegin */ Math.max(
                        0, scriptBuilder.length() -scriptEndHelperBuffer.length),
                    /* srcEnd */ scriptBuilder.length(),
                    /* dst */ scriptEndHelperBuffer,
                    /* dstBegin */ 0);
                if (Arrays.equals(scriptEndTrigger, scriptEndHelperBuffer)) {
                  // We're at the end.  Remove "</script", send everything we got to
                  // characters(), send an endElement() for the script, and move the
                  // reader's cursor past the > from the </script>.
                  scriptBuilder.replace(scriptBuilder.length() - scriptEndHelperBuffer.length,
                      scriptBuilder.length(), "");
                  char[] characters = new char[scriptBuilder.length()];
                  scriptBuilder.getChars(0, characters.length, characters, 0);
                  handler.characters(characters, 0, characters.length);
                  handler.endElement("", "", tagName);
                  while (characterInt >= 0 && ((char) characterInt) != '>') {
                    characterInt = reader.read();
                  }
                  break;
                }
                characterInt = reader.read();
              }
            }
          }
          break;
      }
      if (append) {
        currentBlock.append(c);
      }

      // Handle XML <!-- comments -->!
      if (state == ParserState.INSIDE_ELEMENT &&
          currentBlock.length() == 4 &&
          "<!--".equals(currentBlock.toString())) {
        // Iterate until we find the end of comment.  Since the end-of-comment
        // specifier has 3 characters, it's difficult to do in this per-
        // character state machine without resorting to 3 states.
        StringBuilder seeker = new StringBuilder();
        characterInt = reader.read();
        while (characterInt >= 0) {
          if (seeker.length() > 2) {
            seeker.replace(0, 1, "");
          }
          seeker.append((char) characterInt);
          if ("-->".equals(seeker.toString())) {
            break;
          }
          characterInt = reader.read();
        }
        currentBlock.setLength(0);
        state = ParserState.DEFAULT;
      }

      // Let's go again!
      characterInt = reader.read();
    }

    handler.endDocument();
  }

  /**
   * Takes an element of the form "<?foo?>", "<foo>", "<foo/>", or "</foo>"
   * and calls the appropriate method on the Handler for it.
   * @return the name of the tag that was handled
   */
  private String handleElement(String text) throws SAXException {
    if (text.startsWith("<?")) {
      if (text.endsWith("?>")) {
        int spaceIndex = text.indexOf(' ');
        if (spaceIndex > 0) {
          handler.processingInstruction(
              text.substring(2, spaceIndex), text.substring(spaceIndex + 1, text.length() - 2));
        } else {
          handler.processingInstruction(text.substring(2, text.length() - 2), null);
        }
      } else {
        handler.processingInstruction(text.substring(2,  text.length() - 1), null);
      }
    } else if (text.startsWith("<!")) {
      if (text.startsWith("<!--")) {
        throw new IllegalStateException(
            "Handle element should only be called for elements, not comments.");
      }
      // Ignore this.  It's most likely a !DOCTYPE declaration, and we don't care.
    } else if (text.startsWith("</")) {
      int spaceIndex = text.indexOf(" ");
      if (spaceIndex > 0) {
        handler.endElement("", "", text.substring(2, spaceIndex).trim());
      } else {
        handler.endElement("", "", text.substring(2, text.length() - 1).trim());
      }
    } else {
      LenientElementInterpreter interpreter = new LenientElementInterpreter(text);
      handler.startElement(
          /* uri */ "", 
          /* localName */ "",
          /* qName */ interpreter.getTag(),
          /* atts */ interpreter.getAttributes());
      if (interpreter.isSelfClosing()) {
        handler.endElement("", "", interpreter.getTag());
      }
      return interpreter.getTag();
    }
    return null;
  }

  @Override
  public void parse(String systemId) throws IOException, SAXException {
    parse(new InputSource(systemId));
  }

  void reset() {
    properties.clear();
  }
}