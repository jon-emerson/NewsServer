package com.janknspank.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.CoreProto.Entity.Source;

public class DatabaseTest {
  @Test
  public void test() throws Exception {
    assertEquals(24, Database.getStringLength(Article.class, "url_id"));
    assertEquals(50, Database.getStringLength(ArticleKeyword.class, "keyword"));

    try {
      Database.getStringLength(ArticleKeyword.class, "advanced_monkey_sentiment");
    } catch (Exception e) {
      // Hooray, failure!
      return;
    }
    fail("Should have failed on getStringLength() for non-existent field");
  }

  @Test
  public void testGetFieldDescriptor() throws Exception {
    FieldDescriptor field = Database.getFieldDescriptor(Article.class, "keyword");
    assertEquals(JavaType.MESSAGE, field.getJavaType());

    field = Database.getFieldDescriptor(Article.class, "keyword.source");
    assertEquals(JavaType.ENUM, field.getJavaType());
  }

  private <T extends Message> void assertObjectNotValidForField(
      Class<T> clazz, String fieldName, Object value) {
    try {
      Database.assertObjectValidForField(clazz, fieldName, value);
    } catch (DatabaseRequestException e) {
      return; // Good!  It failed!
    }
    fail("Object should not have been valid: " + clazz.getSimpleName() + "." + fieldName);
  }

  @Test
  public void testAssertObjectValidForField() throws Exception {
    Database.assertObjectValidForField(Article.class, "url_id",
        "012345678901234567890123");
    Database.assertObjectValidForField(Article.class, "paragraph",
        "paragraph goes here yo!");
    Database.assertObjectValidForField(Article.class, "keyword.keyword",
        "hello world");
    Database.assertObjectValidForField(Article.class, "keyword.source",
        Source.ANGELLIST);
    Database.assertObjectValidForField(Article.class, "industry.industry_code_id", 32);
    Database.assertObjectValidForField(Article.class, "keyword",
        ArticleKeyword.getDefaultInstance());

    assertObjectNotValidForField(Article.class, "url_id",
        "0123456789012345"); // Too short.
    assertObjectNotValidForField(Article.class, "url_id",
        "0123456789012345678901234567890123456789"); // Too long.
    assertObjectNotValidForField(Article.class, "paragraph", null);
    assertObjectNotValidForField(Article.class, "paragraph", ImmutableList.of("String"));
    assertObjectNotValidForField(Article.class, "keyword.keyword",
        Source.ANGELLIST);
    assertObjectNotValidForField(Article.class, "keyword.source",
        "angellist");
    assertObjectNotValidForField(Article.class, "industry.industry_code_id", Long.MIN_VALUE);
    assertObjectNotValidForField(Article.class, "keyword", Article.getDefaultInstance());
  }
}
