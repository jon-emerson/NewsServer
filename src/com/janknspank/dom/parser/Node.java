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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class Node {
  private static final String TEXT_LINES_LINE_BREAK_MARKER = "<%-- LINE_BREAK --%>";
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

  public boolean hasChildNodes() {
    for (int i = 0; i < children.size(); i++) {
      if (!isChildTextNode(i)) {
        return true;
      }
    }
    return false;
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

  public boolean hasAttribute(String name) {
    return getAttributeValue(name) != null;
  }

  public Collection<String> getAttributeValues(String name) {
    return attributes.get(name);
  }

  /**
   * Consolidates neighboring text children.
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
        children.set(i, getChildText(i).replaceAll("\\s+", " "));
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
    StringBuilder sb = new StringBuilder();
    getFlattenedText(sb);
    return sb.toString().trim();
  }

  private void getFlattenedText(StringBuilder sb) {
    for (int i = 0; i < getChildCount(); i++) {
      if (isChildTextNode(i)) {
        sb.append(getChildText(i));
      } else {
        // Ignore scripts and styles - We only want text for humans.
        Node child = getChildNode(i);
        if (!child.getTagName().equalsIgnoreCase("script") &&
            !child.getTagName().equalsIgnoreCase("style")) {
          child.getFlattenedText(sb);
        }
      }
    }
  }

  /**
   * Returns a list of all the paragraph-like strings in this list.  Basically,
   * this is .getFlattenedText, but <br/>-aware too.
   */
  public List<String> getTextLines() {
    StringBuilder sb = new StringBuilder();
    buildTextLinesStringBuffer(sb);

    ImmutableList.Builder<String> textLinesBuilder = ImmutableList.builder();
    for (String rawLine : Splitter.on(TEXT_LINES_LINE_BREAK_MARKER).split(sb)) {
      String line = rawLine.trim();
      if (line.length() > 0) {
        textLinesBuilder.add(line);
      }
    }
    return textLinesBuilder.build();
  }

  private void buildTextLinesStringBuffer(StringBuilder sb) {
    for (int i = 0; i < getChildCount(); i++) {
      if (isChildTextNode(i)) {
        sb.append(getChildText(i));
      } else {
        // Ignore scripts and styles - We only want text for humans.
        Node child = getChildNode(i);
        String tagName = child.getTagName();
        if (tagName.equalsIgnoreCase("script") || tagName.equalsIgnoreCase("style")) {
          // Ignore.
        } else if (tagName.equalsIgnoreCase("br")
            || tagName.equalsIgnoreCase("p")
            || tagName.equalsIgnoreCase("h1")
            || tagName.equalsIgnoreCase("h2")
            || tagName.equalsIgnoreCase("h3")) {
          sb.append(TEXT_LINES_LINE_BREAK_MARKER);
          child.buildTextLinesStringBuffer(sb);
        } else {
          child.buildTextLinesStringBuffer(sb);
        }
      }
    }
  }

  public String getId() {
    return this.getAttributeValue("id");
  }

  /**
   * Returns an HTML/XML interpretation of this node, including its children.
   */
  @Override
  public String toString() {
    return toString(0);
  }

  /**
   * Prints out all the attributes on this node, plus all this node's children.
   */
  protected String toString(int depth) {
    StringBuilder sb = new StringBuilder();
    sb.append(Joiner.on("").join(Iterables.limit(Iterables.cycle("  "), depth)))
        .append("<")
        .append(tagName);
    for (String attributeName : attributes.keySet()) {
      for (String attributeValue : attributes.get(attributeName)) {
        sb.append(" ")
            .append(attributeName)
            .append("=\"")
            .append(StringEscapeUtils.escapeHtml4(attributeValue))
            .append("\"");
      }
    }

    if (children.size() == 0) {
      sb.append("/>\n");
      return sb.toString();
    }

    sb.append(">\n");
    for (Object child : children) {
      if (child instanceof String) {
        if ("script".equalsIgnoreCase(tagName) ||
            "style".equalsIgnoreCase(tagName)) {
          sb.append(Joiner.on("").join(Iterables.limit(Iterables.cycle("  "), depth + 1)));
          sb.append("<!-- truncated -->\n");
        } else if (!((String) child).trim().isEmpty()) {
          sb.append(Joiner.on("").join(Iterables.limit(Iterables.cycle("  "), depth + 1)));
          sb.append((String) child).append("\n");
        }
      } else {
        sb.append(((Node) child).toString(depth + 1));
      }
    }

    sb.append(Joiner.on("").join(Iterables.limit(Iterables.cycle("  "), depth)))
        .append("</")
        .append(tagName)
        .append(">\n");
    return sb.toString();
  }
}
