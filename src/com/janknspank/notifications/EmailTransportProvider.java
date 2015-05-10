package com.janknspank.notifications;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

public class EmailTransportProvider {
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

  /**
   * Amazon SES SMTP host name. Note: We can talk to us-east-2, us-west-2, etc.
   * But we verified our emails through us-east-1, and we run our servers out
   * of us-east-1, so we're doing us-east-1 here too.
   */
  private static final String HOST = "email-smtp.us-east-1.amazonaws.com";

  /**
   * Port we will connect to on the Amazon SES SMTP endpoint. We are choosing
   * port 25 because we will use STARTTLS to encrypt the connection.
   */
  private static final int PORT = 587; // 25;

  public static Session getSession() {
    // Create a Properties object to contain connection configuration
    // information.
    Properties props = System.getProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.port", PORT);

    // Set properties indicating that we want to use STARTTLS to encrypt the
    // connection. The SMTP session will begin on an unencrypted connection,
    // and then the client will issue a STARTTLS command to upgrade to an
    // encrypted connection.
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");

    // Create a Session object to represent a mail session with the specified
    // properties.
    return Session.getDefaultInstance(props);
  }

  /**
   * Returns an opened Transport for sending emails via Amazon SES.
   * NOTE(jonemerson): Make sure you close this Transport when you're done!!
   */
  public static Transport getTransport(Session session) throws MessagingException {
    // Connect to Amazon SES using the SMTP username and password you specified
    // above.
    System.out.println("Attempting to send an email through the Amazon SES SMTP interface...");
    Transport transport = session.getTransport();
    transport.connect(HOST, SMTP_USERNAME, SMTP_PASSWORD);
    return transport;
  }
}
