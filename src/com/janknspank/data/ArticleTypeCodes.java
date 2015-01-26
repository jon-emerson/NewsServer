package com.janknspank.data;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.janknspank.proto.Core.ArticleTypeCode;

public class ArticleTypeCodes {
  public static final Map<String, ArticleTypeCode> ARTICLE_CLASSIFICATION_CODE_MAP = Maps.uniqueIndex(
      ImmutableList.of(
          ArticleTypeCode.newBuilder()
              .setCode("prdct")
              .setDescription("Predicts the future")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("ancmt")
              .setDescription("Announcement (e.g. product launch, acquisition, earnings release, funding granted)")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("strtp")
              .setDescription("About startups")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("pdata")
              .setDescription("Presents data (e.g. charts and numbers)")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("teach")
              .setDescription("Teaches how to do something")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("story")
              .setDescription("Tells a story")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("prson")
              .setDescription("About a specific person")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("intrv")
              .setDescription("Interview")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("opnin")
              .setDescription("Opinion")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("humor")
              .setDescription("Humor")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("sappy")
              .setDescription("Sappy / romantic / exaggerated")
              .build(),
          ArticleTypeCode.newBuilder()
              .setCode("waste")
              .setDescription("Waste of time (no practical knowledge gained by reading)")
              .build()),
      new Function<ArticleTypeCode, String>() {
        @Override
        public String apply(ArticleTypeCode ArticleTypeCode) {
          return ArticleTypeCode.getCode();
        }
      });
  
}
