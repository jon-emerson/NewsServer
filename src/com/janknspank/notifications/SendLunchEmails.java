package com.janknspank.notifications;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.http.client.utils.URIBuilder;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.google.template.soy.tofu.SoyTofu.Renderer;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.UserInterests;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.Asserts;
import com.janknspank.common.Host;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.NotificationsProto.DeviceType;
import com.janknspank.proto.NotificationsProto.Notification;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.server.ArticleSerializer;
import com.janknspank.server.NewsServlet;
import com.janknspank.server.WelcomeEmailServlet;
import com.janknspank.server.soy.ViewFeedSoy;
import com.sun.mail.smtp.SMTPMessage;

/**
 * Sends people a top-5 list of articles as an email, when it's around
 * noon.
 */
public class SendLunchEmails {
  private static DateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, MMMM d");
  private static final String SPOTTER_AT_LUNCH_CID =
      "spotterAtLunch@spotternews.com";
  private static final String SPOTTER_RED_TAG_LEFT_CID =
      "spotterEmailRedTagLeft@spotternews.com";
  private static final String SPOTTER_RED_TAG_RIGHT_CID =
      "spotterEmailRedTagRight@spotternews.com";
  private static final String SPOTTER_WHITE_TAG_LEFT_CID =
      "spotterEmailWhiteTagLeft@spotternews.com";
  private static final String SPOTTER_WHITE_TAG_RIGHT_CID =
      "spotterEmailWhiteTagRight@spotternews.com";

  public static String getDate() {
    String date = DATE_FORMAT.format(new Date());
    String suffix = "th";
    if (date.endsWith(" 11") || date.endsWith(" 12") || date.endsWith(" 13")) {
      suffix = "th";
    } else if (date.endsWith("1")) {
      suffix = "st";
    } else if (date.endsWith("2")) {
      suffix = "nd";
    } else if (date.endsWith("3")) {
      suffix = "rd";
    }
    return date + suffix;
  }

  public static String getTitle(Iterable<Article> articles, User user) {
    Set<String> userKeywordSet = UserInterests.getUserKeywordSet(
        user, ImmutableSet.<InterestType>of());
    Set<Integer> userIndustryFeatureIdIds = UserInterests.getUserIndustryFeatureIdIds(user);
    TopList<String, Integer> followedKeywords = new TopList<>(100);
    TopList<String, Integer> notFollowedKeywords = new TopList<>(100);
    int articleNumber = 0;
    for (Article article : articles) {
      articleNumber++;
      for (ArticleKeyword keyword : ArticleSerializer.getBestKeywords(
          article, userKeywordSet, userIndustryFeatureIdIds, null)) {
        int additionalValue = Math.max(1, 10 - articleNumber);
        Integer value = followedKeywords.getValue(keyword.getKeyword());
        if (userKeywordSet.contains(keyword.getKeyword().toLowerCase())) {
          followedKeywords.add(keyword.getKeyword(),
              value == null ? additionalValue : value + additionalValue);
        } else {
          notFollowedKeywords.add(keyword.getKeyword(),
              value == null ? additionalValue : value + additionalValue);
        }
      }
    }
    List<String> keywords = ImmutableList.copyOf(
        Iterables.limit(Iterables.concat(followedKeywords, notFollowedKeywords), 4));
    String keywordTeaser = "";
    switch (Iterables.size(keywords)) {
      case 2:
        keywordTeaser = keywords.get(0) + " and " + keywords.get(1);
        break;
      case 3:
        keywordTeaser = keywords.get(0) + ", " + keywords.get(1) + " and " + keywords.get(2);
        break;
      case 4:
        keywordTeaser = keywords.get(0) + ", " + keywords.get(1) + ", " + keywords.get(2)
             + " and more";
        break;
      default:
        keywordTeaser = Iterables.getFirst(articles, Article.getDefaultInstance()).getTitle();
    }
    return "Spotter@Lunch, " + getDate() + ": " + keywordTeaser;
  }

