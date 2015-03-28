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

    // Kinda B.S. that we need "keyword.entity.keyword" here.  Wish there was
    // a way to say "index if we're in this table but not in other tables".
    // Maybe indexes should be specified at the table level rather than on the
    // fields?
    assertEquals(
        ImmutableSet.of(
            "feature.feature_id", "keyword.entity.keyword", "keyword.keyword", "published_time"),
        indexes);
  }

  /**
   * Tests that protos with primary key fields have their QueryOption.Where
   * clauses properly converted to _ids when they're addressing said primary
   * key fields.
   */
  @Test
  public void testGetFieldName() throws Exception {
    MongoCollection<Article> articleCollection =
        (MongoCollection<Article>) Database.with(Article.class);
    assertEquals("url", articleCollection.getFieldName(new QueryOption.WhereNotNull("url")));
    assertEquals("_id", articleCollection.getFieldName(new QueryOption.WhereNotNull("url_id")));
    assertEquals("feature.feature_id", articleCollection.getFieldName(
        new QueryOption.WhereNotNull("feature.feature_id")));
    assertEquals("keyword.entity.keyword", articleCollection.getFieldName(
        new QueryOption.WhereNotNull("keyword.entity.keyword")));
    assertEquals("keyword.entity._id", articleCollection.getFieldName(
        new QueryOption.WhereNotNull("keyword.entity.id")));
  }
}
