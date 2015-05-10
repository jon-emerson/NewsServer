package com.janknspank.notifications;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.http.client.utils.URIBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.google.template.soy.tofu.SoyTofu.Renderer;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.UserInterests;
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
    if (date.endsWith("1")) {
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
      for (ArticleKeyword keyword :
          ArticleSerializer.getBestKeywords(article, userKeywordSet, userIndustryFeatureIdIds)) {
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
    SoyListData list = new SoyListData();
    for (ArticleKeyword keyword :
        ArticleSerializer.getBestKeywords(article, userKeywordSet, userIndustryFeatureIdIds)) {
      list.add(new SoyMapData(
          "selected", userKeywordSet.contains(keyword.getKeyword().toLowerCase()),
          "keyword", keyword.getKeyword()));
    }
    return list;
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
          "time", ViewFeedSoy.getTime(article),
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
    //return Database.with(Article.class).get(
     //   new QueryOption.DescendingSort("published_time"),
       // new QueryOption.Limit(6));
// TODO(jonemerson): Do the right query...
    return Iterables.limit(Articles.getMainStream(user), 6);
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

  private static Message getMessage(Session session, User user)
      throws MessagingException, IOException, DatabaseSchemaException, BiznessException,
          DatabaseRequestException {
    Iterable<Article> articles = getArticles(user);

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
      MimeBodyPart imagePart = new MimeBodyPart();
      imagePart.attachFile("resources/img/" + imageAttributes.getKey());
      imagePart.setContentID("<" + imageAttributes.getValue() + ">");
      imagePart.setDisposition(MimeBodyPart.INLINE);
      content.addBodyPart(imagePart);
    }

    // Let's go!
    message.setContent(content);
    return message;
  }

  public static void sendLunchEmails() throws DatabaseSchemaException {
    Session session = EmailTransportProvider.getSession();

    Iterable<User> users = Database.with(User.class).get(
        new QueryOption.WhereNotNull("email"),
        new QueryOption.WhereNotTrue("opt_out_email"));
    for (User user : users) {
      Transport transport = null;
      try {
        // Make sure it's lunchtime.
        UserTimezone userTimezone = UserTimezone.getForUser(user, true /* update */);
        if (!userTimezone.isAroundNoon()) {
          continue;
        }

        // Send the email.
        try {
          Message message = getMessage(session, user);
          transport = EmailTransportProvider.getTransport(session);
          transport.sendMessage(message, message.getAllRecipients());
          System.out.println("Email sent to " + user.getEmail());
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
    }
  }

  public static void main(String[] args) throws DatabaseSchemaException {
    sendLunchEmails();
    System.exit(0);
  }
}
