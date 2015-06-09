package com.janknspank.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.util.Sets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.UserInterests;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.AddressBookContact;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.LinkedInContact;
import com.janknspank.proto.UserProto.UrlFavorite;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.User.Experiment;

/**
 * Helper class containing the user's favorite and rated articles.
 */
public class UserHelper {
  private final User user;
  private final Map<String, Long> favoriteArticleIds;
  private final Map<String, Article> articleMap;
  private final Iterable<LinkedInContact> linkedInContacts;
  private final Iterable<AddressBookContact> addressBookContacts;

  public UserHelper(User user) throws DatabaseSchemaException {
    this.user = user;

    // Create a map of the favorite articles.
    favoriteArticleIds = getFavoriteArticleIds();
    articleMap = getArticleMap(favoriteArticleIds.keySet());
    Set<String> tombstonedPeopleNames = getTombstonedPeopleNames();
    linkedInContacts = getLinkedInContacts(tombstonedPeopleNames);
    addressBookContacts = getAddressBookContacts(tombstonedPeopleNames);
  }

  public Set<String> getTombstonedPeopleNames() {
    Set<String> tombstonedPeopleNames = Sets.newHashSet();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == InterestType.ENTITY
          && EntityType.fromValue(interest.getEntity().getType()).isA(EntityType.PERSON)
          && interest.getSource() == InterestSource.TOMBSTONE) {
        tombstonedPeopleNames.add(interest.getEntity().getKeyword());
      }
    }
    return tombstonedPeopleNames;
  }

  private Iterable<LinkedInContact> getLinkedInContacts(final Set<String> tombstonedPeopleNames) {
    return Iterables.filter(user.getLinkedInContactList(), new Predicate<LinkedInContact>() {
      @Override
      public boolean apply(LinkedInContact linkedInContact) {
        return !tombstonedPeopleNames.contains(linkedInContact.getName());
      }
    });
  }

  private Iterable<AddressBookContact> getAddressBookContacts(
      final Set<String> tombstonedPeopleNames) {
    return Iterables.filter(user.getAddressBookContactList(), new Predicate<AddressBookContact>() {
      @Override
      public boolean apply(AddressBookContact addressBookContact) {
        return !tombstonedPeopleNames.contains(addressBookContact.getName());
      }
    });
  }

  private Map<String, Long> getFavoriteArticleIds() throws DatabaseSchemaException {
    Map<String, Long> favoriteArticleIds = Maps.newHashMap();
    for (UrlFavorite favorite : user.getUrlFavoriteList()) {
      favoriteArticleIds.put(favorite.getUrlId(), favorite.getCreateTime());
    }
    return favoriteArticleIds;
  }

  private Map<String, Article> getArticleMap(Iterable<String> articleIds)
      throws DatabaseSchemaException {
    return Maps.uniqueIndex(
        Iterables.isEmpty(articleIds)
            ? Collections.<Article>emptyList()
            : Database.with(Article.class).get(articleIds),
        new Function<Article, String>() {
          @Override
          public String apply(Article article) {
            return article.getUrlId();
          }
        });
  }

  /**
   * @param favoriteArticleIds A map from article URL ID to the time the
   *     current user favorited it.
   */
  private JSONArray getFavoritesJsonArray() {
    List<Article> favoriteArticles = Lists.newArrayList();
    for (String urlId : favoriteArticleIds.keySet()) {
      if (articleMap.containsKey(urlId)) {
        favoriteArticles.add(articleMap.get(urlId));
      }
    }

    Collections.sort(favoriteArticles, new Comparator<Article>() {
      @Override
      public int compare(Article o1, Article o2) {
        // Newest articles first, sorted by when they were favorited.
        return Long.compare(favoriteArticleIds.get(o2.getUrlId()),
            favoriteArticleIds.get(o1.getUrlId()));
      }
    });

    JSONArray favoritesJsonArray = new JSONArray();
    for (Article favoriteArticle : favoriteArticles) {
      JSONObject favoriteJson = Serializer.toJSON(favoriteArticle);
      favoriteJson.put("favorited_time", favoriteArticleIds.get(favoriteArticle.getUrlId()));
      favoritesJsonArray.put(favoriteJson);
    }
    return favoritesJsonArray;
  }

  private JSONObject getExperimentsJsonObject() {
    JSONObject experimentsJsonObject = new JSONObject();
    for (Experiment experiment : user.getExperimentList()) {
      experimentsJsonObject.put(experiment.name().toLowerCase(), "true");
    }
    return experimentsJsonObject;
  }

  private JSONArray getInterestsJsonArray() {
    JSONArray jsonArray = new JSONArray();
    for (Interest interest : UserInterests.getInterests(user)) {
      JSONObject interestJsonObject = Serializer.toJSON(interest);
      if (interest.getType() == InterestType.INDUSTRY) {
        // This client needs to know industry names so it can render them!!
        interestJsonObject.put("name",
            FeatureId.fromId(interest.getIndustryCode()).getTitle());
      }
      jsonArray.put(interestJsonObject);
    }
    return jsonArray;
  }

  public JSONObject getUserJson() throws DatabaseSchemaException {
    JSONObject userJson = Serializer.toJSON(user);
    userJson.put("address_book_contacts", Serializer.toJSON(addressBookContacts));
    userJson.put("favorites", getFavoritesJsonArray());
    userJson.put("interests", getInterestsJsonArray());
    userJson.put("linked_in_contacts", Serializer.toJSON(linkedInContacts));

    // For now, clear the badge count for users when they open the app.
    userJson.put("ios_badge_count", 0);

    if (user.getExperimentCount() > 0) {
      userJson.put("experiments", getExperimentsJsonObject());
    }
    return userJson;
  }
}