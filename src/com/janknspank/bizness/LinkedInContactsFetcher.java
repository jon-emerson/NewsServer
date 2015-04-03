package com.janknspank.bizness;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.collect.Maps;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.UserProto.LinkedInContact;

public class LinkedInContactsFetcher {
  private static final String CONNECTIONS_URL = "https://api.linkedin.com/v1/people/~/connections";

  private final Future<DocumentNode> linkedInConnectionsDocumentFuture;

  public LinkedInContactsFetcher(String linkedInAccessToken) {
    linkedInConnectionsDocumentFuture =
        LinkedInLoginHandler.getDocumentFuture(CONNECTIONS_URL, linkedInAccessToken);
  }

  /**
   * Returns LinkedInContact objects for each person in the user's linked in
   * connections.
   */
  public Iterable<LinkedInContact> getLinkedInContacts() throws BiznessException {
    DocumentNode linkedInConnectionsDocument;
    try {
      linkedInConnectionsDocument = linkedInConnectionsDocumentFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new BiznessException("Error fetching connections: " + e.getMessage(), e);
    }

    Map<String, LinkedInContact> linkedInContactsMap = Maps.newHashMap();
    for (Node personNode : linkedInConnectionsDocument.findAll("person")) {
      StringBuilder nameBuilder = new StringBuilder();
      Node firstNameNode = personNode.findFirst("first-name");
      if (firstNameNode != null) {
        nameBuilder.append(firstNameNode.getFlattenedText());
      }
      Node lastNameNode = personNode.findFirst("last-name");
      if (lastNameNode != null) {
        if (firstNameNode != null) {
          nameBuilder.append(" ");
        }
        nameBuilder.append(lastNameNode.getFlattenedText());
      }
      String name = nameBuilder.toString();
      linkedInContactsMap.put(name, LinkedInContact.newBuilder().setName(name).build());
    }
    return linkedInContactsMap.values();
  }

}
