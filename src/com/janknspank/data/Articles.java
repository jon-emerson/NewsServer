package com.janknspank.data;

import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.common.TopList;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.neuralnet.CompleteUser;
import com.janknspank.neuralnet.NeuralNetworkDriver;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Core.TrainedArticleIndustry;
import com.janknspank.proto.Core.UserInterest;

/**
 * Helper class that manages storing and retrieving Article objects from the
 * database.
 */
public class Articles {
  public static final int MAX_TITLE_LENGTH =
      Database.with(Article.class).getStringLength("title");
  public static final int MAX_PARAGRAPH_LENGTH =
      Database.with(Article.class).getStringLength("paragraph");
  public static final int MAX_DESCRIPTION_LENGTH =
      Database.with(Article.class).getStringLength("description");

  public static Iterable<Article> getArticlesOnTopic(String topic) throws DataInternalException {
    Iterable<ArticleKeyword> articleKeywords = getArticleKeywordsForTopics(ImmutableList.of(topic));
    Set<String> articleIds = Sets.newHashSet();
    for (ArticleKeyword articleKeyword : articleKeywords) {
      articleIds.add(articleKeyword.getUrlId());
    }
    return getArticles(articleIds);
  }

  private static Iterable<ArticleKeyword> getArticleKeywords(Iterable<UserInterest> interests)
      throws DataInternalException {
    // Filter out location interests for now, until we can better prioritize them.
    interests = Iterables.filter(interests, new Predicate<UserInterest>() {
      @Override
      public boolean apply(UserInterest userInterest) {
        return !UserInterests.TYPE_LOCATION.equals(userInterest.getType());
      }
    });
    return getArticleKeywordsForTopics(Iterables.transform(interests,
        new Function<UserInterest, String>() {
          @Override
          public String apply(UserInterest interest) {
            return interest.getKeyword();
          }
    }));
  }

  private static Iterable<ArticleKeyword> getArticleKeywordsForTopics(Iterable<String> topics)
      throws DataInternalException {
    return Database.with(ArticleKeyword.class).get(
        new QueryOption.WhereEquals("keyword", topics),
        new QueryOption.Limit(500));
  }

  /**
   * Gets a list of articles tailored specifically to the current user's
   * interests.
   */
  public static Iterable<Article> getArticlesByInterest(Iterable<UserInterest> interests)
      throws DataInternalException {
    Iterable<ArticleKeyword> articleKeywords = getArticleKeywords(interests);
    Set<String> articleIds = Sets.newHashSet();
    for (ArticleKeyword articleKeyword : articleKeywords) {
      articleIds.add(articleKeyword.getUrlId());
    }
    return getArticles(articleIds);
  }

  public static Iterable<Article> getArticlesRankedByNeuralNetwork(String userId)
      throws DataInternalException, ParserException {
    NeuralNetworkDriver neuralNetworkDriver = NeuralNetworkDriver.getInstance();
    CompleteUser completeUser = new CompleteUser(userId);

    // Sort the articles by rank
    Iterable<Article> articles = getArticlesByInterest(UserInterests.getInterests(userId));
    TopList<Article, Double> topArticles = new TopList<Article, Double>(Iterables.size(articles));
    for (Article article : articles) {
      topArticles.add(article, neuralNetworkDriver.getRank(article, completeUser));
    }
    return topArticles;
  }

  /**
   * Returns articles with the given IDs, ordered by publish time, if they
   * exist. If they don't exist, no error is thrown.
   */
  public static Iterable<Article> getArticles(Iterable<String> urlIds)
      throws DataInternalException {
    return Database.with(Article.class).get(
        new QueryOption.WhereEquals("url_id", urlIds),
        new QueryOption.DescendingSort("published_time"));
  }
  
  public static Article getArticle(String urlId) 
      throws DataInternalException {
    return Database.with(Article.class).get(urlId);
  }

  /**
   * Returns a random article
   */
  public static Article getRandomArticle() throws DataInternalException {
    return Database.with(Article.class).getFirst(
        new QueryOption.Sort("rand()"));
  }

  /**
   * Returns a random untrained article
   */
  public static Article getRandomUntrainedArticle() throws DataInternalException {
    Article article;
    Iterable<TrainedArticleIndustry> taggedIndustries;
    do {
      article = Articles.getRandomArticle();
      taggedIndustries = TrainedArticleIndustries.getFromArticle(article.getUrlId());
    } while (!Iterables.isEmpty(taggedIndustries));
    return article;
  }
  
  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.with(Article.class).createTable();

//    Article.Builder builder = Article.newBuilder();
//    String id = "id" + System.currentTimeMillis();
//    builder.setAuthor("author");
//    builder.setArticleBody("body");
//    builder.setCopyright("copyright");
//    builder.setDescription("desc");
//    builder.setId(id);
//    builder.setImageUrl("image urllllz");
//    builder.setModifiedTime(500L);
//    builder.setPublishedTime(7300L);
//    builder.setTitle("title");
//    builder.setType("article");
//    builder.setUrl("http://www.nytimes.com/super/article.html");
//    Article article = builder.build();
//    Database.insert(article);
//
//    Article articleRefetched = Database.get(id, Article.class);
//    Printer.print(articleRefetched);
//
//    Article.Builder articleBuilder = articleRefetched.toBuilder();
//    articleBuilder.setArticleBody("new body");
//    articleBuilder.setDescription("new description");
//    articleBuilder.setTitle("new title");
//    Database.update(articleBuilder.build());
//
//    articleRefetched = Database.get(id, Article.class);
//    Printer.print(articleRefetched);
  }
}
