package com.janknspank.crawler;

import java.util.Set;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.Sets;

public class JsoupUtils {
  public static Element selectFirst(Element rootElement, Iterable<String> cssQueries) {
    for (String cssQuery : cssQueries) {
      Element firstEl = rootElement.select(cssQuery).first();
      if (firstEl != null) {
        return firstEl;
      }
    }
    return null;
  }

  public static Elements selectAll(Element rootElement, Iterable<String> cssQueries) {
    Set<Element> elementSet = Sets.newHashSet();
    for (String cssQuery : cssQueries) {
      elementSet.addAll(rootElement.select(cssQuery));
    }
    Elements elements = new Elements();
    elements.addAll(elementSet);
    return elements;
  }
}
