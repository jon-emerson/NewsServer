package com.janknspank.dom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.Attributes2Impl;

/**
 * A class that implements Java's SAXParser pattern in a way that doesn't fail
 * due to malformed HTML/XML.
 */
@SuppressWarnings("deprecation")
public class LenientSaxParser extends SAXParser {
  private final LenientXMLReader xmlReader;

  /**
   * Parses an element, e.g. <div> or <div foo="true">, into its tag name and
   * attributes.
   */
  static class LenientElementInterpreter {
    private String tag = null;
    private boolean selfClosing = false;
    private Attributes2Impl attributes = new Attributes2Impl();

    private enum InterpreterState {
      /**
       * We're still gathering characters for the tag name.
       */
      TAG,

      /**
       * We're after the tag, but we're not sure if we'll end up in an attribute
       * name or find a > next.
       */
      ENIGMATIC_VOID,

      /**
       * We're collecting an attribute's name.
       */
      ATTRIBUTE_NAME,

      /**
       * We're collecting an attribute's value, but haven't yet seen any quotes.
       */
      ATTRIBUTE_VALUE,

      /**
       * We're collecting an attribute's value, and it's wrapped in a single
       * quote (so it'll be terminated with a single quote).
       */
      ATTRIBUTE_VALUE_INSIDE_SINGLE_QUOTE,

      /**
       * We're collecting an attribute's value, and it's wrapped in a double
       * quote (so it'll be terminated with a double quote).
       */
      ATTRIBUTE_VALUE_INSIDE_DOUBLE_QUOTE;
    }

    public LenientElementInterpreter(String element) {
      if (element.charAt(0) != '<') {
        throw new IllegalStateException(
            "Elements must start with <.  Instead received: " + element);
      }
      InterpreterState state = InterpreterState.TAG;
      selfClosing = element.endsWith("/>");
      StringBuilder b = new StringBuilder();
      String currentAttributeName = null;
      for (int i = 1; i < element.length(); i++) {
        char c = element.charAt(i);
        if (Character.isWhitespace(c) || i == element.length() - (selfClosing ? 2 : 1)) {
          switch (state) {
            case TAG:
              tag = b.toString();
              b.setLength(0);
              state = InterpreterState.ENIGMATIC_VOID;
              break;
            case ATTRIBUTE_NAME:
              String attributeName = b.toString();
              attributes.addAttribute(
                  /* uri */ "",
                  /* localName */ "",
                  /* qName */ attributeName,
                  /* type */ "",
                  /* value */ attributeName);
              b.setLength(0);
              state = InterpreterState.ENIGMATIC_VOID;
              break;
            case ATTRIBUTE_VALUE:
              // We haven't seen a quote yet... So let's end the value...
              attributes.addAttribute(
                  /* uri */ "",
                  /* localName */ "",
                  /* qName */ currentAttributeName,
                  /* type */ "",
                  /* value */ b.toString());
              currentAttributeName = null;
              b.setLength(0);
              state = InterpreterState.ENIGMATIC_VOID;
              break;
            case ENIGMATIC_VOID:
              break;
            default:
              // These are spaces within quoted attribute values.
              b.append(c);
          }
        } else if (c == '=' && state == InterpreterState.ATTRIBUTE_NAME) {
          currentAttributeName = b.toString();
          b.setLength(0);
          state = InterpreterState.ATTRIBUTE_VALUE;
        } else if (c == '"' && b.length() == 0 && state == InterpreterState.ATTRIBUTE_VALUE) {
          state = InterpreterState.ATTRIBUTE_VALUE_INSIDE_DOUBLE_QUOTE;
        } else if (c == '"' && state == InterpreterState.ATTRIBUTE_VALUE_INSIDE_DOUBLE_QUOTE) {
          attributes.addAttribute(
              /* uri */ "",
              /* localName */ "",
              /* qName */ currentAttributeName,
              /* type */ "",
              /* value */ b.toString());
          currentAttributeName = null;
          b.setLength(0);
          state = InterpreterState.ENIGMATIC_VOID;
        } else if (c == '\'' && b.length() == 0 && state == InterpreterState.ATTRIBUTE_VALUE) {
          state = InterpreterState.ATTRIBUTE_VALUE_INSIDE_SINGLE_QUOTE;
        } else if (c == '\'' && state == InterpreterState.ATTRIBUTE_VALUE_INSIDE_SINGLE_QUOTE) {
          attributes.addAttribute(
              /* uri */ "",
              /* localName */ "",
              /* qName */ currentAttributeName,
              /* type */ "",
              /* value */ b.toString());
          currentAttributeName = null;
          b.setLength(0);
          state = InterpreterState.ENIGMATIC_VOID;
        } else if ((c == '\'' || c == '"') && state == InterpreterState.ATTRIBUTE_NAME) {
          // This is illegal: There shouldn't be quotes inside attribute names.
          // To handle this "best" let's just commit the attribute name so far
          // and ignore the quote.
          String attributeName = b.toString();
          attributes.addAttribute(
              /* uri */ "",
              /* localName */ "",
              /* qName */ attributeName,
              /* type */ "",
              /* value */ attributeName);
          b.setLength(0);
          state = InterpreterState.ENIGMATIC_VOID;
        } else {
          if (state == InterpreterState.ENIGMATIC_VOID) {
            state = InterpreterState.ATTRIBUTE_NAME;
          }
          b.append(c);
        }
      }
    }

    public String getTag() {
      return tag;
    }

    public Attributes getAttributes() {
      return attributes;
    }

    public boolean isSelfClosing() {
      return selfClosing;
    }
  }

  private static class LenientXMLReader implements XMLReader {
    private ContentHandler handler = null;
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
    public void setContentHandler(ContentHandler handler) {
      this.handler = handler;
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
      handler.startDocument();

      // We could be passed either a character stream Reader or an InputStream,
      // so handle both.
      BufferedReader reader;
      if (input.getCharacterStream() != null) {
        reader = new BufferedReader(input.getCharacterStream());
      } else {
        reader = new BufferedReader(new InputStreamReader(input.getByteStream(), "UTF-8"));
      }

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

    private void reset() {
      properties.clear();
    }
  }

  protected LenientSaxParser() {
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
