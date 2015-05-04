package com.janknspank.utils;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.google.template.soy.tofu.SoyTofu.Renderer;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.UserProto.User;
import com.janknspank.server.NewsServlet;

public class SendWelcomeEmails {
  private static final String SMTP_USERNAME;
  private static final String SMTP_PASSWORD;
  static {
    SMTP_USERNAME = System.getenv("SMTP_USERNAME");
    if (SMTP_USERNAME == null) {
      throw new Error("$SMTP_USERNAME is undefined");
    }
    SMTP_PASSWORD = System.getenv("SMTP_PASSWORD");
    if (SMTP_PASSWORD == null) {
      throw new Error("$SMTP_PASSWORD is undefined");
    }
  }

  static final String SUBJECT = "Welcome to Spotter!";

  // Amazon SES SMTP host name. Note: We can talk to us-east-2, us-west-2, etc.
  // But we verified our emails through us-east-1, and we run our servers out
  // of us-east-1, so we're doing us-east-1 here too.
  static final String HOST = "email-smtp.us-east-1.amazonaws.com";

  // Port we will connect to on the Amazon SES SMTP endpoint. We are choosing
  // port 25 because we will use STARTTLS to encrypt the connection.
  static final int PORT = 587; // 25;

  private static String getHtml() {
    SoyTofu soyTofu = NewsServlet.getTofu("welcomeemail");
    Renderer renderer = soyTofu.newRenderer(".main");
    renderer.setData(new SoyMapData(
        "title", "Spotter - Business news, personalized"));
    return renderer.render();
  }

  private static MimeMessage getMessage(Session session, User user)
      throws UnsupportedEncodingException, MessagingException {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress("support@spotternews.com", "Spotter News"));
    message.setRecipient(Message.RecipientType.TO,
        new InternetAddress(user.getEmail(), user.getFirstName() + " " + user.getLastName()));
    message.setSubject(SUBJECT);
    message.setContent(getHtml(), "text/html; charset=utf-8");
    return message;
  }

  public static void main(String[] args) throws DatabaseSchemaException {
    // Create a Properties object to contain connection configuration information.
    Properties props = System.getProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.port", PORT);

    // Set properties indicating that we want to use STARTTLS to encrypt the connection.
    // The SMTP session will begin on an unencrypted connection, and then the client
    // will issue a STARTTLS command to upgrade to an encrypted connection.
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");

    // Create a Session object to represent a mail session with the specified properties. 
    Session session = Session.getDefaultInstance(props);

    Transport transport = null;
    try {
      // Connect to Amazon SES using the SMTP username and password you specified above.
      System.out.println("Attempting to send an email through the Amazon SES SMTP interface...");
      transport = session.getTransport();
      transport.connect(HOST, SMTP_USERNAME, SMTP_PASSWORD);

      // For every user with an email address for whom we have not sent a welcome
      // email, send them a welcome email now.
      // HACK(jonemerson): Right now, only send to Tom.
      Iterable<User> users = Database.with(User.class).get(
          new QueryOption.WhereEquals("email", ImmutableList.of(
              "tom.charytoniuk@gmail.com")));
      // TODO(jonemerson): Switch back to this.
      // Iterable<User> users = Database.with(User.class).get(
      //     new QueryOption.WhereNotNull("email"),
      //     new QueryOption.WhereNull("welcome_email_sent_time"));
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
    } catch (MessagingException | UnsupportedEncodingException e) {
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
}