  /**
   * Returns a soy list of the tags to display on the given article.
   */
  private static SoyListData getTags(Article article, Set<String> userKeywordSet,
      Set<Integer> userIndustryFeatureIdIds) {
    List<SoyMapData> list = Lists.newArrayList();
    boolean hasSelected = false;

    // Start with followed entities, then secondary entities.
    for (ArticleKeyword keyword : ArticleSerializer.getBestKeywords(
        article, userKeywordSet, userIndustryFeatureIdIds, null)) {
      boolean selected = userKeywordSet.contains(keyword.getKeyword().toLowerCase());
      list.add(new SoyMapData(
          "selected", selected,
          "keyword", keyword.getKeyword()));
      hasSelected |= selected;
    }

    // If there are no followed entities, then put the industry.  This way, the
    // user always knows why we chose to show this article.
    if (!hasSelected && article.hasReasonIndustryCode()) {
      FeatureId featureId = FeatureId.fromId(article.getReasonIndustryCode());
      if (featureId != null) {
        list.add(0, new SoyMapData(
            "selected", true,
            "keyword", featureId.getTitle()));
      }

      // Show no more than 2 chips.
      if (list.size() > 2) {
        list = list.subList(0, 1);
      }
    }
    return new SoyListData(list);
  }

  public static String getNotificationUrl(User user, Article article, String notificationId) {
    Asserts.assertNotNull(notificationId, "notificationId cannot be null",
        IllegalStateException.class);
    try {
      URIBuilder builder = new URIBuilder("http://www.spotternews.com/clickthrough");
      builder.addParameter("url", article.getUrl());
      builder.addParameter("uid", user.getId());
      builder.addParameter("nid", notificationId);
      return builder.toString();
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return article.getUrl();
    }
  }

  public static SoyListData getArticleSoyList(User user, Iterable<Article> articles,
      @Nullable String notificationId)
      throws DatabaseSchemaException, BiznessException {
    SoyListData list = new SoyListData();
    Set<String> userKeywordSet = UserInterests.getUserKeywordSet(
        user, ImmutableSet.<InterestType>of());
    Set<Integer> userIndustryFeatureIdIds = UserInterests.getUserIndustryFeatureIdIds(user);
    for (Article article : articles) {
      list.add(new SoyMapData(
          "title", article.getTitle(),
          "description", article.getDescription(),
          "url", (notificationId == null)
              ? article.getUrl()
              : getNotificationUrl(user, article, notificationId),
          "tags", getTags(article, userKeywordSet, userIndustryFeatureIdIds),
          "time", ArticleSerializer.getClientDate(article),
          "site", ViewFeedSoy.getDomain(article)));
    }
    return list;
  }

  private static String getHtml(
      Iterable<Article> articles, User user, @Nullable String notificationId)
      throws DatabaseSchemaException, BiznessException {
    SoyTofu soyTofu = NewsServlet.getTofu("lunchemail");
    Renderer renderer = soyTofu.newRenderer(".main");
    String spotterAtLunchImgSrcPlaceholder = "////spotterAtLunchImgSrc////";
    String spotterEmailRedTagLeftImgSrcPlaceholder = "////spotterEmailRedTagLeftImgSrc////";
    String spotterEmailRedTagRightImgSrcPlaceholder = "////spotterEmailRedTagRightImgSrc////";
    String spotterEmailWhiteTagLeftImgSrcPlaceholder = "////spotterEmailWhiteTagLeftImgSrc////";
    String spotterEmailWhiteTagRightImgSrcPlaceholder = "////spotterEmailWhiteTagRightImgSrc////";
    renderer.setData(
        new SoyMapData(
            "title", getTitle(articles, user),
            "articles", getArticleSoyList(user, articles, notificationId),
            "date", getDate(),
            "spotterAtLunchImgSrc", spotterAtLunchImgSrcPlaceholder,
            "spotterEmailRedTagLeftImgSrc", spotterEmailRedTagLeftImgSrcPlaceholder,
            "spotterEmailRedTagRightImgSrc", spotterEmailRedTagRightImgSrcPlaceholder,
            "spotterEmailWhiteTagLeftImgSrc", spotterEmailWhiteTagLeftImgSrcPlaceholder,
            "spotterEmailWhiteTagRightImgSrc", spotterEmailWhiteTagRightImgSrcPlaceholder,
            "unsubscribeLink",
                WelcomeEmailServlet.getUnsubscribeLink(user, false /* relativeUrl */)));

    // For some reason, Soy Templates don't like cid: URLs.  They claim they're
    // invalid and render them as "#zSoyz" instead of doing as they're told.
    // So, let's give them something "valid" and then replace it with the actual
    // MIME-supported value here.
    return renderer.render()
        .replaceAll(spotterAtLunchImgSrcPlaceholder,
            "cid:" + SPOTTER_AT_LUNCH_CID)
        .replaceAll(spotterEmailRedTagLeftImgSrcPlaceholder,
            "cid:" + SPOTTER_RED_TAG_LEFT_CID)
        .replaceAll(spotterEmailRedTagRightImgSrcPlaceholder,
            "cid:" + SPOTTER_RED_TAG_RIGHT_CID)
        .replaceAll(spotterEmailWhiteTagLeftImgSrcPlaceholder,
            "cid:" + SPOTTER_WHITE_TAG_LEFT_CID)
        .replaceAll(spotterEmailWhiteTagRightImgSrcPlaceholder,
            "cid:" + SPOTTER_WHITE_TAG_RIGHT_CID);
  }

