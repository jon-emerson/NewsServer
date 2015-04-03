package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.LinkedInContactsFetcher;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/follow_linked_in_contacts")
public class FollowLinkedInContactsServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException, JSONException,
      BiznessException {
    // Fire off some requests to get the contacts from LinkedIn and the current
    // User object.
    String linkedInAccessToken = getRequiredParameter(req, "linked_in_access_token");
    LinkedInContactsFetcher linkedInContactsFetcher =
        new LinkedInContactsFetcher(linkedInAccessToken);
    User user = getUser(req);

    // Business logic: Update all the LinkedIn contacts and make sure the user's
    // interested in the LinkedIn connections Interest.
    user = user.toBuilder()
        .clearLinkedInContact()
        .addAllLinkedInContact(linkedInContactsFetcher.getLinkedInContacts())
        .clearInterest()
        .addAllInterest(Iterables.filter(user.getInterestList(),
            new Predicate<Interest>() {
              @Override
              public boolean apply(Interest interest) {
                // Remove any existing interest/non-interest in LinkedIn
                // contacts.  We'll add a new one just a few lines down.
                return interest.getType() != InterestType.LINKED_IN_CONTACTS;
              }
            }))
        .addInterest(Interest.newBuilder()
            .setId(GuidFactory.generate())
            .setType(InterestType.LINKED_IN_CONTACTS)
            .setSource(InterestSource.USER)
            .setCreateTime(System.currentTimeMillis()))
        .build();
    Database.update(user);

    // Create the response.
    JSONObject response = this.createSuccessResponse();
    response.put("user", new UserHelper(user).getUserJson());

    // To help with client latency, return the articles for the user's home
    // screen in this response.
    Iterable<Article> articles = Articles.getRankedArticles(
        user,
        NeuralNetworkScorer.getInstance(),
        GetArticlesServlet.NUM_RESULTS);
    response.put("articles", ArticleSerializer.serialize(articles, user,
        false /* includeLinkedInContacts */, false /* includeAddressBookContacts */));

    return response;
  }
}
