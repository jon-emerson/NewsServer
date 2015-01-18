package com.janknspank.data;

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
import com.janknspank.common.TopList;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.Core.AddressBook;
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
      "SELECT * FROM " + Database.getTableName(UserInterest.class) + " "
      + "WHERE user_id=? AND source != \"" + SOURCE_TOMBSTONE + "\"";

  /**
   * Returns a complete list of the specified user's interests.
   */
  public static List<UserInterest> getInterests(String userId) throws DataInternalException {
    try {
      PreparedStatement stmt = Database.getInstance().prepareStatement(SELECT_FOR_USER_COMMAND);
      stmt.setString(1, userId);
      return Database.createListFromResultSet(stmt.executeQuery(), UserInterest.class);
    } catch (SQLException e) {
      throw new DataInternalException("Error fetching interests: " + e.getMessage(), e);
    }
  }

  /**
   * Returns a list of interests derived from the user's passed-in LinkedIn
   * profile.
   */
  public static List<UserInterest> updateInterests(String userId, DocumentNode profileDocumentNode,
      DocumentNode connectionsDocumentNode)
      throws DataInternalException {

    // Step 1: Update companies / organizations.
    // Note: Dedupe by using a set.
    Set<String> companyNames = Sets.newHashSet();
    for (Node companyNameNode : profileDocumentNode.findAll("position > company > name")) {
      companyNames.add(companyNameNode.getFlattenedText());
    }
    List<UserInterest> companyInterests = Lists.newArrayList();
    for (String companyName : companyNames) {
      companyInterests.add(UserInterest.newBuilder()
          .setId(GuidFactory.generate())
          .setUserId(userId)
          .setKeyword(companyName)
          .setSource(UserInterests.SOURCE_LINKEDIN_PROFILE)
          .setType(UserInterests.TYPE_ORGANIZATION)
          .build());
    }
    updateInterests(userId, companyInterests, SOURCE_LINKEDIN_PROFILE);

    // Step 2: Update people.
    Set<String> peopleNames = Sets.newHashSet();
    for (Node personNode : connectionsDocumentNode.findAll("person")) {
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
      peopleNames.add(nameBuilder.toString());
    }
    List<UserInterest> personInterests = Lists.newArrayList();
    for (String peopleName : peopleNames) {
      personInterests.add(UserInterest.newBuilder()
          .setId(GuidFactory.generate())
          .setUserId(userId)
          .setKeyword(peopleName)
          .setSource(UserInterests.SOURCE_LINKEDIN_CONNECTIONS)
          .setType(UserInterests.TYPE_PERSON)
          .build());
    }
    return updateInterests(userId, personInterests, SOURCE_LINKEDIN_CONNECTIONS);
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

  public static List<UserInterest> updateInterests(String userId, AddressBook addressBook)
      throws DataInternalException {
    return updateInterests(userId, calculateInterests(userId, addressBook), SOURCE_ADDRESS_BOOK);
  }

  private static List<UserInterest> updateInterests(String userId, List<UserInterest> interests,
      String source) throws DataInternalException {
    // Collect all the final interests, so we can return them in the end.
    List<UserInterest> allInterests = Lists.newArrayList();
    allInterests.addAll(interests);

    // Find what interests we already have, so that we can keep ones that the
    // address book still contains, and we can delete ones it doesn't.
    Map<String, Map<String, UserInterest>> interestsByTypeAndKeyword = Maps.newHashMap();
    for (UserInterest interest : getInterests(userId)) {
      if (source.equals(interest.getSource())) {
        Map<String, UserInterest> keywordMap =
            interestsByTypeAndKeyword.get(interest.getType());
        if (keywordMap == null) {
          keywordMap = Maps.newHashMap();
          interestsByTypeAndKeyword.put(interest.getType(), keywordMap);
        }
        keywordMap.put(interest.getKeyword(), interest);
      } else {
        allInterests.add(interest);
      }
    }

    // Figure out which interests we should delete and which we should insert.
    Set<UserInterest> interestsToDelete = Sets.newHashSet();
    List<UserInterest> interestsToInsert = Lists.newArrayList();
    for (Map<String, UserInterest> keywordMap : interestsByTypeAndKeyword.values()) {
      interestsToDelete.addAll(keywordMap.values());
    }
    for (UserInterest interest : interests) {
      if (!source.equals(interest.getSource())) {
        // This method only supports updating one type at a time.
        throw new DataInternalException("Cannot add UserInterest of source " + interest.getSource()
            + " inside a call to update UserInterests of source " + source + ": "
            + interest.toString());
      }
      UserInterest existingInterest =
          interestsByTypeAndKeyword.containsKey(interest.getType())
              ? interestsByTypeAndKeyword.get(interest.getType()).get(interest.getKeyword())
              : null;
      if (existingInterest != null) {
        interestsToDelete.remove(existingInterest);
      } else {
        interestsToInsert.add(interest);
        allInterests.add(interest);
      }
    }

    // Get 'er done.
    Database database = Database.getInstance();
    try {
      database.insert(interestsToInsert);
    } catch (ValidationException e) {
      throw new DataInternalException("Error inserting interests: " + e.getMessage(), e);
    }
    database.delete(interestsToDelete);

    // Return the latest and greatest interests, regardless of type.
    return allInterests;
  }

  /** Helper method for creating the UserInterestData table. */
  public static void main(String args[]) throws Exception {
    Database database = Database.getInstance();
    database.prepareStatement(database.getCreateTableStatement(UserInterest.class)).execute();
    for (String statement : database.getCreateIndexesStatement(UserInterest.class)) {
      database.prepareStatement(statement).execute();
    }
  }
}
