package com.janknspank.notifications;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.google.template.soy.tofu.SoyTofu.Renderer;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.UserProto.User;
import com.janknspank.server.NewsServlet;
import com.janknspank.server.WelcomeEmailServlet;
import com.sun.mail.smtp.SMTPMessage;

public class SendWelcomeEmails {
  private static final String MOBILE_SPOTTER_LOGO_1_CID = "mobileSpotterLogo1@spotternews.com";
  private static final String SETTINGS_ZOOM_CID = "settingsZoom@spotternews.com";

  static final String SUBJECT = "Welcome to Spotter!";

  private static String getHtml(User user) {
    SoyTofu soyTofu = NewsServlet.getTofu("welcomeemail");
    Renderer renderer = soyTofu.newRenderer(".main");
    String mobileSpotterLogo1ImgSrcPlaceholder = "////mobileSpotterLogo1ImgSrc////";
    String settingsZoomImgSrcPlaceholder = "////settingsZoomImgSrc////";
    renderer.setData(
        new SoyMapData(
            "isInBrowser", false,
            "mobileSpotterLogo1ImgSrc", mobileSpotterLogo1ImgSrcPlaceholder,
            "settingsZoomImgSrc", settingsZoomImgSrcPlaceholder,
            "unsubscribeLink",
                WelcomeEmailServlet.getUnsubscribeLink(user, false /* relativeUrl */),
            "welcomeEmailLink",
                WelcomeEmailServlet.getWelcomeEmailLink(user)));

    // For some reason, Soy Templates don't like cid: URLs.  They claim they're
    // invalid and render them as "#zSoyz" instead of doing as they're told.
    // So, let's give them something "valid" and then replace it with the actual
    // MIME-supported value here.
    return renderer.render()
        .replaceAll(mobileSpotterLogo1ImgSrcPlaceholder, "cid:" + MOBILE_SPOTTER_LOGO_1_CID)
        .replaceAll(settingsZoomImgSrcPlaceholder, "cid:" + SETTINGS_ZOOM_CID);
  }

  private static Message getMessage(Session session, User user)
      throws MessagingException, IOException {
    SMTPMessage message = new SMTPMessage(session);
    message.setFrom(new InternetAddress("support@spotternews.com", "Spotter News"));
    message.setRecipient(Message.RecipientType.TO,
        new InternetAddress(user.getEmail(), user.getFirstName() + " " + user.getLastName()));
    message.setSubject(SUBJECT);

    MimeMultipart content = new MimeMultipart();

    // HTML version.
    MimeBodyPart mainPart = new MimeBodyPart();
    String html = getHtml(user);
    mainPart.setContent(html, "text/html; charset=utf-8");
    content.addBodyPart(mainPart);

    // Image attachments.
    MimeBodyPart imagePart = new MimeBodyPart();
    imagePart.attachFile("resources/img/mobileSpotterLogo1@2x.png");
    imagePart.setContentID("<" + MOBILE_SPOTTER_LOGO_1_CID + ">");
    imagePart.setDisposition(MimeBodyPart.INLINE);
    content.addBodyPart(imagePart);

    MimeBodyPart imagePart2 = new MimeBodyPart();
    imagePart2.attachFile("resources/img/settingsZoom@2x.png");
    imagePart2.setContentID("<" + SETTINGS_ZOOM_CID + ">");
    imagePart2.setDisposition(MimeBodyPart.INLINE);
    content.addBodyPart(imagePart2);

    // Let's go!
    message.setContent(content);
    return message;
  }

  public static void sendWelcomeEmails() throws DatabaseSchemaException {
    Transport transport = null;

    try {
      Session session = EmailTransportProvider.getSession();
      transport = EmailTransportProvider.getTransport(session);

      // For every user with an email address for whom we have not sent a welcome
      // email, send them a welcome email now.
      Iterable<User> users = Database.with(User.class).get(
          new QueryOption.WhereNotNull("email"),
          new QueryOption.WhereNotEquals("email", ""),
          new QueryOption.WhereNotTrue("opt_out_email"),
          new QueryOption.WhereNull("welcome_email_sent_time"));
      for (User user : users) {
        // Mark that we're sending him/her a welcome email.
        try {
          Database.update(user.toBuilder()
              .setWelcomeEmailSentTime(System.currentTimeMillis())
              .build());
        } catch (DatabaseRequestException e) {
          e.printStackTrace();
          continue;
        }

        // Send the email.
        Message message = getMessage(session, user);
        transport.sendMessage(message, message.getAllRecipients());
        System.out.println("Email sent to " + user.getEmail());
      }
    } catch (MessagingException | IOException e) {
      e.printStackTrace();
    } finally {
      // Close and terminate the connection.
      if (transport != null) {
        try {
          transport.close();
        } catch (MessagingException e) {}
      }
    }
  }

  public static void main(String[] args) throws DatabaseSchemaException {
    sendWelcomeEmails();
  }
}