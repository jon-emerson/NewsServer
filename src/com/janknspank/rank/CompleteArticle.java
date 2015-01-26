package com.janknspank.rank;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.janknspank.classifier.IndustryClassifier;
import com.janknspank.data.ArticleFacebookEngagements;
import com.janknspank.data.ArticleKeywords;
import com.janknspank.data.Articles;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.TrainedArticleClassifications;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleFacebookEngagement;
import com.janknspank.proto.Core.ArticleIndustryClassification;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Core.TrainedArticleClassification;
import com.janknspank.proto.Core.UserInterest;

/**
 * Convenience class that combines ArticleKeyworks
 * ArticleTypeCodes
 * ArticleIndustries
 * and TrainedArticleRelevance
 * @author tomch
 *
 */
public class CompleteArticle {
  private Article article;
  private List<ArticleKeyword> keywords;
  private List<ArticleIndustryClassification> industryClassifications;
  //private List<TrainedArticleIndustry> industries;
  private List<TrainedArticleClassification> trainedContentClassifications;
  private List<ArticleFacebookEngagement> facebookEngagements;
  private double likeVelocity;
  private double shareVelocity;
  
  public CompleteArticle(String urlId) throws DataInternalException, IOException {
    article = Articles.getArticle(urlId);
    initForArticle(article);
  }
  
  public CompleteArticle(Article article) throws DataInternalException, IOException {
    this.article = article;
    initForArticle(article);
  }
  
  private void initForArticle(Article article) throws DataInternalException, IOException {
    String urlId = article.getUrlId();
    String url = article.getUrl();
    keywords = ArticleKeywords.get(ImmutableList.of(article));
    industryClassifications = IndustryClassifier.getInstance().classify(article);
    //industries = TrainedArticleIndustries.getFromArticle(urlId);
    trainedContentClassifications = TrainedArticleClassifications.getFromArticle(urlId);
    facebookEngagements = ArticleFacebookEngagements.getLatest(url, 2);
  }
  
  // TODO: make getters for things as needed
  public List<ArticleFacebookEngagement> getFacebookEngagements() {
    return facebookEngagements;
  }
  
  public ArticleFacebookEngagement getLatestFacebookEngagement() {
    // TODO: test to make sure this is the correct order
    if (facebookEngagements != null && facebookEngagements.size() > 0) {
      return facebookEngagements.get(0);      
    }
    else {
      return null;
    }
  }
  
  public double getLikeVelocity() {
    System.out.println("TODO: finish CompleteArticle.getLikeVelocity()");
    if (facebookEngagements == null || facebookEngagements.isEmpty()) {
      return 0;
    }
    else if (facebookEngagements.size() == 1) {
      //TODO: use the published data to get the velocity
      return 0;
      
    } else {
      // TODO: use the time interval between the last two
      // engagement checks
      return 0;
    }
  }
  
  public int getAgeInMillis() {
    //TODO: test to make sure this works
    return (int) (System.currentTimeMillis() - article.getPublishedTime()); 
  }
  
  public boolean containsInterest(UserInterest interest) {
    for (ArticleKeyword keyword : keywords) {
      if (keyword.getKeyword().equals(interest.getKeyword())) {
        return true;
      }
    }
    return false;
  }
  
  public boolean containsKeyword(String keyword) {
    for (ArticleKeyword articleKeyword : keywords) {
      if (articleKeyword.getKeyword().equals(keyword)) {
        return true;
      }
    }
    return false;
  }
  
  // Utility methods
  // TODO: Compute at crawl and save to Article
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