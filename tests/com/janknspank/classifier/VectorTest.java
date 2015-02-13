package com.janknspank.classifier;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.CoreProto.VectorData;
import com.janknspank.proto.CoreProto.VectorData.WordFrequency;

public class VectorTest {
  /**
   * Basic test that verifies a Vector created from a universe of Articles
   * computes counts correctly.
   */
  @Test
  public void testArticleConstructor() throws Exception {
    Article article1 = Article.newBuilder()
        .addParagraph("Article1 Is Funky")
        .setTitle("Title1")
        .build();
    Article article2 = Article.newBuilder()
        .addParagraph("Article2 Is Funky Funky Xblacklistedx Twiceavich")
        .setTitle("Title2 Titletext")
        .build();
    Article article3 = Article.newBuilder()
        .addParagraph("Article3 Is Is Funky Twiceavich")
        .setTitle("Title3 Xblacklistedx")
        .build();
    Vector universe = new Vector(ImmutableList.of(article1, article2, article3),
        ImmutableSet.of("Xblacklistedx"));
    assertEquals(1, universe.getWordFrequency("Article1"));
    assertEquals(1, universe.getWordFrequency("Article2"));
    assertEquals(1, universe.getWordFrequency("Article3"));
    assertEquals(1, universe.getWordFrequency("Title1"));
    assertEquals(1, universe.getWordFrequency("Title2"));
    assertEquals(1, universe.getWordFrequency("Title3"));
    assertEquals(1, universe.getWordFrequency("Titletext"));
    assertEquals(4, universe.getWordFrequency("Funky"));
    assertEquals(2, universe.getWordFrequency("Twiceavich"));
    assertEquals(0, universe.getWordFrequency("Is")); // Default blacklist.
    assertEquals(0, universe.getWordFrequency("Xblacklistedx")); // Custom blacklist.

    assertEquals(1, universe.getDocumentOccurences("Article1"));
    assertEquals(1, universe.getDocumentOccurences("Article2"));
    assertEquals(1, universe.getDocumentOccurences("Article3"));
    assertEquals(1, universe.getDocumentOccurences("Title1"));
    assertEquals(1, universe.getDocumentOccurences("Title2"));
    assertEquals(1, universe.getDocumentOccurences("Title3"));
    assertEquals(1, universe.getDocumentOccurences("Titletext"));
    assertEquals(3, universe.getDocumentOccurences("Funky"));
    assertEquals(2, universe.getDocumentOccurences("Twiceavich"));
    assertEquals(0, universe.getDocumentOccurences("Is")); // Default blacklist.
    assertEquals(0, universe.getDocumentOccurences("Xblacklistedx")); // Custom blacklist.
  }

  @Test
  public void testGetCosineSimilarity() throws Exception {
    VectorData v1Data = VectorData.newBuilder()
        .addWordFrequency(WordFrequency.newBuilder().setWord("Seahawks").setFrequency(5))
        .addWordFrequency(WordFrequency.newBuilder().setWord("Wilson").setFrequency(4))
        .addWordFrequency(WordFrequency.newBuilder().setWord("Lynch").setFrequency(3))
        .addWordFrequency(WordFrequency.newBuilder().setWord("Sherman").setFrequency(2))
        .addWordFrequency(WordFrequency.newBuilder().setWord("Chancellor").setFrequency(1))
        .build();
    Vector v1 = new Vector(v1Data);
    VectorData v2Data = VectorData.newBuilder()
        .addWordFrequency(WordFrequency.newBuilder().setWord("Patriots").setFrequency(5))
        .addWordFrequency(WordFrequency.newBuilder().setWord("Brady").setFrequency(4))
        .addWordFrequency(WordFrequency.newBuilder().setWord("Gronkowski").setFrequency(3))
        .addWordFrequency(WordFrequency.newBuilder().setWord("Edelman").setFrequency(2))
        .addWordFrequency(WordFrequency.newBuilder().setWord("Blount").setFrequency(1))
        .build();
    Vector v2 = new Vector(v2Data);
    Vector universe = new Vector(v1Data.toBuilder()
        .addAllWordFrequency(v2Data.getWordFrequencyList())
        .build());
    assertEquals(0, v1.getCosineSimilarity(universe, v2), 0.000001 /* epsilon */);
    assertEquals(1, v1.getCosineSimilarity(universe, v1), 0.000001 /* epsilon */);

    // Create a universe vector, so that we can make sure both individual
    // vectors are marginally related to it.
    assertEquals(0.7, universe.getCosineSimilarity(universe, v1), 0.1 /* epsilon */);
    assertEquals(0.7, universe.getCosineSimilarity(universe, v2), 0.1 /* epsilon */);

    // Verify commutativity.
    assertEquals(v2.getCosineSimilarity(universe, v1), v1.getCosineSimilarity(universe, v2),
        0.000001 /* epsilon */);
  }

  /**
   * This sample data (and projected results) is pulled from
   * http://en.wikipedia.org/wiki/Tf%E2%80%93idf in the "Example of tf-idf"
   * section.
   */
  private ImmutableList<ArticleOrBuilder> testWikipediaTestArticleSet() {
    ArticleOrBuilder article1 = Article.newBuilder()
        .addParagraph("Thisx Isx Axe Axe Sample");
    ArticleOrBuilder article2 = Article.newBuilder()
        .addParagraph("Thisx Isx Anotherx Anotherx Examplex Examplex Examplex");
    return ImmutableList.of(article1, article2);
  }

  /**
   * Tests that we match up to the Tf-Idf example given on Wikipedia, here:
   * http://en.wikipedia.org/wiki/Tf%E2%80%93idf
   */
  @Test
  public void testGetTfIdf() throws Exception {
    ImmutableList<ArticleOrBuilder> articles = testWikipediaTestArticleSet();
    Vector v2 = Vector.fromArticle(articles.get(1));
    Vector universe = new Vector(articles);
    Map<String, Double> tfIdf = v2.getTfIdf(universe);
    assertEquals(0, tfIdf.get("Thisx"), 0.0001 /* epsilon */);
    assertEquals(2.0794385, tfIdf.get("Examplex"), 0.0001 /* epsilon */);
  }

  /**
   * Verifies that saving a vector to disk and then reading it back is lossless.
   * (Implicit in this test is that converting vectors to protos is lossless
   * too... A little weird, but that's ultimately valuable to test too.)
   */
  @Test
  public void testFileIo() throws Exception {
    ImmutableList<ArticleOrBuilder> articles = testWikipediaTestArticleSet();
    Vector universe = new Vector(articles);
    File temp = File.createTempFile("testFileIo", ".vector"); 
    universe.writeToFile(temp);
    assertEquals(universe.toVectorData(), Vector.fromFile(temp).toVectorData());
    temp.delete();
  }
}
