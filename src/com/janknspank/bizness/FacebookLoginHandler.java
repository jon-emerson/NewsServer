package com.janknspank.bizness;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.api.client.util.Lists;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.janknspank.classifier.Feature;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.classifier.Vector;
import com.janknspank.classifier.VectorFeature;
import com.janknspank.common.TopList;
import com.janknspank.crawler.social.FacebookData;
import com.janknspank.crawler.social.SocialException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.nlp.KeywordCanonicalizer;
import com.janknspank.nlp.KeywordFinder;
import com.janknspank.nlp.KeywordUtils;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.Entity.Source;
import com.janknspank.proto.CoreProto.VectorData;
import com.janknspank.proto.CoreProto.VectorData.WordFrequency;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.server.RequestException;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.User.Education;
import com.restfb.types.User.EducationClass;
import com.restfb.types.User.Work;

/**
 * Handles User creation and/or retrieval from Facebook OAuth tokens. Updates
 * the user's implicit interests with industries and entities from the user's
 * latest and greatest Facebook profile.
 */
public class FacebookLoginHandler {
  public static com.restfb.types.User getFacebookUser(String fbAccessToken)
      throws SocialException, RequestException {
    long startTime = System.currentTimeMillis();
    FacebookClient facebookClient = new DefaultFacebookClient(fbAccessToken,
        FacebookData.getFacebookAppSecret(), Version.VERSION_2_2);
    com.restfb.types.User fbUser = facebookClient.fetchObject("/me",
        com.restfb.types.User.class);
    if (fbUser == null) {
      throw new RequestException("Could not retrieve user from Facebook");
    }
    System.out.println("FacebookLoginHandler.getFacebookUser(String), time = "
        + (System.currentTimeMillis() - startTime) + "ms");
    return fbUser;
  }

  /**
   * Constructs a plain new User object starting with the passed-in email
   * address.
   */
  private static User.Builder getNewUserBuilder(com.restfb.types.User fbUser,
      String fbAccessToken) {
    User.Builder newUserBuilder = User.newBuilder()
        .setId(GuidFactory.generate()).setFirstName(fbUser.getFirstName())
        .setLastName(fbUser.getLastName()).setFacebookId(fbUser.getId())
        .setFacebookAccessToken(fbAccessToken)
        .setCreateTime(System.currentTimeMillis())
        .setLastLoginTime(System.currentTimeMillis());
    if (!Strings.isNullOrEmpty(fbUser.getEmail())) {
      newUserBuilder.setEmail(fbUser.getEmail());
    }
    return newUserBuilder;
  }

