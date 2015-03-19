package com.janknspank.utils;

import com.janknspank.bizness.SocialEngagements;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.rank.Personas;

public class FunTimes {
  public static void main(String args[]) throws Exception {
    for (Persona persona : Personas.getPersonaMap().values()) {
      TopList<String, Double> goodUrlTopList = new TopList<>(20);
      for (Article article : Database.with(Article.class).get(
          new QueryOption.WhereEquals("url", persona.getBadUrlList()))) {
        goodUrlTopList.add(article.getUrl(),
            SocialEngagements.getForArticle(article, Site.FACEBOOK).getShareScore());
      }
      System.out.println(persona.getEmail() + ":");
      for (String goodUrl : goodUrlTopList) {
        System.out.println((goodUrlTopList.getValue(goodUrl)) + ": " + goodUrl);
      }
    }
  }
}
