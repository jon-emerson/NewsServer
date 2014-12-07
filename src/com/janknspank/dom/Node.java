package com.janknspank.dom;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Node {
  private final Node parent;
  private final ArrayList<Object> children = new ArrayList<>();
  private final Multimap<String, String> attributes = ArrayListMultimap.create();
  private final String tagName;

  public Node(Node parent, String tagName) {
    this.parent = parent;
    this.tagName = tagName;
  }

  public Node getParent() {
    return parent;
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
   * Finds all Nodes that match a CSS-like specifier.  Currently only works
   * for tag names.
   * TODO(jonemerson): Support IDs and class names.
   */
  private List<Node> findAll(String searchStr, boolean directDescendant) {
    List<Node> result = new ArrayList<Node>();
    String[] searchStrArray = searchStr.split(" ");
    for (int i = 0; i < getChildCount(); i++) {
      if (!isChildTextNode(i)) {
        if (searchStrArray[0].equalsIgnoreCase(getChildNode(i).getTagName())) {
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
        String flattenedText = getChildNode(i).getFlattenedText();
        if (flattenedText.length() > 0) {
          texts.add(flattenedText);
        }
      }
    }
    return Joiner.on(" ").join(texts);
  }
}
