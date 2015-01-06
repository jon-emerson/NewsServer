package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.janknspank.common.TopList;
import com.janknspank.proto.Core.AddressBook;
import com.janknspank.proto.Core.LinkedInProfile;
import com.janknspank.proto.Core.UserInterest;

public class UserInterests {
  public static final String TYPE_LOCATION = "l";
  public static final String TYPE_PERSON = "p";
  public static final String TYPE_ORGANIZATION = "o";
  public static final String SOURCE_ADDRESS_BOOK = "ab";
  public static final String SOURCE_LINKEDIN_CONNECTIONS = "lc";
  public static final String SOURCE_LINKEDIN_PROFILE = "lp";
  public static final String SOURCE_USER = "u";
  public static final String SOURCE_TOMBSTONE = "t";

  private final static PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
  private final static PhoneNumberOfflineGeocoder GEOCODER =
      PhoneNumberOfflineGeocoder.getInstance();
  private static final String SELECT_FOR_USER_COMMAND =
      "SELECT * FROM " + Database.getTableName(UserInterest.class) +
      "    WHERE user_id=? AND source != \"" + SOURCE_TOMBSTONE + "\"";

  /** Helper method for creating the UserInterestData table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(
        UserInterest.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(UserInterest.class)) {
      connection.prepareStatement(statement).execute();
    }
  }

  /**
   * Returns a complete list of the specified user's interests.
   */
  public static List<UserInterest> getInterests(String userId) throws DataInternalException {
    try {
      PreparedStatement stmt = Database.getConnection().prepareStatement(SELECT_FOR_USER_COMMAND);
      stmt.setString(1, userId);
      return Database.createListFromResultSet(stmt.executeQuery(), UserInterest.class);
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching interests: " + e.getMessage(), e);
    }
  }

  /**
   * Returns a list of implied interests derived from the user's passed-in
   * address book.
   */
  private static List<UserInterest> calculateInterests(String userId, LinkedInProfile profile) {
    JSONObject profileJson = new JSONObject(profile.getData());
    if (!profileJson.has("positions")) {
      return Collections.emptyList();
    }

    JSONObject positionsJson = profileJson.getJSONObject("positions");
    if (!positionsJson.has("values")) {
      return Collections.emptyList();
    }

    List<UserInterest> interests = Lists.newArrayList();
    JSONArray positionsJsonArray = positionsJson.getJSONArray("values");
    for (int i = 0; i < positionsJsonArray.length(); i++) {
      JSONObject positionJson = positionsJsonArray.getJSONObject(i);
      if (positionJson.has("company")) {
        JSONObject companyJson = positionJson.getJSONObject("company");
        if (companyJson.has("name")) {
          interests.add(UserInterest.newBuilder()
              .setId(GuidFactory.generate())
              .setUserId(userId)
              .setKeyword(companyJson.getString("name"))
              .setSource(UserInterests.SOURCE_LINKEDIN_PROFILE)
              .setType(UserInterests.TYPE_ORGANIZATION)
              .build());
        }
      }
    }

    return interests;
  }

  /**
   * Returns a list of implied interests derived from the user's passed-in
   * address book.
   */
  private static List<UserInterest> calculateInterests(String userId, AddressBook addressBook) {
    List<UserInterest> interests = Lists.newArrayList();
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
        interests.add(UserInterest.newBuilder()
            .setId(GuidFactory.generate())
            .setUserId(userId)
            .setKeyword(name)
            .setSource(UserInterests.SOURCE_ADDRESS_BOOK)
            .setType(UserInterests.TYPE_PERSON)
            .build());
      }

      // Create a list of where the user's contacts are from, so we can tailor
      // news to where he lives and has lived before.
      JSONArray phoneNumbersJson = personJson.getJSONArray("phoneNumbers");
      for (int j = 0; j < phoneNumbersJson.length(); j++) {
        try {
          PhoneNumber phoneNumber = PHONE_NUMBER_UTIL.parse(phoneNumbersJson.getString(j), "US");
          if (PHONE_NUMBER_UTIL.isValidNumber(phoneNumber)) {
            locations.add(GEOCODER.getDescriptionForNumber(phoneNumber, Locale.US));
          }
        } catch (NumberParseException e) {
          e.printStackTrace();
        }
      }
    }

    // Add the top 3 contact locations to the user's interests.
    TopList topLocations = new TopList(3);
    for (String location : locations.elementSet()) {
      topLocations.add(location, locations.count(location));
    }
    for (String location : topLocations.getKeys()) {
      interests.add(UserInterest.newBuilder()
          .setId(GuidFactory.generate())
          .setUserId(userId)
          .setKeyword(location)
          .setSource(UserInterests.SOURCE_ADDRESS_BOOK)
          .setType(UserInterests.TYPE_LOCATION)
          .build());
    }

    return interests;
  }

  public static void updateInterests(String userId, AddressBook addressBook)
      throws DataInternalException {
    updateInterests(userId, calculateInterests(userId, addressBook), SOURCE_ADDRESS_BOOK);
  }

  public static void updateInterests(String userId, LinkedInProfile linkedInProfile)
      throws DataInternalException {
    updateInterests(userId, calculateInterests(userId, linkedInProfile), SOURCE_LINKEDIN_PROFILE);
  }

  private static void updateInterests(String userId, List<UserInterest> interests, String type)
      throws DataInternalException {
    // Find what interests we already have, so that we can keep ones that the
    // address book still contains, and we can delete ones it doesn't.
    Map<String, Map<String, UserInterest>> interestsByTypeAndKeyword = Maps.newHashMap();
    for (UserInterest interest : getInterests(userId)) {
      if (type.equals(interest.getSource())) {
        Map<String, UserInterest> keywordMap =
            interestsByTypeAndKeyword.get(interest.getType());
        if (keywordMap == null) {
          keywordMap = Maps.newHashMap();
          interestsByTypeAndKeyword.put(interest.getType(), keywordMap);
        }
        keywordMap.put(interest.getKeyword(), interest);
      }
    }

    // Figure out which interests we should delete and which we should insert.
    Set<UserInterest> interestsToDelete = Sets.newHashSet();
    List<UserInterest> interestsToInsert = Lists.newArrayList();
    for (Map<String, UserInterest> keywordMap : interestsByTypeAndKeyword.values()) {
      interestsToDelete.addAll(keywordMap.values());
    }
    for (UserInterest interest : interests) {
      UserInterest existingInterest =
          interestsByTypeAndKeyword.containsKey(interest.getType()) ?
              interestsByTypeAndKeyword.get(interest.getType()).get(interest.getKeyword()) :
              null;
      if (existingInterest != null) {
        interestsToDelete.remove(existingInterest);
      } else {
        interestsToInsert.add(interest);
      }
    }

    // Get 'er done.
    try {
      Database.insert(interestsToInsert);
    } catch (ValidationException e) {
      throw new DataInternalException("Error inserting interests: " + e.getMessage(), e);
    }
    Database.delete(interestsToDelete);
  }
}