  /**
   * Returns a NewsServer user builder object for the passed Facebook user
   * object, if we've already created a user object for the Facebook user
   * before.
   */
  private static User getExistingUser(com.restfb.types.User fbUser)
      throws DatabaseSchemaException {
    ListenableFuture<Iterable<User>> userByFacebookIdFuture = Database.with(
        User.class).getFuture(
        new QueryOption.WhereEquals("facebook_id", fbUser.getId()));
    ListenableFuture<Iterable<User>> userByEmailFuture = Database.with(
        User.class).getFuture(
        new QueryOption.WhereEquals("email", fbUser.getEmail()));
    try {
      User existingUser = Iterables
          .getFirst(userByFacebookIdFuture.get(), null);
      return (existingUser != null) ? existingUser : Iterables.getFirst(
          userByEmailFuture.get(), null);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns a Set of the names of the companies the given user has worked at.
   */
  private static Set<String> getCompanyNames(com.restfb.types.User fbUser) {
    Set<String> companyNames = Sets.newHashSet();
    for (Work work : fbUser.getWork()) {
      companyNames.add(work.getEmployer().getName());
    }
    return companyNames;
  }

  private static Iterable<String> split(String text) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    if (!Strings.isNullOrEmpty(text)) {
      for (String sentence : KeywordFinder.getInstance().getSentences(text)) {
        for (String token : KeywordFinder.getInstance().getTokens(sentence)) {
          if (!Strings.isNullOrEmpty(token)) {
            builder.add(KeywordUtils.cleanKeyword(token));
          }
        }
      }
    }
    return builder.build();
  }

  /**
   * Puts all the human-readable-text we have about the current user, at least
   * as far as professional interests go, into a Vector that we can use to
   * compare against industries.
   */
  private static Vector getFacebookUserVector(com.restfb.types.User fbUser) {
    List<WordFrequency.Builder> rawList = Lists.newArrayList();
    int companyScore = 50;
    for (String companyName : getCompanyNames(fbUser)) {
      for (String word : split(companyName)) {
        rawList.add(WordFrequency.newBuilder().setWord(word)
            .setFrequency(companyScore));
      }
      // Decrease company scores as they get further in the user's past.
      companyScore = Math.max(10, companyScore - 18);
    }
    for (Work work : fbUser.getWork()) {
      if (work.getPosition() != null) {
        for (String word : split(work.getPosition().getName())) {
          rawList
              .add(WordFrequency.newBuilder().setWord(word).setFrequency(10));
        }
      }
      for (String token : split(work.getDescription())) {
        rawList.add(WordFrequency.newBuilder().setWord(token).setFrequency(8));
      }
      if (work.getLocation() != null) {
        for (String token : split(work.getLocation().getName())) {
          rawList
              .add(WordFrequency.newBuilder().setWord(token).setFrequency(1));
        }
      }
    }
    for (Education education : fbUser.getEducation()) {
      for (NamedFacebookType concentration : education.getConcentration()) {
        for (String word : split(concentration.getName())) {
          rawList.add(WordFrequency.newBuilder().setWord(word).setFrequency(5));
        }
      }
      NamedFacebookType school = education.getSchool();
      if (school != null) {
        for (String word : split(school.getName())) {
          rawList.add(WordFrequency.newBuilder().setWord(word).setFrequency(1));
        }
      }
      for (EducationClass educationClass : education.getClasses()) {
        for (String word : split(educationClass.getName())) {
          rawList.add(WordFrequency.newBuilder().setWord(word).setFrequency(1));
        }
      }
    }
    NamedFacebookType location = fbUser.getLocation();
    if (location != null) {
      String locationName = location.getName();
      for (String word : split(locationName)) {
        rawList.add(WordFrequency.newBuilder().setWord(word).setFrequency(1));
      }
    }
    for (String aboutToken : split(fbUser.getAbout())) {
      rawList.add(WordFrequency.newBuilder().setWord(aboutToken)
          .setFrequency(1));
    }
    VectorData.Builder builder = VectorData.newBuilder();
    for (WordFrequency.Builder wordFrequencyBuilder : rawList) {
      wordFrequencyBuilder.setWord(KeywordUtils
          .cleanKeyword(wordFrequencyBuilder.getWord()));
      if (!Strings.isNullOrEmpty(wordFrequencyBuilder.getWord())) {
        builder.addWordFrequency(wordFrequencyBuilder);
      }
    }
    return new Vector(builder.build());
  }

  private static TopList<FeatureId, Double> getIndustryFeatureIds(
      com.restfb.types.User fbUser) {
    Vector facebookUserVector = getFacebookUserVector(fbUser);
    // for (WordFrequency wordFrequency :
    // facebookUserVector.toVectorData().getWordFrequencyList()) {
    // System.out.println(wordFrequency.getWord() + " x " +
    // wordFrequency.getFrequency());
    // }

    // If we have nothing to go off of, let the user choose for himself instead.
    TopList<FeatureId, Double> topIndustryFeatureIds = new TopList<>(4);
    if (facebookUserVector.getUniqueWordCount() < 5) {
      return topIndustryFeatureIds;
    }

    for (Feature feature : Feature.getAllFeatures()) {
      if (feature.getFeatureId() == FeatureId.SPORTS
          || feature.getFeatureId() == FeatureId.SUPERMARKETS
          || feature.getFeatureId() == FeatureId.LEISURE_TRAVEL_AND_TOURISM) {
        continue; // Ya, um, let's not... :)
      }

      if (feature.getFeatureId().getFeatureType() == FeatureType.INDUSTRY
          && feature instanceof VectorFeature) {
        double score = ((VectorFeature) feature)
            .score(facebookUserVector, 0 /* boost */);

        // Slightly punish Aviation since it tends to score well against
        // locations from people's profiles, since airports are usually
        // in those locations too.
        if (feature.getFeatureId() == FeatureId.AVIATION) {
          score -= 0.05;
        }

        // Punish Education since it matches people's universities, which do
        // help associate a user with an industry (e.g. Stanford -> tech), but
        // don't strongly correlate to whether the user's doing teaching for a
        // living.
        if (feature.getFeatureId() == FeatureId.EDUCATION) {
          score -= 0.1;
        }

        // Manual testing showed us that scores < 0.8 tended to be false
        // positives, even if they were the highest scores.
        if (score > 0.675) {
          topIndustryFeatureIds.add(feature.getFeatureId(), score);
        }
      }
    }
    return topIndustryFeatureIds;
  }

  /**
   * Returns a List of Interest objects for industries, companies, skills, and
   * other noun-like entities in the user's Facebook profile. Each Interest will
   * have a Source of FACEBOOK_PROFILE.
   */
  private static Iterable<Interest> getFacebookProfileInterests(
      com.restfb.types.User fbUser) throws DatabaseSchemaException {
    // Create a Map of company name to existing Entity objects, using either
    // our very-helpful-fuzzy-logic-friendly KeywordToEntityId table, or by
    // hoping the user happened to type an Entity we know exactly about.
    Set<String> companyNames = getCompanyNames(fbUser);
    Map<String, String> companyEntityIdMap = Maps.newHashMap();
    for (String companyName : companyNames) {
      String entityId = KeywordCanonicalizer.getEntityIdForKeyword(companyName);
      if (entityId != null) {
        companyEntityIdMap.put(companyName.toLowerCase(), entityId);
      }
    }
    for (Entity entity : Entities.getEntitiesByKeyword(Sets.difference(
        companyNames, ImmutableSet.copyOf(companyEntityIdMap.keySet())))) {
      // Here we're fetching Entities for any companies for which there weren't
      // KeywordToEntityId table rows for.
      companyEntityIdMap.put(entity.getKeyword().toLowerCase(), entity.getId());
    }

    // Start building interests.
    List<Interest> interests = Lists.newArrayList();
    for (String companyName : companyNames) {
      Interest.Builder companyInterestBuilder = Interest
          .newBuilder()
          .setId(GuidFactory.generate())
          .setType(InterestType.ENTITY)
          .setSource(InterestSource.FACEBOOK_PROFILE)
          .setCreateTime(System.currentTimeMillis())
          .setEntity(
              Entity
                  .newBuilder()
                  .setId(
                      companyEntityIdMap.containsKey(companyName.toLowerCase()) ? companyEntityIdMap
                          .get(companyName.toLowerCase()) : GuidFactory
                          .generate())
                  .setKeyword(companyName)
                  .setType(EntityType.COMPANY.toString())
                  .setSource(
                      companyEntityIdMap.containsKey(companyName.toLowerCase()) ? Source.DBPEDIA_INSTANCE_TYPE
                          : Source.USER).build());
      interests.add(companyInterestBuilder.build());
    }
    TopList<FeatureId, Double> industryFeatureIds = getIndustryFeatureIds(fbUser);
    if (Iterables.isEmpty(industryFeatureIds)) {
      // This is to prevent a crash bug in v1.0.0, where if the user has no
      // initial industries, UI comes up to ask them about their industry
      // behind a FTUE, and if the FTUE is then dismissed, there's an exception.
      // To fix it, we just add some general-purpose industries, for now.
      interests.add(Interest.newBuilder().setId(GuidFactory.generate())
          .setType(InterestType.INDUSTRY)
          .setIndustryCode(FeatureId.MANAGEMENT.getId())
          .setSource(InterestSource.DEFAULT_TO_PREVENT_CRASH)
          .setCreateTime(System.currentTimeMillis()).build());
      interests.add(Interest.newBuilder().setId(GuidFactory.generate())
          .setType(InterestType.INDUSTRY)
          .setIndustryCode(FeatureId.INTERNET.getId())
          .setSource(InterestSource.DEFAULT_TO_PREVENT_CRASH)
          .setCreateTime(System.currentTimeMillis()).build());
    } else {
      for (FeatureId industryFeatureId : getIndustryFeatureIds(fbUser)) {
        interests.add(Interest.newBuilder().setId(GuidFactory.generate())
            .setType(InterestType.INDUSTRY)
            .setIndustryCode(industryFeatureId.getId())
            .setSource(InterestSource.FACEBOOK_PROFILE)
            .setCreateTime(System.currentTimeMillis()).build());
      }
    }
    return interests;
  }

  public static User login(com.restfb.types.User fbUser, String fbAccessToken)
      throws RequestException, SocialException, DatabaseSchemaException,
      DatabaseRequestException {
    long startTime = System.currentTimeMillis();
    User user;
    User existingUser = getExistingUser(fbUser);
    if (existingUser != null) {
      user = existingUser
          .toBuilder()
          .setFacebookId(fbUser.getId())
          .setFacebookAccessToken(fbAccessToken)
          .setLastLoginTime(System.currentTimeMillis())
          .setEmail(
              !Strings.isNullOrEmpty(fbUser.getEmail()) ? fbUser.getEmail()
                  : existingUser.getEmail()).build();
      Database.update(user);
    } else {
      user = getNewUserBuilder(fbUser, fbAccessToken).addAllInterest(
          getFacebookProfileInterests(fbUser)).build();
      Database.insert(user);
    }
    System.out.println("FacebookLoginHandler.login(User, String), time = "
        + (System.currentTimeMillis() - startTime) + "ms");
    return user;
  }

  public static void main(String args[]) throws Exception {
    for (User user : Database.with(User.class).get(
        new QueryOption.WhereNotNull("facebook_access_token"))) {
      System.out.println("\n" + user.getEmail() + ":");
      try {
        com.restfb.types.User fbUser = FacebookLoginHandler
            .getFacebookUser(user.getFacebookAccessToken());
        TopList<FeatureId, Double> featureIdTopList = getIndustryFeatureIds(fbUser);
        for (FeatureId featureId : featureIdTopList) {
          System.out.println(featureId.getId() + ": " + featureId.getTitle()
              + " (" + featureIdTopList.getValue(featureId) + ")");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
