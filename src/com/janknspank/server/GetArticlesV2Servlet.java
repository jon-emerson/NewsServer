package com.janknspank.server;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ExpressionProto.Expression;
import com.janknspank.proto.ExpressionProto.ExpressionArticle;

public abstract class GetArticlesV2Servlet extends StandardServlet {
  private static final int[] EXPRESSION_LOCATIONS = new int[] { 2, 5, 9, 14, 20, 27, 35, 44 };
  protected abstract Iterable<Article> getArticles(HttpServletRequest req)
      throws BiznessException, DatabaseSchemaException, DatabaseRequestException, RequestException;

  protected ListenableFuture<Iterable<ExpressionArticle>> getExpressionArticlesFuture(
      HttpServletRequest req) {
    // TODO Auto-generated method stub
    return null;
  }

  protected ListenableFuture<Iterable<Expression>> getExpressionsFuture(
      HttpServletRequest req) {
    return Futures.immediateFuture((Iterable<Expression>) ImmutableList.of(
        Expression.newBuilder()
            .setId(GuidFactory.generate())
            .setShortString("Videos")
            .setLongString("I like to watch videos on my phone.")
            .setDescription("If you agree with this expression, we'll add more videos "
                + "to your Spotter stream")
            .addRelevantToFeatureId(FeatureId.ANIMATION.getId()).build()));
  }

  /**
   * Mixes together a collection of main stream articles with a collection of
   * expression articles that the user's opted-in to receiving.  The expression
   * articles are shown first as long as they were introduced to the user's
   * stream before any of the main stream articles.  Which will put them first
   * on the user's first use every time, but then they'll gradually fall down
   * the list as the day goes on.
   */
  private JSONArray intermixSerializedArticlesAndExpressions(
      Iterable<Article> mainArticles,
      Iterable<Expression> expressions,
      final Iterable<ExpressionArticle> expressionArticles) {
    // Sort all the expression articles by their introduction time, newest
    // articles first.  Use a priority queue so we can just pull the newest
    // ones off first without doing many comparisons.
    PriorityQueue<ExpressionArticle> expressionArticleQueue = new PriorityQueue<ExpressionArticle>(
        Iterables.size(expressionArticles),
        new Comparator<ExpressionArticle>() {
          @Override
          public int compare(ExpressionArticle ea1, ExpressionArticle ea2) {
            return -Long.compare(ea1.getPresentationTime(), ea2.getPresentationTime());
          }
        });
    Iterables.addAll(expressionArticleQueue, expressionArticles);

    // Now, do an O(N) merge of the two article lists by pulling from either
    // data set depending on which one has the newest article.
    List<Article> intermixedArticles = Lists.newArrayList();
    for (Article mainArticle : mainArticles) {
      while (!expressionArticleQueue.isEmpty()
          && (expressionArticleQueue.peek().getPresentationTime()
              > Articles.getPublishedTime(mainArticle))) {
        intermixedArticles.add(expressionArticleQueue.poll().getArticle());
      }
      intermixedArticles.add(mainArticle);
    }
    while (!expressionArticleQueue.isEmpty()) {
      intermixedArticles.add(expressionArticleQueue.poll().getArticle());
    }

    //JSONArray articlesJsonArray = ArticleSerializer.serialize(
    //    getArticles(req), getUser(req), includeLinkedInContacts, includeAddressBookContacts);
    //JSONArray 
    //return intermixedArray;
    return null;
  }

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException, RequestException, BiznessException {
    boolean isSnippetVersion = getClientVersion(req).atLeast("0.5.7");
    ListenableFuture<Iterable<Expression>> expressionsFuture = isSnippetVersion
        ? this.getExpressionsFuture(req)
        : Futures.immediateFuture((Iterable<Expression>) ImmutableList.<Expression>of());
    ListenableFuture<Iterable<ExpressionArticle>> expressionArticlesFuture = isSnippetVersion
        ? this.getExpressionArticlesFuture(req)
        : Futures.immediateFuture(
            (Iterable<ExpressionArticle>) ImmutableList.<ExpressionArticle>of());

    String contactsParameter = getParameter(req, "contacts");
    boolean includeLinkedInContacts = "linked_in".equals(contactsParameter);
    boolean includeAddressBookContacts = "address_book".equals(contactsParameter);

    JSONObject response = createSuccessResponse();
//    if (isSnippetVersion) {
//      // Intermix articles and expressions.
//      JSONArray intermix = new JSONArray();
//      for (int i = 0; i < articlesJsonArray.length(); i++) {
//        intermix.put(articlesJsonArray.getJSONObject(i));
//        if (i == 1) {
//          JSONObject expressionJsonObject = Serializer.toJSON(expressionsFuture.get());
//          expressionJsonObject.put("type", "expression");
//          intermix.put(expressionJsonObject);
//        }
//      }
//      response.put("data", intermix);
//    } else {
//      // Old skool.
//      response.put("articles", articlesJsonArray);
//    }
    return response;
  }
}
