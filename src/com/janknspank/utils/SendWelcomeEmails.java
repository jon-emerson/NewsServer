package com.janknspank.utils;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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

  // Replace with your "From" address. This address must be verified.
  static final String FROM = "SENDER@EXAMPLE.COM";

  // Replace with a "To" address. If your account is still in the
  // sandbox, this address must be verified.
  static final String TO = "panaceaa@gmail.com";

  static final String BODY =
      "This email was sent through the Amazon SES SMTP interface by using Java.";
  static final String SUBJECT =
      "Amazon SES test (SMTP interface accessed using Java)";

  // Amazon SES SMTP host name. This example uses the us-west-2 region.
  static final String HOST = "email-smtp.us-west-2.amazonaws.com";

  // Port we will connect to on the Amazon SES SMTP endpoint. We are choosing
  // port 25 because we will use STARTTLS to encrypt the connection.
  static final int PORT = 25;

  public static void main(String[] args) throws Exception {

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

    // Create a message with the specified information. 
    MimeMessage msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(FROM));
    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
    msg.setSubject(SUBJECT);
    msg.setContent(BODY,"text/plain");

    // Create a transport.
    Transport transport = session.getTransport();

    // Send the message.
    try {
      System.out.println("Attempting to send an email through the Amazon SES SMTP interface...");

      // Connect to Amazon SES using the SMTP username and password you specified above.
      transport.connect(HOST, SMTP_USERNAME, SMTP_PASSWORD);

      // Send the email.
      transport.sendMessage(msg, msg.getAllRecipients());
      System.out.println("Email sent!");
    } catch (Exception e) {
      System.out.println("The email was not sent.");
      System.out.println("Error message: " + e.getMessage());
    } finally {
      // Close and terminate the connection.
      transport.close();
    }
  }
}