package com.janknspank.crawler;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.janknspank.bizness.EntityType;
import com.janknspank.nlp.KeywordCanonicalizer;
import com.janknspank.proto.ArticleProto.ArticleKeyword;

public class KeywordCanonicalizerTest {
  @Test
  public void testCanonicalize() {
    List<ArticleKeyword> keywords = ImmutableList.of(
        ArticleKeyword.newBuilder()
            .setKeyword("Jackson Smith")
            .setStrength(1)
            .setType(EntityType.PLACE.toString()) // Deliberately wrong.
            .build(),
        ArticleKeyword.newBuilder()
            .setKeyword("Smith")
            .setStrength(3) // This strength should fix the merged keyword type.
            .setType(EntityType.PERSON.toString())
            .build(),
        ArticleKeyword.newBuilder()
            .setKeyword("Mr. Smith")
            .setStrength(1)
            .setType(EntityType.PERSON.toString())
            .build(),
        ArticleKeyword.newBuilder()
            .setKeyword("Mr Smith")
            .setStrength(1)
            .setType(EntityType.PERSON.toString())
            .build(),
        ArticleKeyword.newBuilder()
            .setKeyword("IBM")
            .setStrength(1)
            .setType(EntityType.ORGANIZATION.toString())
            .build());
    Iterable<ArticleKeyword> canonicalizedKeywords = KeywordCanonicalizer.canonicalize(keywords);
    assertEquals(2, Iterables.size(canonicalizedKeywords));

    Map<String, ArticleKeyword> keywordMap = Maps.newHashMap();
    for (ArticleKeyword keyword : canonicalizedKeywords) {
      keywordMap.put(keyword.getKeyword(), keyword);
    }
    assertEquals(6, keywordMap.get("Jackson Smith").getStrength());
    assertEquals(EntityType.PERSON,
        EntityType.fromValue(keywordMap.get("Jackson Smith").getType()));
    assertEquals(1, keywordMap.get("IBM").getStrength());
    assertEquals(EntityType.ORGANIZATION,
        EntityType.fromValue(keywordMap.get("IBM").getType()));
  }
}
