package com.janknspank.server;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.Industry;
import com.janknspank.bizness.Intent;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.PushNotification;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_articles")
public class GetArticlesServlet extends AbstractArticlesServlet {
  // TODO(jonemerson): Make a global threadpool for this.  Or figure out
  // a better way to do asynchronous calls to Mongo DB - Hopefully via Futures.
  private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

  private static final int NUM_RESULTS = 50;

  @Override
  protected Iterable<Article> getArticles(HttpServletRequest req) throws DatabaseSchemaException {
    String featureId = this.getParameter(req, "feature_id");
    String industryCodeId = this.getParameter(req, "industry_code");
    String contacts = this.getParameter(req, "contacts");
    String entityKeyword = this.getParameter(req, "entity_keyword");
    String entityType = this.getParameter(req, "entity_type");
    String intentCode = this.getParameter(req, "intent_code");

    // This is sent on requests that were initiated from a user's engagement
    // with an iOS push notification.  They contain an encoded notification ID
    // and the url ID of the article that was highlighted.  We want to make sure
    // we mark the notification as engaged and we put the mentioned article at
    // the top of the response.
    String notificationBlob = this.getParameter(req, "blob");

    if (notificationBlob != null) {
      return getArticlesForNotification(req, notificationBlob);
    } else  if (featureId != null) {
      return Articles.getArticlesForFeature(
          FeatureId.fromId(Integer.parseInt(featureId)),
          NUM_RESULTS);
    } else if (industryCodeId != null) {
      return Articles.getArticlesForFeature(
          Industry.fromCode(Integer.parseInt(industryCodeId)).getFeatureId(),
          NUM_RESULTS);
    } else if (entityKeyword != null) {
      Entity entity = Entity.newBuilder().setKeyword(entityKeyword).setType(entityType).build();
      return Articles.getArticlesForEntity(entity, NUM_RESULTS);
    } else if (Intent.START_COMPANY.getCode().equals(intentCode)) {
      // TODO: fix this so its the right feature for the user's industries
      return Articles.getArticlesForFeature(
          FeatureId.fromId(20000), NUM_RESULTS);
    }

    User user = getUser(req);
    if ("linked_in".equals(contacts)) {
      return Articles.getArticlesForContacts(user, InterestType.LINKED_IN_CONTACTS, NUM_RESULTS);
    } else if ("address_book".equals(contacts)) {
      return Articles.getArticlesForContacts(user, InterestType.ADDRESS_BOOK_CONTACTS, NUM_RESULTS);
    } else {
      return Articles.getRankedArticles(
          user,
          NeuralNetworkScorer.getInstance(),
          NUM_RESULTS);
    }
  }

  @VisibleForTesting
  static String getNotificationBlobValue(String notificationBlob, String key) {
    for (String component : Splitter.on("!").split(notificationBlob)) {
      if (component.startsWith(key + "(")) {
        if (!component.endsWith(")")) {
          throw new IllegalArgumentException("Bad notification blob");
        }
        return component.substring(key.length() + 1, component.length() - 1);
      }
    }
    return null;
  }

  /**
   * A thread that marks the user's notification as clicked while other article
   * processing is happening in the background.
   */
  private static class UpdateNotificationCallable implements Callable<Void> {
    private final String notificationId;

    UpdateNotificationCallable(String notificationId) {
      this.notificationId = notificationId;
    }

    @Override
    public Void call() throws Exception {
      PushNotification pushNotification = Database.with(PushNotification.class).get(notificationId);
      if (pushNotification != null) {
        Database.update(pushNotification.toBuilder()
            .setClickTime(System.currentTimeMillis())
            .build());
      }
      return null;
    }
  }

  /**
   * A thread that retrieves a specific article.
   */
  private static class GetArticleCallable implements Callable<Article> {
    private final String urlId;

    GetArticleCallable(String urlId) {
      this.urlId = urlId;
    }

    @Override
    public Article call() throws Exception {
      return Database.with(Article.class).get(urlId);
    }
  }

  /**
   * @param notificationBlob The blob from the notification, indicating the URL
   *     ID of the article we highlighted and the notification ID we need to
   *     mark as engaged.
   * @throws DatabaseSchemaException 
   */
  private Iterable<Article> getArticlesForNotification(
      HttpServletRequest req, String notificationBlob) throws DatabaseSchemaException {
    // Asynchronously start updating the notification and retrieving the
    // specific article we need to show.
    // NOTE(jonemerson): Notification blob is currently in the form:
    // uid(XXX)!nid(YYY)
    final String urlId = getNotificationBlobValue(notificationBlob, "uid");
    final String notificationId = getNotificationBlobValue(notificationBlob, "nid");
    Future<Void> updateNotificationFuture = (notificationId == null) ?
        Futures.immediateFuture((Void) null) :
        EXECUTOR.submit(new UpdateNotificationCallable(notificationId));
    Future<Article> articleFuture = (urlId == null) ?
        Futures.immediateFuture((Article) null) :
        EXECUTOR.submit(new GetArticleCallable(urlId));

    // Now get the standard /getArticles stream.
    User user = getUser(req);
    Iterable<Article> rankedArticles = Articles.getRankedArticles(
        user, NeuralNetworkScorer.getInstance(), NUM_RESULTS);

    // OK, now collate the notification's article into the stream!
    try {
      Article article = articleFuture.get();
      if (article != null) {
        return Iterables.concat(ImmutableList.of(article),
            Iterables.filter(rankedArticles, new Predicate<Article>() {
              @Override
              public boolean apply(Article rankedArticle) {
                return !rankedArticle.getUrlId().equals(urlId);
              }
            }));
      }
    } catch (InterruptedException | ExecutionException e) {
      System.out.println("Error fetching article: " + urlId
          + ". Falling back to standard stream.");
      e.printStackTrace();
    } finally {
      try {
        updateNotificationFuture.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    return rankedArticles;
  }
}
