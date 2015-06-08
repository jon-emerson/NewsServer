package com.janknspank.server;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.api.client.util.Strings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.ExploreTopics;
import com.janknspank.bizness.TimeRankingStrategy.EntityStreamStrategy;
import com.janknspank.bizness.TimeRankingStrategy.IndustryStreamStrategy;
import com.janknspank.bizness.Users;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.Entity.Source;
import com.janknspank.proto.NotificationsProto.Notification;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.DiversificationPass;

@AuthenticationRequired
@ServletMapping(urlPattern = "/v1/get_articles")
public class GetArticlesServlet extends StandardServlet {
  // TODO(jonemerson): Make a global threadpool for this.  Or figure out
  // a better way to do asynchronous calls to Mongo DB - Hopefully via Futures.
  private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

  /**
   * Use this if your form parameter get relatively large, e.g. with
   * exclude_url_ids.
   */
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException, RequestException, BiznessException {
    return doGetInternal(req, resp);
  }

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, DatabaseRequestException, RequestException, BiznessException {
    JSONObject response = createSuccessResponse();

    // If the user queried for a specific entity, go get it, so we can make
    // sure it's included in the serialization of the articles.
    String entityId = this.getParameter(req, "entity_id");
    ListenableFuture<Iterable<Entity>> queriedEntityFuture = (entityId != null)
        ? Database.with(Entity.class).getFuture(
            new QueryOption.WhereEquals("id", entityId))
        : null;

    // Get articles.
    String contactsParameter = getParameter(req, "contacts");
    boolean includeLinkedInContacts = "linked_in".equals(contactsParameter);
    boolean includeAddressBookContacts = "address_book".equals(contactsParameter);
    Iterable<Article> articles = getArticles(req);

    // Get the queried entity and industry code.
    Entity queriedEntity = null;
    if (queriedEntityFuture != null) {
      try {
        queriedEntity = Iterables.getFirst(queriedEntityFuture.get(), null);
      } catch (InterruptedException | ExecutionException e) {
        // This shouldn't be a user-visible problem.  But if it happens, we
        // should find out why, and fix it.
        e.printStackTrace();
      }
    }
    String queriedIndustryCodeStr = this.getParameter(req, "industry_code");
    Integer queriedIndustryCode = queriedIndustryCodeStr == null
        ? null : NumberUtils.toInt(queriedIndustryCodeStr, 0);

    // Let's serialize!
    User user = getUser(req);
    response.put("articles", ArticleSerializer.serialize(
        Iterables.limit(articles, Articles.NUM_RESULTS - 1),
        user, includeLinkedInContacts, includeAddressBookContacts, queriedEntity));
    response.put("explore_topics",
        Serializer.toJSON(ExploreTopics.get(articles, user, queriedEntity, queriedIndustryCode)));
    if (Iterables.size(articles) == Articles.NUM_RESULTS) {
      response.put("next_page", getNextPageParameters(req, articles));
    }
    return response;
  }

  private String getNextPageParameters(HttpServletRequest req, Iterable<Article> articles) {
    List<NameValuePair> params = Lists.newArrayList();

    // Add existing parameters, verbatim, so that we can support more filters
    // in the future without worrying about updating this.  Exclude
    // authentication and other parameters we'll update more specifically below.
    Map<String, String[]> parameterMap = req.getParameterMap();
    Set<String> specialParameters = ImmutableSet.of(
        "session_key", "exclude_url_ids", "page_num");
    for (String key : parameterMap.keySet()) {
      if (!specialParameters.contains(key)) {
        for (String value : parameterMap.get(key)) {
          params.add(new BasicNameValuePair(key, value));
        }
      }
    }

    // Add exclude_url_ids.
    Set<String> urlIds = Sets.newHashSet();
    for (Article article : Iterables.limit(articles, Articles.NUM_RESULTS - 1)) {
      urlIds.add(article.getUrlId());
    }
    if (parameterMap.containsKey("exclude_url_ids")) {
      for (String parameterValue : parameterMap.get("exclude_url_ids")) {
        for (String urlId : Splitter.on(",").split(parameterValue)) {
          urlIds.add(urlId);
        }
      }
    }
    params.add(new BasicNameValuePair("exclude_url_ids", Joiner.on(",").join(urlIds)));

    // Add page number and current time, for future logging / analysis.
    params.add(new BasicNameValuePair("page_num",
        Integer.toString(NumberUtils.toInt(getParameter(req, "page_num"), 1) + 1)));
    if (!parameterMap.containsKey("first_page_time")) {
      params.add(new BasicNameValuePair("first_page_time",
          Long.toString(System.currentTimeMillis())));
    }

    return URLEncodedUtils.format(params, Charsets.UTF_8);
  }

