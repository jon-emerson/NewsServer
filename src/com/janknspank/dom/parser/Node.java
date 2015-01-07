package com.janknspank.dom.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class Node {
  private final Node parent;
  private final ArrayList<Object> children = new ArrayList<>();
  private final Multimap<String, String> attributes = ArrayListMultimap.create();
  private final String tagName;

  /**
   * The character offset this element began at in the document.
   * E.g. if a page starts with <HTML>, offset would be 0.
   */
  private final long startingOffset;

  public Node(Node parent, String tagName, long startingOffset) {
    this.parent = parent;
    this.tagName = tagName;
    this.startingOffset = startingOffset;
  }

  public Node getParent() {
    return parent;
  }

  public long getStartingOffset() {
    return startingOffset;
  }

  public boolean isChildTextNode(int index) {
    if (index >= children.size()) {
      throw new IndexOutOfBoundsException();
    }
    return children.get(index) instanceof String;
  }

  public Node getChildNode(int index) {
    if (index >= children.size()) {
      throw new IndexOutOfBoundsException();
    }
    if (!(children.get(index) instanceof Node)) {
      throw new IllegalArgumentException("Requested node is a text node");
    }
    return (Node) children.get(index);
  }

  public String getChildText(int index) {
    if (index >= children.size()) {
      throw new IndexOutOfBoundsException();
    }
    if (!(children.get(index) instanceof String)) {
      throw new IllegalArgumentException("Requested node is a tag node");
    }
    return (String) children.get(index);
  }

  public int getChildCount() {
    return children.size();
  }

  public String getTagName() {
    return tagName;
  }

  public void addChildNode(Node child) {
    children.add(child);
  }

  public void addChildText(String text) {
    children.add(text);
  }

  public void addAttribute(String name, String value) {
    attributes.put(name, value);
  }

  public Set<String> getAttributeNames() {
    return attributes.keySet();
  }

  public String getAttributeValue(String name) {
    Collection<String> values = attributes.get(name);
    if (values.size() > 0) {
      return values.iterator().next();
    } else {
      return null;
    }
  }

  public Collection<String> getAttributeValues(String name) {
    return attributes.get(name);
  }

  /**
   * Consolidates neighbor text children into single, trimmed text
   * children.
   */
  public void condenseTextChildren() {
    int i = 0;
    while (i < getChildCount()) {
      if (i < getChildCount() - 1 &&
          isChildTextNode(i) &&
          isChildTextNode(i + 1)) {
        StringBuilder sb = new StringBuilder();
        sb.append(getChildText(i));
        while (i < getChildCount() - 1 && isChildTextNode(i + 1)) {
          sb.append(getChildText(i + 1));
          children.remove(i + 1);
        }
        children.set(i, sb.toString());
      }
      if (isChildTextNode(i)) {
        String trimmedText = getChildText(i).trim();
        if (trimmedText.length() == 0) {
          children.remove(i);
        } else {
          children.set(i, trimmedText);
        }
      }
      i++;
    }
  }

  /**
   * Returns a collection of all the CSS classes that this node has.
   */
  public Iterable<String> getClasses() {
    String rawClasses = getAttributeValue("class");
    if (rawClasses == null) {
      return Collections.emptyList();
    }
    return Splitter.on(" ").split(rawClasses);
  }

  /**
   * Finds all Nodes that match a CSS-like specifier.  Currently works for tag
   * names, classes, and IDs.
   * @param maxNodeCount the maximum number of nodes to return (performance
   *     optimization)
   */
  private List<Node> find(List<Selector> selectors, int maxNodeCount) {
    List<Node> result = new ArrayList<Node>();
    for (int i = 0; i < getChildCount() && result.size() < maxNodeCount; i++) {
      if (!isChildTextNode(i)) {
        if (selectors.get(0).matches(getChildNode(i))) {
          if (selectors.size() == 1) {
            result.add(getChildNode(i));
          } else {
            result.addAll(getChildNode(i).find(
                selectors.subList(1, selectors.size()),
                maxNodeCount - result.size()));
          }
        }

        // Keep digging - Even if this was a match!
        // E.g. selector="p" matches <p><p/></p> twice!
        if (!selectors.get(0).isDirectDescendant()) {
          result.addAll(getChildNode(i).find(selectors, maxNodeCount - result.size()));
        }
      }
    }
    return result;
  }

  public Node findFirst(String searchStr) {
    return Iterables.getFirst(find(Selector.parseSelectors(searchStr), 1), null);
  }

  /**
   * Returns the first node that matches any of the passed selector definitions.
   */
  public Node findFirst(Iterable<String> selectorDefinitionList) {
    for (String selectorDefinition : selectorDefinitionList) {
      Node node = findFirst(selectorDefinition);
      if (node != null) {
        return node;
      }
    }
    return null;
  }

  public List<Node> findAll(String selectorDefinition) {
    return find(Selector.parseSelectors(selectorDefinition), Integer.MAX_VALUE);
  }

  public List<Node> findAll(Iterable<String> selectorDefinitionList) {
    List<Node> nodes = Lists.newArrayList();
    for (String selectorDefinition : selectorDefinitionList) {
      nodes.addAll(findAll(selectorDefinition));
    }
    return nodes;
  }

  /**
   * Returns all the texts that are children of this node, concatenated, with
   * spaces in between them.  Any tags (e.g. <a> hyperlinks) are dropped.
   */
  public String getFlattenedText() {
    List<String> texts = new ArrayList<String>();
    for (int i = 0; i < getChildCount(); i++) {
      if (isChildTextNode(i)) {
        texts.add(getChildText(i));
      } else {
        Node child = getChildNode(i);

        // Ignore scripts and styles - We only want text for humans.
        if (child.getTagName().equalsIgnoreCase("script") ||
            child.getTagName().equalsIgnoreCase("style")) {
          continue;
        }

        String flattenedText = child.getFlattenedText();
        if (flattenedText.length() > 0) {
          texts.add(flattenedText);
        }
      }
    }
    return Joiner.on(" ").join(texts);
  }

  public String getId() {
    return this.getAttributeValue("id");
  }

  /**
   * Returns an HTML/XML interpretation of this node, including its children.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<");
    sb.append(tagName);
    for (String attributeName : attributes.keySet()) {
      for (String attributeValue : attributes.get(attributeName)) {
        sb.append(" ");
        sb.append(attributeName);
        sb.append("=\"");
        sb.append(StringEscapeUtils.escapeHtml4(attributeValue));
        sb.append("\"");
      }
    }

    if (children.size() == 0) {
      sb.append("/>");
      return sb.toString();
    }


    sb.append(">");
    for (Object child : children) {
      if (child instanceof String) {
        sb.append((String) child);
      } else {
        sb.append(((Node) child).toString());
      }
    }

    sb.append("</");
    sb.append(tagName);
    sb.append(">");
    return sb.toString();
  }
}
