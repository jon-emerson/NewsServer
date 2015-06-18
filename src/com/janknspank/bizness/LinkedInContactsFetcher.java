package com.janknspank.bizness;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.collect.Maps;
import com.janknspank.proto.UserProto.LinkedInContact;

public class LinkedInContactsFetcher {
  private static final String CONNECTIONS_URL = "https://api.linkedin.com/v1/people/~/connections";

  private final Future<Document> linkedInConnectionsDocumentFuture;

  public LinkedInContactsFetcher(String linkedInAccessToken) {
    linkedInConnectionsDocumentFuture =
        LinkedInLoginHandler.getDocumentFuture(CONNECTIONS_URL, linkedInAccessToken);
  }

  /**
   * Returns LinkedInContact objects for each person in the user's linked in
   * connections.
   */
  public Iterable<LinkedInContact> getLinkedInContacts() throws BiznessException {
    Document linkedInConnectionsDocument;
    try {
      linkedInConnectionsDocument = linkedInConnectionsDocumentFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new BiznessException("Error fetching connections: " + e.getMessage(), e);
    }

    Map<String, LinkedInContact> linkedInContactsMap = Maps.newHashMap();
    for (Element personEl : linkedInConnectionsDocument.select("person")) {
      StringBuilder nameBuilder = new StringBuilder();
      Element firstNameEl = personEl.select("first-name").first();
      if (firstNameEl != null) {
        nameBuilder.append(firstNameEl.text());
      }
      Element lastNameEl = personEl.select("last-name").first();
      if (lastNameEl != null) {
        if (lastNameEl != null) {
          nameBuilder.append(" ");
        }
        nameBuilder.append(lastNameEl.text());
      }
      String name = nameBuilder.toString();
      linkedInContactsMap.put(name, LinkedInContact.newBuilder().setName(name).build());
    }
    return linkedInContactsMap.values();
  }

}
