package com.janknspank.dom.parser;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

  public String getAttributeValue(String name) {
    Collection<String> values = attributes.get(name);
    if (values.size() > 0) {
      return values.iterator().next();
    } else {
      return null;
    }
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
  private Iterable<String> getClasses() {
    String rawClasses = getAttributeValue("class");
    if (rawClasses == null) {
      return Collections.emptyList();
    }
    return Splitter.on(" ").split(rawClasses);
  }

  /**
   * Returns true if this Node matches the passed search string.  E.g. if
   * this node is a div, and the search string is "div", this returns true.
   * Also, if this node has class "hello", and the search string is ".hello",
   * this also returns true.  Lastly, "#foo" matches id="foo", "p.foo"
   * matches paragraphs with class="foo", and "p#foo" matches paragraphs with
   * id="foo".
   */
  private boolean matchesSearchStr(String searchStr) {
    // Make sure parsing happened before this and we're only comparing one
    // attribute against this node.
    if (searchStr.contains(" ")) {
      throw new RuntimeException("Search string may only be a single delimiter");
    }
    if (searchStr.contains("#") && searchStr.contains(".")) {
      // TODO(jonemerson): Implement this! :)
      throw new RuntimeException("Can't specify both class and ID yet");
    }

    if (searchStr.startsWith(".")) {
      return Iterables.contains(getClasses(), searchStr.substring(1));
    } else if (searchStr.startsWith("#")) {
      return searchStr.substring(1).equals(getId());
    } else if (searchStr.contains(".")) {
      String searchTag = searchStr.substring(0, searchStr.indexOf("."));
      String searchClassName = searchStr.substring(searchStr.indexOf(".") + 1);
      return tagName.equalsIgnoreCase(searchTag) &&
          Iterables.contains(getClasses(), searchClassName);
    } else if (searchStr.contains("#")) {
      String searchTag = searchStr.substring(0, searchStr.indexOf("#"));
      String searchId = searchStr.substring(searchStr.indexOf("#") + 1);
      return tagName.equalsIgnoreCase(searchTag) && searchId.equals(getId());
    } else {
      // NOTE(jonemerson): Maybe some day be strict about case here.
      return tagName.equalsIgnoreCase(searchStr);
    }
  }

  /**
   * Finds all Nodes that match a CSS-like specifier.  Currently works for tag
   * names, classes, and IDs.
   */
  private List<Node> findAll(String searchStr, boolean directDescendant) {
    List<Node> result = new ArrayList<Node>();
    String[] searchStrArray = searchStr.split(" ");
    for (int i = 0; i < getChildCount(); i++) {
      if (!isChildTextNode(i)) {
        if (getChildNode(i).matchesSearchStr(searchStrArray[0])) {
          if (searchStrArray.length == 1) {
            result.add(getChildNode(i));
          } else {
            if (searchStrArray.length > 2 && ">".equals(searchStrArray[1])) {
              result.addAll(getChildNode(i).findAll(
                  searchStr.substring(searchStr.indexOf(">") + 2), true));
            } else {
              result.addAll(getChildNode(i).findAll(
                  searchStr.substring(searchStr.indexOf(" ") + 1), false));
            }
          }
        } else if (!directDescendant) {
          result.addAll(getChildNode(i).findAll(searchStr));
        }
      }
    }
    return result;
  }

  public List<Node> findAll(String searchStr) {
    return findAll(searchStr, false);
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
