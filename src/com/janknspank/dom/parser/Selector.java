package com.janknspank.dom.parser;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A CSS-like selector.  Specifies which tagName, classes, ID, attribute names,
 * and possibly attribute values, a tag must have to satisfy a selection.
 */
public class Selector {
  @VisibleForTesting final String tagName;
  @VisibleForTesting final Set<String> classes;
  @VisibleForTesting final String id;
  @VisibleForTesting final Set<String> attributes;
  @VisibleForTesting final Map<String, String> attributeValues;
  @VisibleForTesting final boolean directDescendant;

  public Selector(String definition) {
    this(definition, false);
  }

  public Selector(String definition, boolean directDescendant) {
    this.directDescendant = directDescendant;

    String tagName = null;
    ImmutableSet.Builder<String> classes = ImmutableSet.builder();
    String id = null;
    ImmutableSet.Builder<String> attributes = ImmutableSet.builder();
    ImmutableMap.Builder<String, String> attributeValues = ImmutableMap.builder();
    int i = 0;
    for (String token : tokenizeDefinition(definition)) {
      if (token.startsWith("#")) {
        if (id != null) {
          throw new IllegalStateException("Attribute selector cannot have two " +
              "IDs.  Definition=\"" + definition + "\"");
        }
        id = token.substring(1);
      } else if (token.startsWith(".")) {
        classes.add(token.substring(1));
      } else if (token.startsWith("[")) {
        if (!token.endsWith("]")) {
          throw new IllegalStateException("Attribute selector does not " +
              "terminate properly.  Definition=\"" + definition + "\"");
        }
        String[] components = token.substring(1, token.length() - 1).split("=", 2);
        if (components.length == 1) {
          attributes.add(components[0].trim());
        } else {
          String value = components[1].trim();
          if ((value.charAt(0) == '\'' || value.charAt(0) == '"') &&
              value.charAt(0) == value.charAt(value.length() - 1)) {
            value = value.substring(1, value.length() - 1);
          }
          attributeValues.put(components[0].trim(), value);
        }
      } else {
        if (i != 0) {
          // This should not be possible: It indicates a logic problem in this
          // class.
          throw new IllegalStateException("Tag names should be first in " +
              "selector definitions.  Definition=\"" + definition + "\"");
        }
        tagName = token;
      }
      i++;
    }

    this.tagName = tagName;
    this.classes = classes.build();
    this.id = id;
    this.attributes = attributes.build();
    this.attributeValues = attributeValues.build();
  }

  public static List<Selector> parseSelectors(String definitionStack) {
    List<Selector> selectors = Lists.newArrayList();
    boolean isNextDirectDescendent = false;
    for (String definition : tokenizeDefinitionStack(definitionStack)) {
      if (!">".equals(definition)) {
        selectors.add(new Selector(definition, isNextDirectDescendent));
      }
      isNextDirectDescendent = ">".equals(definition);
    }
    return selectors;
  }

