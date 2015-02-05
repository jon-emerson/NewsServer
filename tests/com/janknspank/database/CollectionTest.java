package com.janknspank.database;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleIndustry;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.ArticleKeyword.Source;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;

public class CollectionTest {
  private void assertInvalidType(FieldDescriptor field, Object value) {
    try {
      Collection.validateType(field, value);
    } catch (DatabaseRequestException e) {
      // Expected!
      return;
    }
    fail("Object " + value.toString() + " (" + value.getClass().getCanonicalName() +
        ") should not have been valid for " + field.getFullName());
  }

  @Test
  public void testValidateType() throws Exception {
    Collection.validateType(Database.getFieldDescriptor(Article.class, "url"), "http://moo/");
    Collection.validateType(Database.getFieldDescriptor(Article.class, "industry"),
        ImmutableList.of(ArticleIndustry.newBuilder()
            .setIndustryCodeId(5)
            .setSimilarity(0.2525)
            .build()));
    Collection.validateType(Database.getFieldDescriptor(Article.class, "keyword"),
        ImmutableList.of(ArticleKeyword.newBuilder()
            .setSource(Source.HYPERLINK)
            .setStrength(5)
            .setKeyword("moose")
            .build()));
    Collection.validateType(Database.getFieldDescriptor(Article.class, "word_count"), 500L);
    Collection.validateType(Database.getFieldDescriptor(Article.class, "keyword.keyword"), "moose");
    Collection.validateType(Database.getFieldDescriptor(Article.class, "keyword.source"), Source.HYPERLINK);

    // Should be scaler.
    assertInvalidType(Database.getFieldDescriptor(Article.class, "url"), ImmutableList.of("http://moo/"));

    // Should be repeated.
    assertInvalidType(Database.getFieldDescriptor(Article.class, "industry"),
        ArticleIndustry.newBuilder()
            .setIndustryCodeId(5)
            .setSimilarity(0.2525)
            .build());
    assertInvalidType(Database.getFieldDescriptor(Article.class, "keyword"),
        ArticleKeyword.newBuilder()
            .setSource(Source.HYPERLINK)
            .setStrength(5)
            .setKeyword("moose")
            .build());

    // Should be a scalar number.
    assertInvalidType(Database.getFieldDescriptor(Article.class, "word_count"), ImmutableList.of(500L));
    assertInvalidType(Database.getFieldDescriptor(Article.class, "word_count"), "moose");
    assertInvalidType(Database.getFieldDescriptor(Article.class, "word_count"), Source.HYPERLINK);
    assertInvalidType(Database.getFieldDescriptor(Article.class, "word_count"), this);

    // Should be a scalar string.
    assertInvalidType(Database.getFieldDescriptor(Article.class, "keyword.keyword"),
        ImmutableList.of("moose"));
    assertInvalidType(Database.getFieldDescriptor(Article.class, "keyword.keyword"), 500L);
    assertInvalidType(Database.getFieldDescriptor(Article.class, "keyword.keyword"),
        ArticleKeyword.newBuilder()
            .setSource(Source.HYPERLINK)
            .setStrength(5)
            .setKeyword("moose")
            .build());
    assertInvalidType(Database.getFieldDescriptor(Article.class, "keyword.keyword"), Source.HYPERLINK);

    // Should be a scalar enum of type Source.
    assertInvalidType(Database.getFieldDescriptor(Article.class, "keyword.source"), "hyperlink");
    assertInvalidType(Database.getFieldDescriptor(Article.class, "keyword.source"), Site.FACEBOOK);
    assertInvalidType(Database.getFieldDescriptor(Article.class, "keyword.source"), 1);
    assertInvalidType(Database.getFieldDescriptor(Article.class, "keyword.source"),
        ImmutableList.of(Source.HYPERLINK));
  }
}
