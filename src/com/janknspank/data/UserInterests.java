package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import com.janknspank.TopList;
import com.janknspank.proto.Core.AddressBook;
import com.janknspank.proto.Core.UserInterest;

public class UserInterests {
  private final static PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
  private final static PhoneNumberOfflineGeocoder GEOCODER =
      PhoneNumberOfflineGeocoder.getInstance();
  private static final String SELECT_FOR_USER_COMMAND =
      "SELECT * FROM " + Database.getTableName(UserInterest.class) +
      "    WHERE user_id=?";

  public static final String TYPE_PERSON = "p";
  public static final String TYPE_ORGANIZATION = "o";
  public static final String TYPE_LOCATION = "l";
  public static final String SOURCE_ADDRESS_BOOK = "ab";
  public static final String SOURCE_USER = "u";
  public static final String SOURCE_LINKEDIN_PROFILE = "lp";
  public static final String SOURCE_LINKEDIN_CONNECTIONS = "lc";

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
   * Returns a complete list of the specified user's interests, both implicit
   * and explicit.  NOTE(jonemerson): We do not want to send the implicit ones
   * to the client.
   */
  public static List<UserInterest> getInterests(String userId) throws DataInternalException {
    try {
      PreparedStatement stmt = Database.getConnection().prepareStatement(SELECT_FOR_USER_COMMAND);
      stmt.setString(1, userId);
      return Database.createListFromResultSet(stmt.executeQuery(), UserInterest.class);
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching favorites", e);
    }
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
      if (name != null) {
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
    List<UserInterest> interests = calculateInterests(userId, addressBook);

    // Find what interests we already have, so that we can keep ones that the
    // address book still contains, and we can delete ones it doesn't.
    Map<String, UserInterest> peopleInterestsFromAddressBook = Maps.newHashMap();
    Map<String, UserInterest> locationInterestsFromAddressBook = Maps.newHashMap();
    for (UserInterest interest : getInterests(userId)) {
      if (SOURCE_ADDRESS_BOOK.equals(interest.getSource())) {
        if (TYPE_PERSON.equals(interest.getType())) {
          peopleInterestsFromAddressBook.put(interest.getKeyword(), interest);
        } else if (TYPE_LOCATION.equals(interest.getType())) {
          locationInterestsFromAddressBook.put(interest.getKeyword(), interest);
        }
      }
    }

    // Figure out which interests we should delete and which we should insert.
    Set<UserInterest> interestsToDelete = Sets.newHashSet();
    List<UserInterest> interestsToInsert = Lists.newArrayList();
    interestsToDelete.addAll(peopleInterestsFromAddressBook.values());
    interestsToDelete.addAll(locationInterestsFromAddressBook.values());
    for (UserInterest interest : interests) {
      if (TYPE_PERSON.equals(interest.getType())) {
        UserInterest existingInterest =
            peopleInterestsFromAddressBook.get(interest.getKeyword());
        if (existingInterest != null) {
          interestsToDelete.remove(existingInterest);
        } else {
          interestsToInsert.add(interest);
        }
      } else if (TYPE_LOCATION.equals(interest.getType())) {
        UserInterest existingInterest =
            locationInterestsFromAddressBook.get(interest.getKeyword());
        if (existingInterest != null) {
          interestsToDelete.remove(existingInterest);
        } else {
          interestsToInsert.add(interest);
        }
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
