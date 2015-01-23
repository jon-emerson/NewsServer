package com.janknspank.neuralnet;

import java.util.List;

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
    int wordCount = 0;
    boolean word = false;
    int endOfLine = s.length() - 1;

    for (int i = 0; i < s.length(); i++) {
      // if the char is a letter, word = true.
      if (Character.isLetter(s.charAt(i)) && i != endOfLine) {
        word = true;
        // if char isn't a letter and there have been letters before,
        // counter goes up.
      } else if (!Character.isLetter(s.charAt(i)) && word) {
        wordCount++;
        word = false;
        // last word of String; if it doesn't end with a non letter, it
        // wouldn't count without this.
      } else if (Character.isLetter(s.charAt(i)) && i == endOfLine) {
        wordCount++;
      }
    }
    return wordCount;
  }
}