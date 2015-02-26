package com.janknspank.bizness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.api.client.util.Maps;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.janknspank.common.TopList;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.AddressBook;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.Source;
import com.janknspank.proto.UserProto.User;

public class UserInterests {
  public static final String TYPE_LOCATION = "l";
  public static final String TYPE_PERSON = "p";
  public static final String TYPE_ORGANIZATION = "o";

  /**
   * Returns only valid, currently followed user interests. 
   * Filters out TOMBSTONEd interests
   */
  public static List<Interest> getCurrentInterests(User user) {
    List<Interest> allInterests = user.getInterestList();
    List<Interest> currentInterests = new ArrayList<>();
    for (Interest interest : allInterests) {
      if (interest.getSource() != Interest.Source.TOMBSTONE) {
        currentInterests.add(interest);
      }
    }
    return currentInterests;
  }
  
  /**
   * Returns only valid, currently followed LinkedIn contacts. 
   */
  public static List<Interest> getCurrentLinkedInContacts(User user) {
    List<Interest> allInterests = user.getInterestList();
    List<Interest> linkedInContacts = new ArrayList<>();
    for (Interest interest : allInterests) {
      if (interest.getSource() != Interest.Source.LINKED_IN_CONNECTIONS) {
        linkedInContacts.add(interest);
      }
    }
    return linkedInContacts;
  }
  
  /**
   * Returns only valid, currently followed contacts. 
   * Filters out TOMBSTONEd interests
   */
  public static List<Interest> getCurrentAddressBookContacts(User user) {
    List<Interest> allInterests = user.getInterestList();
    List<Interest> addressBookContacts = new ArrayList<>();
    for (Interest interest : allInterests) {
      if (interest.getSource() != Interest.Source.ADDRESS_BOOK) {
        addressBookContacts.add(interest);
      }
    }
    return addressBookContacts;
  }
  
  /**
   * Returns a list of implied interests derived from the user's passed-in
   * address book.
   */
  private static List<Interest> calculateInterests(User user, AddressBook addressBook) {
    List<Interest> interests = Lists.newArrayList();
    Multiset<String> locations = HashMultiset.create();

    JSONArray addressBookJson = new JSONArray(addressBook.getData());
    for (int i = 0; i < addressBookJson.length(); i++) {
      String name = null;
      JSONObject personJson = addressBookJson.getJSONObject(i);
      if (personJson.has("firstName")) {
        if (personJson.has("lastName")) {
          name = personJson.getString("firstName") + " " + personJson.getString("lastName");
        } else {
          name = personJson.getString("firstName");
        }
      } else if (personJson.has("lastName")) {
        name = personJson.getString("lastName");
      }
      if (name != null && name.contains(" ")) {
        interests.add(Interest.newBuilder()
            .setId(GuidFactory.generate())
            .setKeyword(name)
            .setSource(Source.ADDRESS_BOOK)
            .setType(TYPE_PERSON)
            .setCreateTime(System.currentTimeMillis())
            .build());
      }
    }

    // Add the top 3 contact locations to the user's interests.
    TopList<String, Integer> topLocations = new TopList<>(3);
    for (String location : locations.elementSet()) {
      topLocations.add(location, locations.count(location));
    }
    for (String location : topLocations.getKeys()) {
      interests.add(Interest.newBuilder()
          .setId(GuidFactory.generate())
          .setKeyword(location)
          .setSource(Source.ADDRESS_BOOK)
          .setType(TYPE_LOCATION)
          .setCreateTime(System.currentTimeMillis())
          .build());
    }

    return interests;
  }

  public static User updateInterests(User user, AddressBook addressBook)
      throws BiznessException, DatabaseSchemaException {
    return updateInterests(user, calculateInterests(user, addressBook), Source.ADDRESS_BOOK);
  }

  /**
   * Converts an interest to a String that can be used for uniqueness.
   */
  private static String hashInterest(Interest interest) {
    List<String> tokens = Lists.newArrayList();
    if (interest.hasSource()) {
      tokens.add(interest.getSource().name());
    }
    if (interest.hasType()) {
      tokens.add(interest.getType());
    }
    if (interest.hasKeyword()) {
      tokens.add(interest.getKeyword());
    }
    return Joiner.on(":").join(tokens);
  }

  /**
   * For the given {@code source}, replaces the user's interests with those
   * passed in {@code interests}.  If any previous interests from the source
   * match interests in {@code interests}, the previous object instances are
   * retained, so that that {@code Interest#getCreateTime()} is not lost.
   */
  private static User updateInterests(User user, List<Interest> interests,
      Source source) throws BiznessException, DatabaseSchemaException {
    List<Interest> finalInterests = Lists.newArrayList();
    Map<String, Interest> existingInterestMap = Maps.newHashMap();
    for (Interest interest : user.getInterestList()) {
      if (interest.getSource() == source) {
        existingInterestMap.put(hashInterest(interest), interest);
      } else {
        finalInterests.add(interest);
      }
    }
    for (Interest interest : interests) {
      String hash = hashInterest(interest);
      if (existingInterestMap.containsKey(hash)) {
        finalInterests.add(existingInterestMap.get(hash));
      } else {
        finalInterests.add(interest);
      }
    }
    try {
      return Database.set(user, "interest", finalInterests);
    } catch (DatabaseRequestException e) {
      throw new BiznessException("Could not update interests: " + e.getMessage(), e);
    }
  }
}
