package com.janknspank;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class TopListTest {
  @Test
  public void testTopList() throws Exception {
    TopList list = new TopList(5);
    list.add("z", -1);
    list.add("a", 10);
    list.add("b", 1);
    list.add("c", 100);
    list.add("d", 2);
    list.add("e", 10000);
    list.add("f", 3);
    list.add("g", 100000);
    list.add("h", 4);

    List<String> topKeys = list.getKeys();
    assertEquals(5, topKeys.size());
    assertEquals("g", topKeys.get(0));
    assertEquals(100000, list.getValue(topKeys.get(0)));
    assertEquals("e", topKeys.get(1));
    assertEquals(10000, list.getValue(topKeys.get(1)));
    assertEquals("c", topKeys.get(2));
    assertEquals(100, list.getValue(topKeys.get(2)));
    assertEquals("a", topKeys.get(3));
    assertEquals(10, list.getValue(topKeys.get(3)));
    assertEquals("h", topKeys.get(4));
    assertEquals(4, list.getValue(topKeys.get(4)));
  }
}