  private Set<String> getExcludeUrlIdSet(HttpServletRequest req) {
    String excludeUrlIdsParameter = this.getParameter(req, "exclude_url_ids");
    if (!Strings.isNullOrEmpty(excludeUrlIdsParameter)) {
      return ImmutableSet.copyOf(Splitter.on(",").split(excludeUrlIdsParameter));
    } else {
      return ImmutableSet.<String>of();
    }
  }

  private Iterable<Article> getArticles(HttpServletRequest req)
      throws DatabaseSchemaException, NumberFormatException, BiznessException, RequestException {
    String industryCodeId = this.getParameter(req, "industry_code");
    String contacts = this.getParameter(req, "contacts");
    String entityId = this.getParameter(req, "entity_id");
    String entityKeyword = this.getParameter(req, "entity_keyword");
    String entityType = this.getParameter(req, "entity_type");
    Set<String> excludeUrlIdSet = getExcludeUrlIdSet(req);

    // This is sent on requests that were initiated from a user's engagement
    // with an iOS push notification.  They contain an encoded notification ID
    // and the url ID of the article that was highlighted.  We want to make sure
    // we mark the notification as engaged and we put the mentioned article at
    // the top of the response.
    String notificationBlob = this.getParameter(req, "blob");
    if (notificationBlob != null) {
      return getArticlesForNotification(req, notificationBlob);
    }

    // Mark that the user's using the app, as long as it's from a real user
    // action.  Occasions when we have robots hitting our system:
    // * New Relic monitoring
    // * Client pre-caching of the main stream when it receives a notification
    User user = getUser(req);
    boolean isRequestFromRobotUser =
        (this.getParameter(req, "blob") != null) || (this.getParameter(req, "newrelic") != null);
    Future<User> updateLast5AppUseTimesFuture =
        isRequestFromRobotUser
            ? Futures.immediateFuture(user)
            : Users.updateLast5AppUseTimes(user, getRemoteAddr(req));

    // Based on the user's query, return articles that match.
    try {
      if (industryCodeId != null) {
        int industryCode = NumberUtils.toInt(industryCodeId, 0);
        if (FeatureId.fromId(industryCode) == null) {
          throw new RequestException("Unknown industry code: \"" + industryCodeId + "\"");
        }
        return Articles.getStream(
            user.toBuilder()
                .clearInterest()
                .addInterest(Interest.newBuilder()
                    .setType(InterestType.INDUSTRY)
                    .setIndustryCode(Integer.parseInt(industryCodeId))
                    .build())
                .build(),
            new IndustryStreamStrategy(),
            new DiversificationPass.IndustryStreamPass(),
            excludeUrlIdSet);
      } else if (entityId != null) {
        Entity.Builder entityBuilder = Entity.newBuilder().setId(entityId);
        Entity entity = null;
        if (entityType == null || entityKeyword == null) {
          System.out.println("Warning: "
              + "Inefficient query - Please include type + keyword for entity queries");
          entity = Database.with(Entity.class).get(entityId);
        }
        entityBuilder.setType(entity != null ? entity.getType() : entityType);
        entityBuilder.setKeyword(entity != null ? entity.getKeyword() : entityKeyword);
        return Articles.getStream(
            user.toBuilder()
                .clearInterest()
                .addInterest(Interest.newBuilder()
                    .setType(InterestType.ENTITY)
                    .setEntity(entityBuilder.build())
                    .build())
                .build(),
            new EntityStreamStrategy(),
            new DiversificationPass.NoOpPass(),
            excludeUrlIdSet);
      } else if (!Strings.isNullOrEmpty(entityKeyword)) {
        return Articles.getStream(
            user.toBuilder()
                .clearInterest()
                .addInterest(Interest.newBuilder()
                    .setType(InterestType.ENTITY)
                    .setEntity(Entity.newBuilder()
                        .setSource(Source.USER)
                        .setKeyword(entityKeyword))
                    .build())
                .build(),
            new EntityStreamStrategy(),
            new DiversificationPass.NoOpPass(),
            excludeUrlIdSet);
      } else if ("linked_in".equals(contacts)) {
        return Articles.getArticlesForLinkedInContacts(user, excludeUrlIdSet);
      } else if ("address_book".equals(contacts)) {
        return Articles.getArticlesForAddressBookContacts(user, excludeUrlIdSet);
      } else {
        return Articles.getMainStream(user, excludeUrlIdSet);
      }
    } finally {
      try {
        updateLast5AppUseTimesFuture.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
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
      Notification pushNotification = Database.with(Notification.class).get(notificationId);
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
   * @throws BiznessException 
   */
  private Iterable<Article> getArticlesForNotification(
      HttpServletRequest req, String notificationBlob)
      throws DatabaseSchemaException, BiznessException, RequestException {
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
    Iterable<Article> rankedArticles = Articles.getMainStream(user);

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
