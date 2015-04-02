package com.janknspank.rank;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;

public class DeduperTest {
  private static final Article MICHELLE_PHAN_ARTICLE_1 = Article.newBuilder()
      .setUrl("http://mashable.com/2015/03/31/michelle-phan-icon/")
      .setTitle("YouTube megastar Michelle Phan launches new lifestyle network")
      .addAllDedupingStems(ImmutableList.of(
          "mega", "phan", "mich", "netw", "laun", "yout", "life"))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(20000)
          .setSimilarity(0.8109678032963561))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30000)
          .setSimilarity(0))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30001)
          .setSimilarity(1))
     .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30002)
          .setSimilarity(0))
     .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40000)
          .setSimilarity(0.461887745488102))
     .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40001)
          .setSimilarity(0.4663175498610544))
     .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40002)
          .setSimilarity(0.4606438740292229))
     .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10509)
          .setSimilarity(0.896188887675416))
     .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10501)
          .setSimilarity(0.8911693756535627))
     .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10560)
          .setSimilarity(0.852700833697388))
     .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10502)
          .setSimilarity(0.7807963015031111))
     .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10503)
          .setSimilarity(0.7219234873495146))
     .build();
  private static final Article MICHELLE_PHAN_ARTICLE_2 = Article.newBuilder()
      .setUrl("http://www.theverge.com/2015/3/31/8318875/michelle-phan-icon-video-network")
      .setTitle("YouTube star Michelle Phan has officially outgrown YouTube")
      .addAllDedupingStems(ImmutableList.of(
          "phan", "mich", "star", "yout", "outg", "offi"))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(20000)
          .setSimilarity(0.769240128581727))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30000)
          .setSimilarity(0))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30001)
          .setSimilarity(0.8))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30002)
          .setSimilarity(0))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40000)
          .setSimilarity(0.8052751748910376))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40001)
          .setSimilarity(0.8989790374726779))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40002)
          .setSimilarity(0.6437205522300026))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10513)
          .setSimilarity(0.9905757291032511))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10511)
          .setSimilarity(0.9163410049598835))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10506)
          .setSimilarity(0.9025674998766593))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10501)
          .setSimilarity(0.8602699332446949))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10548)
          .setSimilarity(0.819527870851339))
      .build();
  private static final Article BASEMENT_BOY_ARTICLE = Article.newBuilder()
      .setUrl("http://abcnews.go.com/US/wireStory/"
          + "key-hearing-couple-case-detroit-boy-basement-29946681")
      .setTitle("Key Hearing for Couple in Case of Detroit Boy in Basement")
      .addAllDedupingStems(ImmutableList.of(
          "detr", "coup", "boy", "key", "hear", "case", "base"))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(20000)
          .setSimilarity(0.3593677532923046))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30000)
          .setSimilarity(0))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30001)
          .setSimilarity(0))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30002)
          .setSimilarity(0))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40000)
          .setSimilarity(0.46353095757181734))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40001)
          .setSimilarity(0.46005033781739213))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40002)
          .setSimilarity(0.4592657652585296))
      .build();
  private static final Article FARMING_GROWTH_MARKETS_ARTICLE = Article.newBuilder()
      .setUrl("http://www.farms.com/news/growth-markets-89775.aspx")
      .setTitle("Growth Markets")
      .addAllDedupingStems(ImmutableList.of(
          "grow", "mark"))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(20000)
          .setSimilarity(0.01599892986997185))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30000)
          .setSimilarity(0))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30001)
          .setSimilarity(0))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(30002)
          .setSimilarity(0))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40000)
          .setSimilarity(0.00782204675117926))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40001)
          .setSimilarity(0.01267864632594518))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(40002)
          .setSimilarity(0.010204045801079338))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10540)
          .setSimilarity(0.9945368948894608))
      .addFeature(ArticleFeature.newBuilder()
          .setFeatureId(10522)
          .setSimilarity(0.990056791859896))
      .build();

  @Test
  public void test() throws Exception {
    assertTrue(Deduper.isDupe(MICHELLE_PHAN_ARTICLE_1, MICHELLE_PHAN_ARTICLE_2));
    assertTrue(Deduper.isDupe(MICHELLE_PHAN_ARTICLE_2, MICHELLE_PHAN_ARTICLE_1));
    assertFalse(Deduper.isDupe(MICHELLE_PHAN_ARTICLE_1, BASEMENT_BOY_ARTICLE));
    assertFalse(Deduper.isDupe(MICHELLE_PHAN_ARTICLE_2, BASEMENT_BOY_ARTICLE));
    assertFalse(Deduper.isDupe(BASEMENT_BOY_ARTICLE, MICHELLE_PHAN_ARTICLE_1));
    assertFalse(Deduper.isDupe(BASEMENT_BOY_ARTICLE, MICHELLE_PHAN_ARTICLE_2));
    assertFalse(Deduper.isDupe(MICHELLE_PHAN_ARTICLE_1, FARMING_GROWTH_MARKETS_ARTICLE));
    assertFalse(Deduper.isDupe(MICHELLE_PHAN_ARTICLE_2, FARMING_GROWTH_MARKETS_ARTICLE));
    assertFalse(Deduper.isDupe(FARMING_GROWTH_MARKETS_ARTICLE, MICHELLE_PHAN_ARTICLE_1));
    assertFalse(Deduper.isDupe(FARMING_GROWTH_MARKETS_ARTICLE, BASEMENT_BOY_ARTICLE));
    assertFalse(Deduper.isDupe(BASEMENT_BOY_ARTICLE, FARMING_GROWTH_MARKETS_ARTICLE));
    assertFalse(Deduper.isDupe(FARMING_GROWTH_MARKETS_ARTICLE, MICHELLE_PHAN_ARTICLE_1));
  }
}
