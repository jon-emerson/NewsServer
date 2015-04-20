package com.janknspank.rank;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.bizness.UserIndustries;
import com.janknspank.bizness.UserInterests;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.nlp.KeywordCanonicalizer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.UserProto.AddressBookContact;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.LinkedInContact;
import com.janknspank.proto.UserProto.User;

/**
 * Helper class to generate input node values for the Scorer.
 */
public class InputValuesGenerator {
  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s|\\xA0)+");
  private static final ContactsKeywordsCache CONTACTS_KEYWORDS_CACHE = new ContactsKeywordsCache();

  private static class ContactsKeywordsCacheItem {
    private String userId = null;
    private long lastUpdatedMillis = 0;
    private Set<String> contactsKeywords;
  }

  /**
   * Poor-mans @RequestScoped cache for a user's contact keywords.  Really wish
   * I had Guice for this instead.  This allows us to calculate keywords for the
   * current user's contacts only once per /get_articles (etc.) request.
   */
  private static class ContactsKeywordsCache extends ThreadLocal<ContactsKeywordsCacheItem> {
    @Override
    protected ContactsKeywordsCacheItem initialValue() {
      return null;
    }

    public Set<String> getContactsKeywords(User user) {
      if (!user.hasId()) {
        throw new IllegalStateException("User ID cannot be null");
      }
      ContactsKeywordsCacheItem cacheItem = get();
      if (cacheItem != null
          && user.getId().equals(cacheItem.userId)
          && (System.currentTimeMillis() - cacheItem.lastUpdatedMillis)
              < TimeUnit.MINUTES.toMillis(1)) {
        return get().contactsKeywords;
      }

      cacheItem = new ContactsKeywordsCacheItem();
      cacheItem.userId = user.getId();
      cacheItem.lastUpdatedMillis = System.currentTimeMillis();
      Set<String> contactsKeywords = Sets.newHashSet();
      for (Interest interest : UserInterests.getInterests(user)) {
        if (interest.getType() == InterestType.ENTITY
            && EntityType.fromValue(interest.getEntity().getType()).isA(EntityType.PERSON)) {
          contactsKeywords.add(interest.getEntity().getKeyword());
        }
        if (interest.getType() == InterestType.ADDRESS_BOOK_CONTACTS) {
          for (AddressBookContact contact : user.getAddressBookContactList()) {
            contactsKeywords.add(contact.getName());
          }
        }
        if (interest.getType() == InterestType.LINKED_IN_CONTACTS) {
          for (LinkedInContact contact : user.getLinkedInContactList()) {
            contactsKeywords.add(contact.getName());
          }
        }
      }
      cacheItem.contactsKeywords = contactsKeywords;
      this.set(cacheItem);
      return contactsKeywords;
    }
  };

  public static double relevanceToUserIndustries(User user, Article article) {
    Map<FeatureId, Double> articleIndustryMap = getArticleIndustryMap(article);
    double userRelevance = 0;
    for (FeatureId industryFeatureId : UserIndustries.getIndustryFeatureIds(user)) {
      if (articleIndustryMap.containsKey(industryFeatureId)) {
        userRelevance = Math.max(userRelevance, articleIndustryMap.get(industryFeatureId));
      }
    }
    return userRelevance;
  }

  /**
   * Basically calculates the irrelevance of this article, as valued by the
   * number of industries this article is more about than industries the user
   * cares about.
   */
  public static double relevanceToNonUserIndustries(User user, Article article) {
    int numIndustriesMoreRelevant = 0;
    double relevanceToUserIndustries = relevanceToUserIndustries(user, article);
    for (ArticleFeature feature : article.getFeatureList()) {
      if (feature.getSimilarity() >= relevanceToUserIndustries
          && FeatureId.fromId(feature.getFeatureId()).getFeatureType() == FeatureType.INDUSTRY) {
        numIndustriesMoreRelevant++;
      }
    }
    return Math.min(1, (numIndustriesMoreRelevant * 0.1));
  }

  public static double relevanceOnFacebook(User user, Article article) {
    SocialEngagement engagement = SocialEngagements.getForArticle(
        article, SocialEngagement.Site.FACEBOOK);
    return (engagement == null) ? 0 : engagement.getShareScore();
  }

  public static double relevanceOnTwitter(User user, Article article) {
    SocialEngagement engagement = SocialEngagements.getForArticle(
        article, SocialEngagement.Site.TWITTER);
    return (engagement == null) ? 0 : engagement.getShareScore();
  }

  public static double relevanceToContacts(User user, Article article) {
    double value = 0;
    Set<String> contactsKeywords = CONTACTS_KEYWORDS_CACHE.getContactsKeywords(user);
    for (String keyword : contactsKeywords) {
      if (article.getTitle().contains(keyword)) {
        value += 0.1;
      }
      if (article.getParagraph(0).contains(keyword)) {
        value += 0.05;
      }
      for (ArticleKeyword articleKeyword : article.getKeywordList()) {
        if (articleKeyword.getKeyword().equals(keyword)) {
          value += 0.1;
        } else if (WHITESPACE_PATTERN.matcher(articleKeyword.getKeyword()).find()
            && (articleKeyword.getKeyword().contains(keyword)
                || keyword.contains(articleKeyword.getKeyword()))) {
          value += 0.05;
        }
      }
    }
    return Math.min(1, value);
  }

  public static double relevanceToCompanyEntities(User user, Article article) {
    double value = 0;
    for (Interest interest : UserInterests.getInterests(user)) {
      if (interest.getType() == InterestType.ENTITY
          && EntityType.fromValue(interest.getEntity().getType()).isA(EntityType.ORGANIZATION)) {
        String keyword = interest.getEntity().getKeyword();
        if (article.getTitle().contains(keyword)) {
          value += 0.05;
        }
        if (article.getParagraph(0).contains(keyword)) {
          // There can be lots of false positives here, since we're just looking
          // for the keyword string anywhere within the paragraph, regardless of
          // tokenization / industry matches.
          value += 0.01;
        }
        for (ArticleKeyword articleKeyword : article.getKeywordList()) {
          if (articleKeyword.getKeyword().equals(keyword)
              || interest.getEntity().getId().equals(articleKeyword.getEntity().getId())) {
            if (articleKeyword.getStrength()
                  >= KeywordCanonicalizer.STRENGTH_FOR_TITLE_MATCH) {
              value += 0.1;
            } else if (articleKeyword.getStrength()
                  >= KeywordCanonicalizer.STRENGTH_FOR_FIRST_PARAGRAPH_MATCH) {
              value += 0.75;
            }
          } else if (articleKeyword.getParagraphNumber() <= 2
              && (articleKeyword.getKeyword().contains(keyword)
                  || keyword.contains(articleKeyword.getKeyword()))) {
            value += 0.025;
          }
        }
      }
    }
    return Math.min(1, value);
  }

  public static double relevanceToStartups(User user, Article article) {
    ArticleFeature startupFeature =
        ArticleFeatures.getFeature(article, FeatureId.STARTUPS);
    return (startupFeature == null) ? 0 : startupFeature.getSimilarity();
  }

  public static double relevanceToAcquisitions(User user, Article article) {
    ArticleFeature acquisitionFeature =
        ArticleFeatures.getFeature(article, FeatureId.MANUAL_HEURISTIC_ACQUISITIONS);
    return (acquisitionFeature == null) ? 0 : acquisitionFeature.getSimilarity();
  }

  public static double relevanceToLaunches(User user, Article article) {
    ArticleFeature acquisitionFeature =
        ArticleFeatures.getFeature(article, FeatureId.MANUAL_HEURISTIC_LAUNCHES);
    return (acquisitionFeature == null) ? 0 : acquisitionFeature.getSimilarity();
  }

  public static double relevanceToFundraising(User user, Article article) {
    ArticleFeature acquisitionFeature =
        ArticleFeatures.getFeature(article, FeatureId.MANUAL_HEURISTIC_FUNDRAISING);
    return (acquisitionFeature == null) ? 0 : acquisitionFeature.getSimilarity();
  }

  private static Map<FeatureId, Double> getArticleIndustryMap(Article article) {
    Map<FeatureId, Double> articleIndustryMap = Maps.newHashMap();
    for (ArticleFeature articleFeature : article.getFeatureList()) {
      articleIndustryMap.put(
          FeatureId.fromId(articleFeature.getFeatureId()), articleFeature.getSimilarity());
    }
    return articleIndustryMap;
  }

  /**
   * Returns a score for how related this article is to sports, entertainment,
   * or popular politics.
   */
  public static double relevanceToPopCulture(Article article) {
    double score = 0;
    for (ArticleFeature feature : new ArticleFeature[] {
        ArticleFeatures.getFeature(article, FeatureId.TOPIC_ENTERTAINMENT),
        ArticleFeatures.getFeature(article, FeatureId.TOPIC_SPORTS),
        ArticleFeatures.getFeature(article, FeatureId.TOPIC_POLITICS)
    }) {
      if (feature != null) {
        score = Math.max(score, feature.getSimilarity());
      }
    }
    return score;
  }

  public static double relevanceToMurderCrimeWar(Article article) {
    ArticleFeature murderCrimeWarFeature =
        ArticleFeatures.getFeature(article, FeatureId.TOPIC_MURDER_CRIME_WAR);
    // Only value relevance greater than 66.7%.
    return (murderCrimeWarFeature == null) ? 0 :
        Math.max(0, murderCrimeWarFeature.getSimilarity() * 3 - 2);
  }

  // Normalize any value to [0,1]
  // private static double sigmoid(double x) {
  //   return 1 / (1 + Math.exp(-x));
  // }
}