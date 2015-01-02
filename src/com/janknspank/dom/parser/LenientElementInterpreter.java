package com.janknspank.dom.parser;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ext.Attributes2Impl;


/**
 * Parses an element, e.g. <div> or <div foo="true">, into its tag name and
 * attributes.
 */
class LenientElementInterpreter {
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
                /* value */ StringEscapeUtils.unescapeHtml4(b.toString()));
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
            /* value */ StringEscapeUtils.unescapeHtml4(b.toString()));
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
            /* value */ StringEscapeUtils.unescapeHtml4(b.toString()));
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