  /**
   * Tokenizes possibly multiple element selector definitions into their
   * individual selector definitions.
   *
   * Example call: div#id  a.class[href][target = "#hello"]>b
   * Would return: {"div#id", "a.class[href][target = "#hello"]", ">", "b"}.
   */
  @VisibleForTesting
  static List<String> tokenizeDefinitionStack(String definitionStack) {
    List<String> tokens = Lists.newArrayList();
    StringBuilder currentToken = new StringBuilder();
    boolean inBracket = false;
    char inQuoteType = 0; // If set, the type of quote we're looking to end.
    for (int i = 0; i < definitionStack.length(); i++) {
      char c = definitionStack.charAt(i);
      if (c == ' ' || c == '>') {
        if (!inBracket && inQuoteType == 0) {
          if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
          }
          if (c == '>') {
            tokens.add(">");
          }
          currentToken.setLength(0);
          continue;
        }
      }
      switch (c) {
        case '[':
          inBracket = true;
          break;
        case ']':
          if (inQuoteType == 0) {
            inBracket = false;
          }
          break;
        case '"':
        case '\'':
          if (inBracket) {
            if (inQuoteType == 0) {
              inQuoteType = c;
            } else {
              if (inQuoteType == c) {
                inQuoteType = 0;
              }
            }
          }
      }
      currentToken.append(c);
    }
    if (currentToken.length() > 0) {
      tokens.add(currentToken.toString());
    }
    return tokens;
  }

  /**
   * Tokenizes a single element selector definition into its element type, ID,
   * class, and attribute selectors.  Only the uninterpreted strings are
   * returned - it's up to the caller to interpret what they mean.
   * 
   * Example call: a.class#id[href][target="_blank"][value=" [] "]
   * Would return: {"a", ".class", "#id", "[href]", "[target=\"_blank\"]",
   * "[value=\" [] \"]"}.
   */
  @VisibleForTesting
  static List<String> tokenizeDefinition(String definition) {
    // Pass one: Break on dots, hashes, and ['s.  There's definitely room for
    // mistakes here: These can all validly exist within []s.  We'll fix this
    // in a second pass.
    // TODO(jonemerson): I thought long and hard about how to do this - It could
    // probably be done with more elegantly regexps, preventing the second pass,
    // but this was mentally less taxing to do.
    List<String> tokens = Lists.newArrayList();
    StringBuilder currentToken = new StringBuilder();
    for (int i = 0; i < definition.length(); i++) {
      char c = definition.charAt(i);
      if (c == '.' || c == '#' || c == '[') {
        tokens.add(currentToken.toString());
        currentToken.setLength(0);
      }
      currentToken.append(c);
    }
    tokens.add(currentToken.toString());

    // Here's the second pass: If a token starts with [, but doesn't end with ],
    // then we prematurely tokenized.  Join said token with its next neighbor.
    // This pass also fixes the problem where ".foo" creates an empty token to
    // start.
    for (int i = 0; i < tokens.size(); /* no-op */) {
      String token = tokens.get(i);
      if (token.isEmpty()) {
        tokens.remove(i);
        continue;
      }
      if (token.startsWith("[")) {
        while (!tokens.get(i).endsWith("]")) {
          if (i + 1 >= tokens.size()) {
            throw new IllegalStateException("Illegal definition: Unclosed square bracket");
          }
          tokens.set(i, tokens.get(i) + tokens.get(i + 1));
          tokens.remove(i + 1);
        }
      }
      i++;
    }
    return tokens;
  }

  /**
   * Returns true if the passed in Node satisfies ALL the requirements
   * dictated by tagName, id, classes, etc.  Does not attempt to enforce
   * {@code directDescendant} - since we don't know the larger context.
   */
  public boolean matches(Node node) {
    if (tagName != null && !tagName.equals(node.getTagName()) && !"*".equals(tagName)) {
      return false;
    }
    if (!classes.isEmpty()) {
      Set<String> nodeClasses = ImmutableSet.copyOf(node.getClasses());
      nodeClasses = Sets.filter(nodeClasses, new Predicate<String>() {
        @Override
        public boolean apply(String clazz) {
          return !clazz.trim().isEmpty();
        }
      });
      return nodeClasses.containsAll(classes);
    }
    if (id != null && !id.equals(node.getId())) {
      return false;
    }
    if (!attributes.isEmpty()) {
      return node.getAttributeNames().containsAll(attributes);
    }
    if (!attributeValues.isEmpty()) {
      for (Map.Entry<String, String> entry : attributeValues.entrySet()) {
        if (!node.getAttributeValues(entry.getKey()).contains(entry.getValue())) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean isDirectDescendant() {
    return directDescendant;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (directDescendant) {
      sb.append("> ");
    }
    if (tagName != null) {
      sb.append(tagName);
    }
    for (String clazz : classes) {
      sb.append(".").append(clazz);
    }
    if (id != null) {
      sb.append("#").append(id);
    }
    for (String attribute : attributes) {
      sb.append("[").append(attribute).append("]");
    }
    for (Map.Entry<String, String> attributeEntry : attributeValues.entrySet()) {
      sb.append("[").append(attributeEntry.getKey()).append("=\"").append(attributeEntry.getValue()).append("\"]");
    }
    return sb.toString();
  }
}
