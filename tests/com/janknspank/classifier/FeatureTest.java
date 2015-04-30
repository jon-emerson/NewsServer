package com.janknspank.classifier;

import org.junit.Test;
import static org.junit.Assert.*;

import com.janknspank.proto.ArticleProto.Article;

public class FeatureTest {
  @Test
  public void testOilAndEnergy() throws Exception {
    assertEquals(2, Feature.getBoost(
        FeatureId.OIL_AND_ENERGY,
        Article.newBuilder().setUrl("http://www.rigzone.com/news/oil_gas/a/137267"
            + "/Study_Oil_Price_Downturn_Creates_Need_for_Cost_Culture_in_Industry/")));
    assertEquals(0, Feature.getBoost(
        FeatureId.OIL_AND_ENERGY,
        Article.newBuilder().setUrl("http://opinionator.blogs.nytimes.com/2015/03/12/"
            + "unraveling-the-church-ban-on-gay-sex/")));
  }

  @Test
  public void testArchitectureAndPlanning() throws Exception {
    assertEquals(4, Feature.getBoost(
        FeatureId.ARCHITECTURE_AND_PLANNING,
        Article.newBuilder().setUrl("http://www.designboom.com/architecture/"
            + "francine-houben-mecanoo-interview-12-17-2014/")));
    assertEquals(4, Feature.getBoost(
        FeatureId.ARTS,
        Article.newBuilder().setUrl("http://www.designboom.com/art/"
            + "urban-shapes-sebastian-weiss-03-06-2015/")));
    assertEquals(1, Feature.getBoost(
        FeatureId.ARTS,
        Article.newBuilder().setUrl("http://www.designboom.com/architecture/"
            + "francine-houben-mecanoo-interview-12-17-2014/")));
    assertEquals(0, Feature.getBoost(
        FeatureId.ARCHITECTURE_AND_PLANNING,
        Article.newBuilder().setUrl("http://www.designboom.com/art/"
            + "urban-shapes-sebastian-weiss-03-06-2015/")));
    assertEquals(0, Feature.getBoost(
        FeatureId.ARCHITECTURE_AND_PLANNING,
        Article.newBuilder().setUrl("http://opinionator.blogs.nytimes.com/2015/03/12/"
            + "unraveling-the-church-ban-on-gay-sex/")));
  }
}
