package com.janknspank.interpreter;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.janknspank.data.ArticleKeywords;
import com.janknspank.proto.Core.ArticleKeyword;

public class KeywordCanonicalizerTest {
  private static final String URL_ID = "url_id_2";

  @Test
  public void testCanonicalize() {
    List<ArticleKeyword> keywords = ImmutableList.of(
        ArticleKeyword.newBuilder()
            .setKeyword("Jackson Smith")
            .setStrength(1)
            .setType(ArticleKeywords.TYPE_LOCATION) // Deliberately wrong.
            .setUrlId(URL_ID)
            .build(),
        ArticleKeyword.newBuilder()
            .setKeyword("Smith")
            .setStrength(3) // This strength should fix the merged keyword type.
            .setType(ArticleKeywords.TYPE_PERSON)
            .setUrlId(URL_ID)
            .build(),
        ArticleKeyword.newBuilder()
            .setKeyword("Mr. Smith")
            .setStrength(1)
            .setType(ArticleKeywords.TYPE_PERSON)
            .setUrlId(URL_ID)
            .build(),
        ArticleKeyword.newBuilder()
            .setKeyword("Mr Smith")
            .setStrength(1)
            .setType(ArticleKeywords.TYPE_PERSON)
            .setUrlId(URL_ID)
            .build(),
        ArticleKeyword.newBuilder()
            .setKeyword("IBM")
            .setStrength(1)
            .setType(ArticleKeywords.TYPE_ORGANIZATION)
            .setUrlId(URL_ID)
            .build());
    Iterable<ArticleKeyword> canonicalizedKeywords = KeywordCanonicalizer.canonicalize(keywords);
    assertEquals(2, Iterables.size(canonicalizedKeywords));

    Map<String, ArticleKeyword> keywordMap = Maps.newHashMap();
    for (ArticleKeyword keyword : canonicalizedKeywords) {
      keywordMap.put(keyword.getKeyword(), keyword);
    }
    assertEquals(6, keywordMap.get("Jackson Smith").getStrength());
    assertEquals(ArticleKeywords.TYPE_PERSON, keywordMap.get("Jackson Smith").getType());
    assertEquals(1, keywordMap.get("IBM").getStrength());
    assertEquals(ArticleKeywords.TYPE_ORGANIZATION, keywordMap.get("IBM").getType());
  }

}
