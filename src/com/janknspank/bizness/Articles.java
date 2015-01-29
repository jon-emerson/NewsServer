package com.janknspank.bizness;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.database.QueryOption.LimitWithOffset;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Core.TrainedArticleIndustry;
import com.janknspank.proto.Core.UserInterest;
import com.janknspank.rank.CompleteArticle;
import com.janknspank.rank.CompleteUser;
import com.janknspank.rank.Scorer;

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

  public static Iterable<Article> getArticlesOnTopic(String topic) throws DatabaseSchemaException {
    Iterable<ArticleKeyword> articleKeywords = getArticleKeywordsForTopics(ImmutableList.of(topic));
    Set<String> articleIds = Sets.newHashSet();
    for (ArticleKeyword articleKeyword : articleKeywords) {
      articleIds.add(articleKeyword.getUrlId());
    }
    return getArticles(articleIds);
  }

  private static Iterable<ArticleKeyword> getArticleKeywords(Iterable<UserInterest> interests)
      throws DatabaseSchemaException {
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
      throws DatabaseSchemaException {
    return Database.with(ArticleKeyword.class).get(
        new QueryOption.WhereEqualsIgnoreCase("keyword", topics),
        new QueryOption.Limit(500));
  }

  /**
   * Gets articles that contain a set of keywords
   * @throws DataInternalException 
   */
  public static Iterable<Article> getArticlesForKeywords(Iterable<String> keywords) 
      throws DatabaseSchemaException {
    Iterable<ArticleKeyword> articleKeywords = getArticleKeywordsForTopics(keywords);
    Set<String> articleIds = Sets.newHashSet();
    for (ArticleKeyword articleKeyword : articleKeywords) {
      articleIds.add(articleKeyword.getUrlId());
    }
    return getArticles(articleIds);
  }

  /**
   * Gets a list of articles tailored specifically to the current user's
   * interests.
   */
  public static Iterable<Article> getArticlesByInterest(Iterable<UserInterest> interests)
      throws DatabaseSchemaException {
    Iterable<ArticleKeyword> articleKeywords = getArticleKeywords(interests);
    Set<String> articleIds = Sets.newHashSet();
    for (ArticleKeyword articleKeyword : articleKeywords) {
      articleIds.add(articleKeyword.getUrlId());
    }
    return getArticles(articleIds);
  }

  public static Iterable<Article> getRankedArticles(String userId, Scorer scorer)
      throws DatabaseSchemaException, ParserException, BiznessException, DatabaseRequestException {
    Map<CompleteArticle, Double> ranks = getCompleteArticlesAndScores(userId, scorer);

    // Sort the articles
    TopList<Article, Double> articles = new TopList<>(ranks.size());
    for (Map.Entry<CompleteArticle, Double> entry : ranks.entrySet()) {
      articles.add(entry.getKey().getArticle(), entry.getValue());
    }

    return articles.getKeys();
  }

  public static Map<CompleteArticle, Double> getCompleteArticlesAndScores(String userId, Scorer scorer)
      throws DatabaseSchemaException, ParserException, BiznessException, DatabaseRequestException {
    //NeuralNetworkScorer neuralNetworkRank = NeuralNetworkScorer.getInstance();
    CompleteUser completeUser = new CompleteUser(userId);
    // TODO: replace this with getArticles(UserIndustries.getIndustries(userId))
    Iterable<Article> articles = getArticlesByInterest(UserInterests.getInterests(userId));
    Map<CompleteArticle, Double> ranks = new HashMap<>();

    CompleteArticle completeArticle;
    for (Article article : articles) {
      completeArticle = new CompleteArticle(article);
      ranks.put(completeArticle, scorer.getScore(completeUser, completeArticle));
    }

    return ranks;
  }

  /**
   * Returns articles with the given IDs, ordered by publish time, if they
   * exist. If they don't exist, no error is thrown.
   */
  public static Iterable<Article> getArticles(Iterable<String> urlIds) throws DatabaseSchemaException {
    return Database.with(Article.class).get(
        new QueryOption.WhereEquals("url_id", urlIds),
        new QueryOption.DescendingSort("published_time"));
  }

  public static Article getArticle(String urlId) throws DatabaseSchemaException {
    return Database.with(Article.class).get(urlId);
  }

  public static Iterable<Article> getPageOfArticles(int limit, int offset)
      throws DatabaseSchemaException {
    return Database.with(Article.class).get(new LimitWithOffset(limit, offset));
  }

  /**
   * Returns a random article
   */
  public static Article getRandomArticle() throws DatabaseSchemaException {
    return Database.with(Article.class).getFirst(
        new QueryOption.AscendingSort("rand()"));
  }

  /**
   * Returns a random untrained article
   */
  public static Article getRandomUntrainedArticle() throws DatabaseSchemaException {
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
