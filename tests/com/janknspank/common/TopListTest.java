package com.janknspank.common;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.janknspank.proto.Core.Article;

public class TopListTest {
  @Test
  public void testTopIntegers() throws Exception {
    TopList<String, Integer> list = new TopList<>(5);
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
    assertEquals((Integer) 100000, list.getValue(topKeys.get(0)));
    assertEquals("e", topKeys.get(1));
    assertEquals((Integer) 10000, list.getValue(topKeys.get(1)));
    assertEquals("c", topKeys.get(2));
    assertEquals((Integer) 100, list.getValue(topKeys.get(2)));
    assertEquals("a", topKeys.get(3));
    assertEquals((Integer) 10, list.getValue(topKeys.get(3)));
    assertEquals("h", topKeys.get(4));
    assertEquals((Integer) 4, list.getValue(topKeys.get(4)));
  }
  
  @Test
  public void testTopArticlesByRank() {
    TopList<Article, Double> topList = new TopList<>(3);
    topList.add(Article.newBuilder().setTitle("a").build(), 500.0);
    topList.add(Article.newBuilder().setTitle("b").build(), 600.0);
    topList.add(Article.newBuilder().setTitle("c").build(), 100000.0);
    topList.add(Article.newBuilder().setTitle("d").build(), -2.0);
    topList.add(Article.newBuilder().setTitle("e").build(), 50000.0);
    List<Article> topArticles = topList.getKeys();
    assertEquals(3, topArticles.size());
    assertEquals("c", topArticles.get(0).getTitle());
    assertEquals("e", topArticles.get(1).getTitle());
    assertEquals("b", topArticles.get(2).getTitle());
  }
}
