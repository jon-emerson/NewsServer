package com.janknspank.bizness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Strings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.janknspank.classifier.FeatureId;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.Entity.Source;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.LinkedInProfile;
import com.janknspank.proto.UserProto.LinkedInProfile.Employer;
import com.janknspank.proto.UserProto.User.AuthenticationService;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserOrBuilder;
import com.janknspank.server.RequestException;

public class LinkedInLoginHandler {
  // See what fields are available here: https://developer.linkedin.com/docs/fields/basic-profile
  // For now, we only ask for what we need.
  private static final String PROFILE_URL = "https://api.linkedin.com/v1/people/~:("
      + Joiner.on(",").join(ImmutableList.of("id", "email-address", "first-name", "last-name",
          "industry", "positions", "picture-url"))
      + ")";

  private final String linkedInAccessToken;
  private final Future<DocumentNode> linkedInProfileDocumentFuture;
  private User updatedUser = null;

  public LinkedInLoginHandler(String linkedInAccessToken) {
    this.linkedInAccessToken = linkedInAccessToken;
    linkedInProfileDocumentFuture = getDocumentFuture(PROFILE_URL, linkedInAccessToken);
  }

  /**
   * Returns an existing user with the passed email address, if one exists in
   * the system.  Else, returns null.
   */
  private User.Builder getExistingUserBuilder(String email)
      throws DatabaseSchemaException {
    User user = Users.getByEmail(email);
    if (user != null) {
      return user.toBuilder()
          .setLinkedInAccessToken(linkedInAccessToken)
          .setLastLoginTime(System.currentTimeMillis());
    }
    return null;
  }

  /**
   * Constructs a plain new User object starting with the passed-in email
   * address.
   */
  private User.Builder getNewUserBuilder(String email) {
    User.Builder builder = User.newBuilder()
        .setId(GuidFactory.generate())
        .setLinkedInAccessToken(linkedInAccessToken)
        .setCreateTime(System.currentTimeMillis())
        .setLastLoginTime(System.currentTimeMillis())
        .addLast5AppUseTime(System.currentTimeMillis())
        .setAuthenticationService(AuthenticationService.LINKED_IN);
    if (!Strings.isNullOrEmpty(email)) {
      builder.setEmail(email);
    }
    return builder;
  }

  public synchronized User getUser()
      throws DatabaseRequestException, DatabaseSchemaException, BiznessException, RequestException {
    if (updatedUser == null) {
      updatedUser = getUpdatedUser();
    }
    return updatedUser;
  }

