package com.janknspank.data;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.janknspank.proto.Core.ArticleClassification;

public class ArticleClassifications {
  public static final Map<String, ArticleClassification> ARTICLE_CLASSIFICATION_CODE_MAP = Maps.uniqueIndex(
      ImmutableList.of(
          ArticleClassification.newBuilder()
              .setCode("prdct")
              .setDescription("Predicts the future")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("ancmt")
              .setDescription("Announcement (e.g. product launch, acquisition, earnings release, funding granted)")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("pdata")
              .setDescription("Presents data (e.g. charts and numbers)")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("teach")
              .setDescription("Teaches how to do something")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("story")
              .setDescription("Tells a story")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("prson")
              .setDescription("About a specific person")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("intrv")
              .setDescription("Interview")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("opnin")
              .setDescription("Opinion")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("humor")
              .setDescription("Humor")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("sappy")
              .setDescription("Sappy / romantic / exaggerated")
              .build(),
          ArticleClassification.newBuilder()
              .setCode("waste")
              .setDescription("Waste of time (no practical knowledge gained by reading)")
              .build()),
      new Function<ArticleClassification, String>() {
        @Override
        public String apply(ArticleClassification articleClassification) {
          return articleClassification.getCode();
        }
      });
  
}
