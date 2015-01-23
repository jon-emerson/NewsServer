package com.janknspank.neuralnet;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.janknspank.data.ArticleKeywords;
import com.janknspank.data.Articles;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.TrainedArticleClassifications;
import com.janknspank.data.TrainedArticleIndustries;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Core.TrainedArticleClassification;
import com.janknspank.proto.Core.TrainedArticleIndustry;
import com.janknspank.proto.Core.UserInterest;

/**
 * Convenience class that combines ArticleKeyworks
 * ArticleClassifications
 * ArticleIndustries
 * and TrainedArticleRelevance
 * @author tomch
 *
 */
public class CompleteArticle {
  private Article article;
  private List<ArticleKeyword> keywords;
  private List<TrainedArticleClassification> classifications;
  private List<TrainedArticleIndustry> industries;
  
  public CompleteArticle(String urlId) throws DataInternalException {
    article = Articles.getArticle(urlId);
    keywords = ArticleKeywords.get(ImmutableList.of(article));
    classifications = TrainedArticleClassifications.getFromArticle(urlId);
    industries = TrainedArticleIndustries.getFromArticle(urlId);
  }
  
  //TODO: make getters for everything
  
  public boolean containsInterest(UserInterest interest) {
    for (ArticleKeyword keyword : keywords) {
      if (keyword.getKeyword().equals(interest.getKeyword())) {
        return true;
      }
    }
    return false;
  }
  
  public int wordCount() {
    int paragraphCount = article.getParagraphCount();
    int wordCount = 0;
    for (int i = 0; i < paragraphCount; i++) {
      wordCount += CompleteArticle.countWords(article.getParagraph(i));
    }
    return wordCount;
  }
  
  public static int countWords(String s){
    Pattern pattern = Pattern.compile("[\\s]+");
    Matcher matcher = pattern.matcher(s);
    int words = 0;
    while (matcher.find()) {
      words++;
    }
    return words + 1;
  }
}