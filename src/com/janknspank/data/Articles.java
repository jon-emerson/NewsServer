package com.janknspank.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.janknspank.dom.parser.ParserException;
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
      Database.getStringLength(Article.class, "title");
  public static final int MAX_PARAGRAPH_LENGTH =
      Database.getStringLength(Article.class, "paragraph");
  public static final int MAX_DESCRIPTION_LENGTH =
      Database.getStringLength(Article.class, "description");
  
  public static Iterable<Article> getArticlesOnTopic(String topic) throws DataInternalException {
    List<ArticleKeyword> articleKeywords = getArticleKeywordsForTopics(ImmutableList.of(topic));
    Set<String> articleIds = Sets.newHashSet();
    for (ArticleKeyword articleKeyword : articleKeywords) {
      articleIds.add(articleKeyword.getUrlId());
    }
    return getArticles(articleIds);
  }

  private static List<ArticleKeyword> getArticleKeywords(Iterable<UserInterest> interests)
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

  private static List<ArticleKeyword> getArticleKeywordsForTopics(Iterable<String> topics)
      throws DataInternalException {
    return Database.getInstance().get(ArticleKeyword.class,
        new QueryOption.WhereEquals("keyword", topics),
        new QueryOption.Limit(500));
  }

  /**
   * Gets a list of articles tailored specifically to the current user's
   * interests.
   */
  public static List<Article> getArticles(List<UserInterest> interests)
      throws DataInternalException {
    List<ArticleKeyword> articleKeywords = getArticleKeywords(interests);
    Set<String> articleIds = Sets.newHashSet();
    for (ArticleKeyword articleKeyword : articleKeywords) {
      articleIds.add(articleKeyword.getUrlId());
    }
    return getArticles(articleIds);
  }
  
  public static List<Article> getArticlesRankedByNeuralNetwork(String userId)
      throws DataInternalException, ParserException {
    NeuralNetworkDriver neuralNetworkDriver = NeuralNetworkDriver.getInstance();
    neuralNetworkDriver.setUser(userId);
    // TODO: replace this with getArticles(UserIndustries.getIndustries(userId))
    List<Article> articles = getArticles(UserInterests.getInterests(userId));
    Map<Article, Double> ranks = new HashMap<Article, Double>();
    
    // Sort the articles by rank
    for (Article article : articles) {
      ranks.put(article, neuralNetworkDriver.getRank(article.getUrlId()));
    }
    
    PriorityQueue<Entry<Article, Double>> pq = new PriorityQueue<Map.Entry<Article,Double>>(
        ranks.size(), new Comparator<Entry<Article, Double>>() {

      @Override
      public int compare(Entry<Article, Double> arg0, Entry<Article, Double> arg1) {
          return arg0.getValue().compareTo(arg1.getValue()) * -1;
      }
    });
    pq.addAll(ranks.entrySet());
    
    List<Article> sortedArticles = new ArrayList<Article>();
    while (!pq.isEmpty()) {
      sortedArticles.add(pq.poll().getKey());
    }
    return sortedArticles;
  }

  /**
   * Returns articles with the given IDs, ordered by publish time, if they
   * exist. If they don't exist, no error is thrown.
   */
  public static List<Article> getArticles(Iterable<String> urlIds)
      throws DataInternalException {
    return Database.getInstance().get(Article.class,
        new QueryOption.WhereEquals("url_id", urlIds),
        new QueryOption.DescendingSort("published_time"));
  }
  
  public static Article getArticle(String urlId) 
      throws DataInternalException {
    return Database.getInstance().get(Article.class, 
        new QueryOption.WhereEquals("url_id", urlId)).get(0);
  }

  /**
   * Returns a random article
   */
  public static Article getRandomArticle() throws DataInternalException {
    return Database.getInstance().getFirst(Article.class,
        new QueryOption.Sort("rand()"));
  }
  
  /**
   * Returns a random untrained article
   */
  public static Article getRandomUntrainedArticle() throws DataInternalException {
    Article art;
    List<TrainedArticleIndustry> taggedIndustries;
    do {
      art = Articles.getRandomArticle();
      taggedIndustries = TrainedArticleIndustries.getFromArticle(art.getUrlId());
    } while (!taggedIndustries.isEmpty());
    return art;
  }
  
  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Database.getInstance().createTable(Article.class);

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
