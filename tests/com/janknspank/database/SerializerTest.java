package com.janknspank.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.janknspank.database.Serializer;
import com.janknspank.database.Validator;
import com.janknspank.proto.Core.Article;

public class SerializerTest {
  private static final List<String> PARAGRAPHS = ImmutableList.of("paragraph1", "p2");
  private static final String COPYRIGHT = "copyright";
  private static final String DESCRIPTION = "description";
  private static final String URL_ID = "id";
  private static final String IMAGE_URL = "http://path.com/to/image.jpg";
  private static final long MODIFIED_TIME = 200000000L;
  private static final long PUBLISHED_TIME = 300000000L;
  private static final String TITLE = "title";
  private static final String TYPE = "article";
  private static final String URL = "http://www.nytimes.com/super/article.html";

  @Test
  public void testSerializer() throws Exception {
    Article.Builder builder = Article.newBuilder();
    builder.clearAuthor(); // Make sure this does NOT get serialized.
    builder.addAllParagraph(PARAGRAPHS);
    builder.setCopyright(COPYRIGHT);
    builder.setDescription(DESCRIPTION);
    builder.setImageUrl(IMAGE_URL);
    builder.setModifiedTime(MODIFIED_TIME);
    builder.setPublishedTime(PUBLISHED_TIME);
    builder.setTitle(TITLE);
    builder.setType(TYPE);
    builder.setUrl(URL);
    builder.setUrlId(URL_ID);
    Article article = builder.build();
    Validator.assertValid(article);

    JSONObject o = Serializer.toJSON(article);
    assertFalse(o.has("author"));
    assertEquals(COPYRIGHT, o.getString("copyright"));
    assertEquals(DESCRIPTION, o.getString("description"));
    assertEquals(IMAGE_URL, o.getString("image_url"));
    assertEquals(MODIFIED_TIME, o.getLong("modified_time"));
    assertEquals(PUBLISHED_TIME, o.getLong("published_time"));
    assertEquals(TITLE, o.getString("title"));
    assertEquals(TYPE, o.getString("type"));
    assertEquals(URL, o.getString("url"));
    assertEquals(URL_ID, o.getString("url_id"));

    // These fields should not exist - They were explicitly marked as server-
    // only.
    assertFalse(o.has("articleBody"));
    assertFalse(o.has("article_body"));
  }
}
