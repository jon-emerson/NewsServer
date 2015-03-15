package com.janknspank.server;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.EntityType;
import com.janknspank.common.TopList;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;

public abstract class AbstractArticlesServlet extends StandardServlet {
  private static final Set<Integer> SHOW_IMAGE_OFFSETS = ImmutableSet.of(
      1, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
      73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149,
      151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227,
      229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307,
      311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389,
      397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467,
      479, 487, 491, 499);
  protected abstract Iterable<Article> getArticles(HttpServletRequest req)
      throws BiznessException, DatabaseSchemaException, DatabaseRequestException, RequestException;

  /**
   * Returns true if we have rights to show this article's content natively
   * in the mobile web application.
   */
  static boolean isNativeReaderEnabled(Article article) {
    // TODO(jonemerson): Trigger based on partnerships for launch.  But for
    // now, enable it for TechCrunch and TheVerge, only for testing purposes.
    return article.getUrl().startsWith("http://www.t");
  }

  /**
   * Transforms the passed Article iterable so that the first article tends to
   * have a good image associated with it.  For now, we don't know much about
   * good images vs. bad images, so we just make sure the first article has an
   * image (if possible).
   */
  private Iterable<Article> putImageArticleFirst(Iterable<Article> articles) {
    Article firstImageArticle = Iterables.find(articles, new Predicate<Article>() {
      @Override
      public boolean apply(Article article) {
        return (article.hasImageUrl());
      }
    });
    return Iterables.concat(ImmutableList.of(firstImageArticle),
        Iterables.filter(articles, new Predicate<Article>() {
      @Override
      public boolean apply(Article article) {
        return article != firstImageArticle;
      }
    }));
  }

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException, RequestException, BiznessException {
    JSONObject response = createSuccessResponse();

    Iterable<Article> articles = putImageArticleFirst(getArticles(req));
    JSONArray articlesJson = new JSONArray();
    int i = 1;
    for (Article article : articles) {
      articlesJson.put(serialize(article, getUserKeywordSet(getUser(req)), i++));
    }
    response.put("articles", articlesJson);
    return response;
  }

  private Set<String> getUserKeywordSet(User user) {
    Set<String> userKeywordSet = Sets.newHashSet();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == InterestType.ENTITY) {
        userKeywordSet.add(interest.getEntity().getKeyword().toLowerCase());
      }
    }
    return userKeywordSet;
  }

  private JSONObject serialize(Article article, Set<String> userKeywordSet, int offset) {
    JSONObject articleJson = Serializer.toJSON(article);
    List<String> paragraphs = article.getParagraphList();
    articleJson.put("first_paragraphs", toJsonArray(
        paragraphs.subList(0, Math.min(1, paragraphs.size()))));
    articleJson.put("first_3_paragraphs", toJsonArray(
        paragraphs.subList(0, Math.min(3, paragraphs.size()))));
    articleJson.put("native_reader_enabled", isNativeReaderEnabled(article));
    articleJson.put("keyword", Serializer.toJSON(getArticleKeywords(article, userKeywordSet)));
    if (SHOW_IMAGE_OFFSETS.contains(offset)) {
      articleJson.put("show_image", true);
    }

    // Replace the published time with the crawl time, since people often just
    // give a date for a publish time, so without this, the clients are showing
    // midnight as most articles' ages.
    articleJson.put("published_time", Long.toString(Articles.getPublishedTime(article)));

    return articleJson;
  }

  /**
   * Returns the top 2 keywords for an article, as tuned to the current user.
   */
  private Iterable<ArticleKeyword> getArticleKeywords(Article article, Set<String> userKeywordSet) {
    TopList<ArticleKeyword, Integer> topUserKeywords = new TopList<>(2);
    TopList<ArticleKeyword, Integer> topNonUserKeyword = new TopList<>(1);
    for (ArticleKeyword keyword : article.getKeywordList()) {
      if (userKeywordSet.contains(keyword.getKeyword().toLowerCase())) {
        topUserKeywords.add(keyword, keyword.getStrength());
      } else if (keyword.getKeyword().length() < 15
          && !EntityType.fromValue(keyword.getType()).isA(EntityType.PERSON)
          && !EntityType.fromValue(keyword.getType()).isA(EntityType.PLACE)) {
        // Only include small-ish keywords because the super long ones are often
        // crap.
        topNonUserKeyword.add(keyword, keyword.getStrength());
      }
    }
    return Iterables.limit(Iterables.concat(topUserKeywords, topNonUserKeyword), 2);
  }

  public static JSONArray toJsonArray(List<String> strings) {
    JSONArray array = new JSONArray();
    for (String string : strings) {
      array.put(string);
    }
    return array;
  }
}