  public DocumentNode getLinkedInProfileDocument() throws BiznessException, RequestException {
    try {
      return linkedInProfileDocumentFuture.get();
    } catch (ExecutionException|InterruptedException e) {
      Throwables.propagateIfInstanceOf(e.getCause(), BiznessException.class);
      Throwables.propagateIfInstanceOf(e.getCause(), RequestException.class);
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a User object representing the current authenticated user, with all
   * Interest / Employer / Industry etc. fields updated based on the data we
   * received from LinkedIn.  This method handles writing the updated fields to
   * the database.
   * @throws RequestException 
   */
  private User getUpdatedUser()
      throws DatabaseRequestException, DatabaseSchemaException, BiznessException, RequestException {
    System.out.println("Entered getUpdatedUser");
    try {
      // Get the user's email address from the LinkedIn profile response.
      DocumentNode linkedInProfileDocument = linkedInProfileDocumentFuture.get();
      long startTime = System.currentTimeMillis();
      System.out.println("Processing profile...");

      // Get a User.Builder object we can start updating with the user's updated
      // LinkedIn profile and connections values.
      boolean isNewUser = false;
      String email = getEmail(linkedInProfileDocument);
      User.Builder userBuilder = getExistingUserBuilder(email);
      if (userBuilder == null) {
        isNewUser = true;
        userBuilder = getNewUserBuilder(email);
      }
      userBuilder.setFirstName(linkedInProfileDocument.findFirst("first-name").getFlattenedText());
      userBuilder.setLastName(linkedInProfileDocument.findFirst("last-name").getFlattenedText());

      System.out.println("Read existing user: " + (System.currentTimeMillis() - startTime) + "ms");

      // Update LinkedInProfile field on User object, including an updated set of Employers.
      long stepStartTime = System.currentTimeMillis();
      userBuilder.setLinkedInProfile(createLinkedInProfile(linkedInProfileDocument));
      System.out.println("setLinkedInProfile: " + (System.currentTimeMillis() - stepStartTime)
          + "ms");

      // Update Interests.
      stepStartTime = System.currentTimeMillis();
      Iterable<Interest> updatedInterests =
          getUpdatedInterests(userBuilder, linkedInProfileDocument);
      userBuilder.clearInterest();
      userBuilder.addAllInterest(updatedInterests);
      System.out.println("addAllInterest: " + (System.currentTimeMillis() - stepStartTime) + "ms");

      // Update LinkedIn profile photo URL.
      stepStartTime = System.currentTimeMillis();
      String linkedInProfilePhotoUrl = getLinkedInProfilePhotoUrl(linkedInProfileDocument);
      if (linkedInProfilePhotoUrl != null) {
        userBuilder.setLinkedInProfilePhotoUrl(linkedInProfilePhotoUrl);
      }
      System.out.println("setLinkedInProfilePhotoUrl: "
          + (System.currentTimeMillis() - stepStartTime) + "ms");

      System.out.println("Completed User update processing in "
          + (System.currentTimeMillis() - startTime) + "ms");
      startTime = System.currentTimeMillis();
      try {
        User user = userBuilder.build();
        if (isNewUser) {
          Database.insert(user);
        } else {
          Database.update(user);
        }
        System.out.println("Updated User in DB in "
            + (System.currentTimeMillis() - startTime) + "ms");
        return user;
      } catch (DatabaseRequestException e) {
        e.printStackTrace();
        throw new BiznessException("Error creating user: " + e.getMessage(), e);
      }
    } catch (ExecutionException|InterruptedException e) {
      Throwables.propagateIfInstanceOf(e.getCause(), BiznessException.class);
      Throwables.propagateIfInstanceOf(e.getCause(), DatabaseRequestException.class);
      Throwables.propagateIfInstanceOf(e.getCause(), DatabaseSchemaException.class);
      Throwables.propagateIfInstanceOf(e.getCause(), RequestException.class);
      throw new RuntimeException(e);
    }
  }

  private static String getEmail(DocumentNode linkedInProfileDocument) throws BiznessException {
    Node emailNode = linkedInProfileDocument.findFirst("email-address");
    if (emailNode == null) {
      throw new BiznessException("Could not get email from LinkedIn profile");
    }
    return emailNode.getFlattenedText();
  }

  private static String getLinkedInProfilePhotoUrl(DocumentNode linkedInProfileDocument) {
    Node pictureNode = linkedInProfileDocument.findFirst("picture-url");
    return (pictureNode == null) ? null : pictureNode.getFlattenedText();
  }

  /**
   * Constructs a new LinkedInProfile object for the current user, given their
   * LinkedIn Profile document object.
   */
  private LinkedInProfile createLinkedInProfile(DocumentNode linkedInProfileDocument) {
    LinkedInProfile.Builder linkedInProfileBuilder = LinkedInProfile.newBuilder()
        .setData(linkedInProfileDocument.toLiteralString())
        .setCreateTime(System.currentTimeMillis());
    List<Employer> employers = getEmployers(linkedInProfileDocument);
    if (employers.size() > 0) {
      linkedInProfileBuilder.setCurrentEmployer(employers.get(0));
    }
    if (employers.size() > 1) {
      linkedInProfileBuilder.addAllPastEmployer(employers.subList(1, employers.size()));
    }
    return linkedInProfileBuilder.build();
  }

  /**
   * Gets a list of the user's employers, past and present, ordered by end time
   * descending.
   */
  @VisibleForTesting
  static List<Employer> getEmployers(DocumentNode linkedInProfileDocument) {
    List<Employer> employers = Lists.newArrayList();
    for (Node node : linkedInProfileDocument.findAll("positions > position")) {
      Employer.Builder builder = Employer.newBuilder();
      builder.setName(node.findFirst("company > name").getFlattenedText());
      builder.setTitle(node.findFirst("title").getFlattenedText());
      Long startTime = makeDate(node.findFirst("startDate > year"),
          node.findFirst("startDate > month"));
      if (startTime != null) {
        builder.setStartTime(startTime);
      }
      Long endTime = makeDate(node.findFirst("endDate > year"),
          node.findFirst("endDate > month"));
      if (endTime != null) {
        builder.setEndTime(endTime);
      }
      employers.add(builder.build());
    }
    Collections.sort(employers, new Comparator<Employer>() {
      @Override
      public int compare(Employer o1, Employer o2) {
        return - Long.compare(
            o1.hasEndTime() ? o1.getEndTime() : Long.MAX_VALUE,
            o2.hasEndTime() ? o2.getEndTime() : Long.MAX_VALUE);
      }
    });
    return employers;
  }

  /**
   * Returns an updated list of Interests for the current user, replacing any
   * existing LinkedIn connections or profile interests with ones derived from
   * the user's latest LinkedIn API response documents.
   */
  private Iterable<Interest> getUpdatedInterests(UserOrBuilder user,
      DocumentNode linkedInProfileDocument) {
    Interest linkedInContactInterest = null;
    final Set<Integer> userIndustryFeatureIds = new HashSet<>();
    final Set<Interest> tombstonedEntityInterests = new HashSet<>();
    for (Interest interest : user.getInterestList()) {
      if (interest.getType() == InterestType.LINKED_IN_CONTACTS) {
        linkedInContactInterest = interest;
      } else if (interest.getType() == InterestType.INDUSTRY 
          && interest.getSource() == InterestSource.USER) {
        userIndustryFeatureIds.add(interest.getIndustryCode());
      } else if (interest.getType() == InterestType.ENTITY
          && interest.getSource() == InterestSource.TOMBSTONE) {
        tombstonedEntityInterests.add(interest);
      }
    }
    if (linkedInContactInterest == null) {
      linkedInContactInterest = Interest.newBuilder()
          .setId(GuidFactory.generate())
          .setType(InterestType.LINKED_IN_CONTACTS)
          .setSource(InterestSource.USER)
          .setCreateTime(System.currentTimeMillis())
          .build();
    }

    return Iterables.concat(
        Iterables.filter(user.getInterestList(), new Predicate<Interest>() {
          @Override
          public boolean apply(Interest interest) {
            return interest.getSource() != InterestSource.LINKED_IN_PROFILE
                && interest.getType() != InterestType.LINKED_IN_CONTACTS;
          }
        }),
        Iterables.filter(getLinkedInProfileInterests(linkedInProfileDocument),
            new Predicate<Interest>() {
              @Override
              public boolean apply(Interest interest) {
                // Filter out any industries from the linkedIn profile that the
                // user has already explicitly added.
                if (interest.getType() == InterestType.INDUSTRY 
                    && userIndustryFeatureIds.contains(interest.getIndustryCode())) {
                  return false;
                } else if (interest.getType() == InterestType.ENTITY) {
                  for (Interest tombstonedInterest : tombstonedEntityInterests) {
                    if (UserInterests.isConflict(tombstonedInterest, interest)) {
                      return false;
                    }
                  }
                }
                return true;
              }
            }),
        ImmutableList.of(linkedInContactInterest));
  }

  /**
   * Returns an FeatureId representing the current Industry specified in the passed
   * LinkedIn profile.
   */
  private FeatureId getLinkedInIndustryFeature(DocumentNode linkedInProfileDocument) {
    Node headlineNode = linkedInProfileDocument.findFirst("headline");
    if (headlineNode != null) {
      String headline = headlineNode.getFlattenedText().toLowerCase();
      if (headline.startsWith("ux ")
          || headline.contains(" ux ")
          || (headline.contains("ux") && headline.contains("design"))
          || headline.contains("user experience")
          || headline.contains("visual design")
          || headline.contains("ui design")
          || headline.contains("design director")
          || headline.contains("product design")) {
        return FeatureId.USER_EXPERIENCE;
      }
      if (headline.contains("electric") && headline.contains("engineer")) {
        return FeatureId.ELECTRICAL_ENGINEERING;
      }
    }
    Node industryNode = linkedInProfileDocument.findFirst("industry");
    if (industryNode != null) {
      return Industry.fromDescription(industryNode.getFlattenedText()).getFeatureId();
    }
    // Ehh what the hell, let's do this, who doesn't love the Internet? :)
    return FeatureId.INTERNET;
  }

  /**
   * Returns a List of Interest objects for industries, companies, skills, and
   * other noun-like entities in the user's LinkedIn Profile document.  Each
   * Interest will have a Source of LINKED_IN_PROFILE.
   * NOTE(jonemerson): Actually, this just does companies for now.  We found
   * that skills and locations added too much noise to the user's stream.
   */
  private Iterable<Interest> getLinkedInProfileInterests(DocumentNode linkedInProfileDocument) {
    Set<String> companyNames = Sets.newHashSet();
    for (Node companyNameNode : linkedInProfileDocument.findAll("position > company > name")) {
      companyNames.add(companyNameNode.getFlattenedText());
    }
    List<Interest> interests = Lists.newArrayList();
    for (String companyName : companyNames) {
      interests.add(Interest.newBuilder()
          .setId(GuidFactory.generate())
          .setType(InterestType.ENTITY)
          .setEntity(Entity.newBuilder()
              // TODO(jonemerson): Canonicalize these vs. the Entity table in
              // MySQL.
              .setId(GuidFactory.generate())
              .setKeyword(companyName)
              .setType(EntityType.COMPANY.toString())
              .setSource(Source.USER)
              .build())
          .setSource(InterestSource.LINKED_IN_PROFILE)
          .setCreateTime(System.currentTimeMillis())
          .build());
    }
    FeatureId industryFeatureId = getLinkedInIndustryFeature(linkedInProfileDocument);
    if (industryFeatureId != null) {
      interests.add(Interest.newBuilder()
              .setId(GuidFactory.generate())
              .setType(InterestType.INDUSTRY)
              .setIndustryCode(industryFeatureId.getId())
              .setSource(InterestSource.LINKED_IN_PROFILE)
              .setCreateTime(System.currentTimeMillis())
              .build());
    }
    return interests;
  }

  /**
   * Parses a Linked In API-style date format to a an epoch time Long.
   */
  private static Long makeDate(Node year, Node month) {
    if (year == null) {
      return null;
    }
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.set(Calendar.YEAR, Integer.parseInt(year.getFlattenedText()));
    if (month != null) {
      calendar.set(Calendar.MONTH, Integer.parseInt(month.getFlattenedText()) - 1);
    }
    return calendar.getTimeInMillis();
  }

  static Future<DocumentNode> getDocumentFuture(
      final String url, final String linkedInAccessToken) {
    try {
      final long startTime = System.currentTimeMillis();
      HttpRequest request =
          new NetHttpTransport().createRequestFactory().buildGetRequest(new GenericUrl(url));
      HttpHeaders headers = new HttpHeaders();
      headers.setAuthorization("Bearer " + linkedInAccessToken);
      request.setHeaders(headers);

      return Futures.transform(JdkFutureAdapters.listenInPoolThread(request.executeAsync()),
          new AsyncFunction<HttpResponse, DocumentNode>() {
            @Override
            public ListenableFuture<DocumentNode> apply(HttpResponse response)
                throws Exception {
              if (response.getStatusCode() != HttpServletResponse.SC_OK) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ByteStreams.copy(response.getContent(), baos);
                return Futures.immediateFailedFuture(
                    new RequestException("Bad access token.  Response code = " +
                        + response.getStatusCode() + "\n" + new String(baos.toByteArray())));
              }
              System.out.println(
                  "Received " + url + " in " + (System.currentTimeMillis() - startTime) + "ms");
              return Futures.immediateFuture(
                  DocumentBuilder.build(url, new InputStreamReader(response.getContent())));
            }
          });
    } catch (IOException e) {
      return Futures.immediateFailedFuture(
          new BiznessException("Error reading from LinkedIn: " + e.getMessage(), e));
    }
  }
}
