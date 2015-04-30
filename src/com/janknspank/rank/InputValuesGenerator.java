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
import com.janknspank.classifier.manual.ManualFeatureAcquisitions;
import com.janknspank.classifier.manual.ManualFeatureBigMoney;
import com.janknspank.classifier.manual.ManualFeatureFundraising;
import com.janknspank.classifier.manual.ManualFeatureIsList;
import com.janknspank.classifier.manual.ManualFeatureLaunches;
import com.janknspank.classifier.manual.ManualFeatureQuarterlyEarnings;
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

  /**
   * Returns a score for how relevant the passed article is to industries that
   * the user is following.  The scores go as follow:
   *  0 - No match
   *  0.5 - Match on something, but not a strong match
   *  0.8 - Strong match on 1 industry
   *  0.9 - Strong match on 2 industries
   *  1.0 - Strong match on 3 or more industries
   */
  public static double relevanceToUserIndustries(User user, Article article) {
    boolean matched = false;
    int numAbove90Percentile = 0;

    Map<FeatureId, Double> articleIndustryMap = getArticleIndustryMap(article);
    for (FeatureId industryFeatureId : UserIndustries.getIndustryFeatureIds(user)) {
      if (articleIndustryMap.containsKey(industryFeatureId)) {
        matched = true;
        if (articleIndustryMap.get(industryFeatureId) > 0.9) {
          numAbove90Percentile++;
        }
      }
    }
    switch (numAbove90Percentile) {
      case 0:
        return matched ? 0.5 : 0.0;
      case 1:
        return 0.8;
      case 2:
        return 0.9;
      default:
        // 3 or more matched industries.
        return 1.0;
    }
  }

  /**
   * Basically calculates the irrelevance of this article, as valued by the
   * number of industries this article is more about than industries the user
   * cares about.
   */
  public static double relevanceToNonUserIndustries(User user, Article article) {
    double highestSimilarityScoreThatMatchesUser = 0;
    Map<FeatureId, Double> articleIndustryMap = getArticleIndustryMap(article);
    for (FeatureId userIndustryFeatureId : UserIndustries.getIndustryFeatureIds(user)) {
      if (articleIndustryMap.containsKey(userIndustryFeatureId)) {
        highestSimilarityScoreThatMatchesUser = Math.max(highestSimilarityScoreThatMatchesUser,
            articleIndustryMap.get(userIndustryFeatureId));
      }
    }
    int numIndustriesMoreRelevant = 0;
    for (ArticleFeature feature : article.getFeatureList()) {
      FeatureId articleFeatureId = FeatureId.fromId(feature.getFeatureId());
      if (articleFeatureId != null
          && articleFeatureId.getFeatureType() == FeatureType.INDUSTRY
          && feature.getSimilarity() > (highestSimilarityScoreThatMatchesUser + 0.001)) {
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
    // If the article isn't relevant to the user's industries, then if there's
    // any contact name keyword string matches, they're probably false
    // positives.  As such, just score them as 0.
    if (relevanceToUserIndustries(user, article) < 0.1) {
      return 0;
    }

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
    return ArticleFeatures.getFeatureSimilarity(article, FeatureId.STARTUPS);
  }

  public static double relevanceToAcquisitions(Set<FeatureId> userIndustryFeatureIds, Article article) {
    return ManualFeatureAcquisitions.isRelevantToUser(userIndustryFeatureIds)
        ? ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_ACQUISITIONS)
        : 0;
  }

  public static double relevanceToLaunches(Set<FeatureId> userIndustryFeatureIds, Article article) {
    return ManualFeatureLaunches.isRelevantToUser(userIndustryFeatureIds)
        ? ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_LAUNCHES)
        : 0;
  }

  public static double relevanceToFundraising(Set<FeatureId> userIndustryFeatureIds, Article article) {
    return ManualFeatureFundraising.isRelevantToUser(userIndustryFeatureIds)
        ? ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_FUNDRAISING)
        : 0;
  }

  public static double relevanceToBigMoney(Set<FeatureId> userIndustryFeatureIds, Article article) {
    return ManualFeatureBigMoney.isRelevantToUser(userIndustryFeatureIds)
        ? ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_BIG_MONEY)
        : 0;
  }

  public static double relevanceToQuarterlyEarnings(
      Set<FeatureId> userIndustryFeatureIds, Article article) {
    return ManualFeatureQuarterlyEarnings.isRelevantToUser(userIndustryFeatureIds)
        ? ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_QUARTERLY_EARNINGS)
        : 0;
  }

  public static double relevanceToList(
      Set<FeatureId> userIndustryFeatureIds, Article article) {
    return ManualFeatureIsList.isRelevantToUser(userIndustryFeatureIds)
        ? ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_IS_LIST)
        : 0;
  }

  /**
   * If the user is following M&A, return the article similarity for that feature
   */
  public static double relevanceToMergersAndAcquisitions(
      Set<FeatureId> userIndustryFeatureIds, Article article) {
    return (userIndustryFeatureIds.contains(FeatureId.MERGERS_AND_ACQUISITIONS))
        ? ArticleFeatures.getFeatureSimilarity(article, FeatureId.MERGERS_AND_ACQUISITIONS)
        : 0;
  }

  /**
   * If the user is following Equity Investing, return the article similarity
   */
  public static double relevanceToEquityInvesting(
      Set<FeatureId> userIndustryFeatureIds, Article article) {
    return (userIndustryFeatureIds.contains(FeatureId.EQUITY_INVESTING))
        ? ArticleFeatures.getFeatureSimilarity(article, FeatureId.EQUITY_INVESTING)
        : 0;
  }
  
  /**
   * If the user is following Venture Capital, return the article similarity
   */
  public static double relevanceToVentureCapital(
      Set<FeatureId> userIndustryFeatureIds, Article article) {
    return (userIndustryFeatureIds.contains(FeatureId.VENTURE_CAPITAL))
        ? ArticleFeatures.getFeatureSimilarity(article, FeatureId.VENTURE_CAPITAL)
        : 0;
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
   * Returns an adjusted value for an article's score against a given feature
   * ID.  The value is adjusted such that scores less than 0.6667 receive 0s,
   * then scores in the remaining [0.6667, 1.0] range receive scores between
   * [0, 1], on a linear scale.
   */
  public static double getOptimizedFeatureValue(Article article, FeatureId featureId) {
    return Math.max(0, ArticleFeatures.getFeatureSimilarity(article, featureId) * 3 - 2);
  }
}