  /**
   * Returns the 6 articles that are most relevant to the user.
   */
  public static Iterable<Article> getArticles(User user)
      throws DatabaseSchemaException, BiznessException {
    Iterable<Article> mainStream = Articles.getMainStream(user);
    TopList<Article, Double> top6LastDay = new TopList<>(6);
    long dateInMillisOneDayAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
    long dateInMillis12HoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(12);
    for (Article article : mainStream) {
      long publishedTime = Articles.getPublishedTime(article);
      if (publishedTime > dateInMillisOneDayAgo) {
        // Punish articles from yesterday.  The email should try to be mostly
        // about articles published that day, before noon.
        boolean fromYesterday = publishedTime > dateInMillis12HoursAgo;
        top6LastDay.add(article, fromYesterday ? article.getScore() - 0.1 : article.getScore());
      }
    }
    return top6LastDay;
  }

  /**
   * Stores a row in the MySQL database indicating that we've sent the user
   * an email notification.
   */
  private static Notification createNotification(User user)
      throws DatabaseRequestException, DatabaseSchemaException {
    Notification notification = Notification.newBuilder()
        .setId(GuidFactory.generate())
        .setCreateTime(System.currentTimeMillis())
        .setText("lunch")
        .setDeviceId(user.getEmail())
        .setDeviceType(DeviceType.EMAIL)
        .setHost(Host.get())
        .setUserId(user.getId())
        .build();
    Database.insert(notification);
    return notification;
  }

  private static Message getMessage(Session session, User user, Iterable<Article> articles)
      throws MessagingException, IOException, DatabaseSchemaException, BiznessException,
          DatabaseRequestException {
    SMTPMessage message = new SMTPMessage(session);
    message.setFrom(new InternetAddress("support@spotternews.com", "Spotter News"));
    message.setRecipient(Message.RecipientType.TO,
        new InternetAddress(user.getEmail(), user.getFirstName() + " " + user.getLastName()));
    message.setSubject(getTitle(articles, user));

    MimeMultipart content = new MimeMultipart();

    // HTML version.
    MimeBodyPart mainPart = new MimeBodyPart();
    Notification notification = createNotification(user);
    String html = getHtml(articles, user, notification.getId());
    mainPart.setContent(html, "text/html; charset=utf-8");
    content.addBodyPart(mainPart);

    // Image attachments.
    for (Map.Entry<String, String> imageAttributes : ImmutableMap.of(
        "spotterAtLunch2@2x.png", SPOTTER_AT_LUNCH_CID,
        "spotterEmailRedTagLeft@2x.png", SPOTTER_RED_TAG_LEFT_CID,
        "spotterEmailRedTagRight@2x.png", SPOTTER_RED_TAG_RIGHT_CID,
        "spotterEmailWhiteTagLeft@2x.png", SPOTTER_WHITE_TAG_LEFT_CID,
        "spotterEmailWhiteTagRight@2x.png", SPOTTER_WHITE_TAG_RIGHT_CID).entrySet()) {
      // Only attach images if they're referenced in the email.  Otherwise, if
      // they're unreferenced, they show up as independent attachments.
      if (html.contains("\"cid:" + imageAttributes.getValue() + "\"")) {
        MimeBodyPart imagePart = new MimeBodyPart();
        imagePart.attachFile("resources/img/" + imageAttributes.getKey());
        imagePart.setContentID("<" + imageAttributes.getValue() + ">");
        imagePart.setHeader("Content-Type", "image/png; name=\"" + imageAttributes.getKey() + "\"");
        imagePart.setHeader("Content-Disposition", "INLINE");
        content.addBodyPart(imagePart);
      }
    }

    // Let's go!
    message.setContent(content);
    return message;
  }

