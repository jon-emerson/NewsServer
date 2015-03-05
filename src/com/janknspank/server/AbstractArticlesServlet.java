package com.janknspank.server;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
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

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException, RequestException, BiznessException {
    JSONObject response = createSuccessResponse();

    Iterable<Article> articles = getArticles(req);
    JSONArray articlesJson = new JSONArray();
    for (Article article : articles) {
      articlesJson.put(serialize(article, getUserKeywordSet(getUser(req))));
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

  private JSONObject serialize(Article article, Set<String> userKeywordSet) {
    JSONObject articleJson = Serializer.toJSON(article);
    List<String> paragraphs = article.getParagraphList();
    articleJson.put("first_paragraphs", toJsonArray(
        paragraphs.subList(0, Math.min(1, paragraphs.size()))));
    articleJson.put("first_3_paragraphs", toJsonArray(
        paragraphs.subList(0, Math.min(3, paragraphs.size()))));
    articleJson.put("native_reader_enabled", isNativeReaderEnabled(article));
    articleJson.put("keyword", Serializer.toJSON(getArticleKeywords(article, userKeywordSet)));
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
