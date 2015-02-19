package com.janknspank.database;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Descriptors.Descriptor;
import com.janknspank.proto.ArticleProto.Article;

public class MongoCollectionTest {
  @Test
  public void testGetIndexes() throws Exception {
    Descriptor articleDescriptor =
        Database.getDefaultInstance(Article.class).getDescriptorForType();
    Set<String> indexes =
        Sets.newHashSet(MongoCollection.getIndexes(articleDescriptor.getFields()));
    assertEquals(ImmutableSet.of("feature.feature_id", "keyword.keyword", "published_time"),
        indexes);
  }
}