  private static class LunchEmailCallable implements Callable<Void> {
    private final Session session;
    private final User user;

    private LunchEmailCallable(Session session, User user) {
      this.session = session;
      this.user = user;
    }

    @Override
    public Void call() throws Exception {
      Transport transport = null;
      try {
        // Make sure it's lunchtime.
        UserTimezone userTimezone = UserTimezone.getForUser(user, true /* update */);
        if (!userTimezone.isAroundNoon() || userTimezone.isWeekend()) {
          return null;
        }

        // Send the email.
        try {
          Iterable<Article> articles = getArticles(user);
          if (Iterables.size(articles) > 2) {
            Message message = getMessage(session, user, articles);
            transport = EmailTransportProvider.getTransport(session);
            transport.sendMessage(message, message.getAllRecipients());
            System.out.println("Email sent to " + user.getEmail());
          }
        } catch (BiznessException e) {
          System.out.println("Error while sending email to " + user.getEmail());
          e.printStackTrace();
        }
      } catch (MessagingException | DatabaseRequestException | IOException e) {
        e.printStackTrace();
      } finally {
        // Close and terminate the transport.
        if (transport != null) {
          try {
            transport.close();
          } catch (MessagingException e) {}
        }
      }
      return null;
    }
  }

  public static void sendLunchEmails() throws DatabaseSchemaException, InterruptedException {
    Session session = EmailTransportProvider.getSession();

    // Figure out who's received a lunch email in the last 18 hours, so we
    // don't send them another lunch email on the same day.  We do 18 hours
    // rather than 24 to account for both small adjustments in this task's
    // start time, and to allow for the user to change timezones.
    Set<String> userIdsToSkip = Sets.newHashSet();
    for (Notification notification : Database.with(Notification.class).get(
        new QueryOption.WhereEqualsEnum("device_type", DeviceType.EMAIL),
        new QueryOption.WhereGreaterThan("create_time",
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(18)))) {
      userIdsToSkip.add(notification.getUserId());
    }

    // Amazon SES gave us a quota of 14 emails/second.  Getting a user's stream
    // and processing it tends to take around 500ms, so to stay well within quota,
    // we need this to be pretty small.
    ExecutorService executor = Executors.newFixedThreadPool(6);
    List<Callable<Void>> callables = Lists.newArrayList();
    for (User user : Database.with(User.class).get(
        new QueryOption.WhereNotNull("email"),
        new QueryOption.WhereNotEquals("email", ""),
        new QueryOption.WhereNotTrue("opt_out_email"))) {
      if (!userIdsToSkip.contains(user.getId())) {
        callables.add(new LunchEmailCallable(session, user));
      }
    }
    executor.invokeAll(callables);
    executor.shutdown();
  }

  public static void main(String args[]) throws Exception {
    sendLunchEmails();
    System.exit(0);
  }

//  public static void main(String args[]) throws Exception {
//    User user = Users.getByEmail("panaceaa@gmail.com");
//    Session session = EmailTransportProvider.getSession();
//    new LunchEmailCallable(session, user).call();
//  }
}
