package com.janknspank.dom;

import java.util.Comparator;

/**
 * Comparator for sorting DOM nodes based on their location in the document
 * (nodes that start first are given priority).
 */
public class NodeOffsetComparator implements Comparator<Node> {
  @Override
  public int compare(Node o1, Node o2) {
    return (int) (o1.getStartingOffset() - o2.getStartingOffset());
  }
}
