package com.janknspank.classifier;

import org.junit.Test;
import static org.junit.Assert.*;

import com.janknspank.proto.ArticleProto.Article;

public class FeatureTest {
  @Test
  public void testOilAndEnergy() throws Exception {
    Feature feature = new VectorFeature(FeatureId.OIL_AND_ENERGY);
    assertEquals(10, feature.getBoost(Article.newBuilder().setUrl(
        "http://www.rigzone.com/news/oil_gas/a/137267"
        + "/Study_Oil_Price_Downturn_Creates_Need_for_Cost_Culture_in_Industry/")));
    assertEquals(0, feature.getBoost(Article.newBuilder().setUrl(
        "http://opinionator.blogs.nytimes.com/2015/03/12/unraveling-the-church-ban-on-gay-sex/")));
  }

  @Test
  public void testArchitectureAndPlanning() throws Exception {
    Feature feature = new VectorFeature(FeatureId.ARCHITECTURE_AND_PLANNING);
    assertEquals(10, feature.getBoost(Article.newBuilder().setUrl(
        "http://www.designboom.com/architecture/francine-houben-mecanoo-interview-12-17-2014/")));
    assertEquals(2, feature.getBoost(Article.newBuilder().setUrl(
        "http://www.designboom.com/art/urban-shapes-sebastian-weiss-03-06-2015/")));
    assertEquals(0, feature.getBoost(Article.newBuilder().setUrl(
        "http://opinionator.blogs.nytimes.com/2015/03/12/unraveling-the-church-ban-on-gay-sex/")));
  }
}
