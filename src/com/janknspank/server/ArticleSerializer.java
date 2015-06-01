package com.janknspank.server;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.UserInterests;
import com.janknspank.bizness.Users;
import com.janknspank.classifier.FeatureId;
import com.janknspank.common.TopList;
import com.janknspank.crawler.Interpreter;
import com.janknspank.crawler.RequiredFieldException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.nlp.KeywordCanonicalizer;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.server.soy.ViewFeedSoy;

public class ArticleSerializer {
  private static final Set<Integer> SHOW_IMAGE_OFFSETS = ImmutableSet.of(
      1, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
      73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149,
      151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227,
      229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307,
      311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389,
      397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467,
      479, 487, 491, 499);

  public static JSONArray serialize(Iterable<Article> articles,
      User user, boolean includeLinkedInContacts, boolean includeAddressBookContacts) {
    articles = putImageArticleFirst(articles);
    JSONArray articlesJson = new JSONArray();
    int i = 1;
    Set<String> userKeywordSet =
        getUserKeywordSet(user, includeLinkedInContacts, includeAddressBookContacts);
    Set<Integer> userIndustryFeatureIdIds = UserInterests.getUserIndustryFeatureIdIds(user);
    for (Article article : articles) {
      articlesJson.put(serialize(article, userKeywordSet, userIndustryFeatureIdIds, i++));
    }
    return articlesJson;
  }

  /**
   * @param user current user
   * @param includeLinkedInContacts whether linked in contacts should be FORCED
   *     ON because the user clicked "LinkedIn Contacts" in the UI, to
   *     specifically see articles about them, even though he might have
   *     LinkedIn contacts disabled on his account
   * @param includeAddressBookContacts whether address book contacts should be
   *     FORCED ON(!!!), just like LinkedIn contacts above
   * @return a string set for the names of people that should be considered
   *     contacts for the purpose of this request
   */
  public static Set<String> getUserKeywordSet(
      User user, boolean includeLinkedInContacts, boolean includeAddressBookContacts) {
    Set<InterestType> forcedInterests = Sets.newHashSet();
    if (includeLinkedInContacts) {
      forcedInterests.add(InterestType.LINKED_IN_CONTACTS);
    }
    if (includeAddressBookContacts) {
      forcedInterests.add(InterestType.ADDRESS_BOOK_CONTACTS);
    }
    return UserInterests.getUserKeywordSet(user, forcedInterests);
  }

  public static JSONObject serialize(
      Article article,
      Set<String> userKeywordSet,
      Set<Integer> userIndustryFeatureIdIds,
      int offset) {
    JSONObject articleJson = Serializer.toJSON(article);
    List<String> paragraphs = article.getParagraphList();
    articleJson.put("type", "article");
    articleJson.put("first_paragraphs", toJsonArray(
        paragraphs.subList(0, Math.min(1, paragraphs.size()))));
    articleJson.put("native_reader_enabled", isNativeReaderEnabled(article));
    articleJson.put("native_reader_v1.1_enabled", true);
    articleJson.put("keyword",
        Serializer.toJSON(getBestKeywords(article, userKeywordSet, userIndustryFeatureIdIds)));
    articleJson.put("client_date", getClientDate(article));
    if (!articleJson.has("origin")) {
      articleJson.put("origin", ViewFeedSoy.getDomain(article));
    }
    if (SHOW_IMAGE_OFFSETS.contains(offset) || showImageBecauseOfFeature(article)) {
      articleJson.put("show_image", true);
    }
    if (article.hasReasonIndustryCode()) {
      FeatureId featureId = FeatureId.fromId(article.getReasonIndustryCode());
      if (featureId != null) {
        articleJson.put("reason_industry_name", featureId.getTitle());
      }
    }

    // Replace the published time with the crawl time, since people often just
    // give a date for a publish time, so without this, the clients are showing
    // midnight as most articles' ages.
    articleJson.put("published_time", Long.toString(Articles.getPublishedTime(article)));

    return articleJson;
  }

  static boolean showImageBecauseOfFeature(Article article) {
    return ArticleFeatures.getFeatureSimilarity(article, FeatureId.ARTS) > 0.9
        || ArticleFeatures.getFeatureSimilarity(article, FeatureId.ARCHITECTURE_AND_PLANNING) > 0.9
        || ArticleFeatures.getFeatureSimilarity(article, FeatureId.APPAREL_AND_FASHION) > 0.9
        || ArticleFeatures.getFeatureSimilarity(article, FeatureId.PHOTOGRAPHY) > 0.9;
  }

  /**
   * Returns true if we have rights to show this article's content natively
   * in the mobile web application.
   */
  static boolean isNativeReaderEnabled(Article article) {
    // TODO(jonemerson): Trigger based on partnerships for launch.  But for
    // now, enable it for TechCrunch and TheVerge, only for testing purposes.
    return article.getUrl().startsWith("http://www.t");
  }

  public static JSONArray toJsonArray(List<String> strings) {
    JSONArray array = new JSONArray();
    for (String string : strings) {
      array.put(string);
    }
    return array;
  }

  /**
   * Transforms the passed Article iterable so that the first article tends to
   * have a good image associated with it.  For now, we don't know much about
   * good images vs. bad images, so we just make sure the first article has an
   * image (if possible).
   */
  private static Iterable<Article> putImageArticleFirst(final Iterable<Article> articles) {
    if (Iterables.isEmpty(articles)) {
      return Collections.emptyList();
    }
    final Article firstImageArticle = getFirstImageArticle(articles);
    if (firstImageArticle == null) {
      return articles;
    }
    return Iterables.concat(ImmutableList.of(firstImageArticle),
        Iterables.filter(articles, new Predicate<Article>() {
      @Override
      public boolean apply(Article article) {
        return article != firstImageArticle;
      }
    }));
  }

  private static Article getFirstImageArticle(final Iterable<Article> articles) {
    for (Article article : articles) {
      if (article.hasImageUrl()) {
        return article;
      }
    }
    return null;
  }

  private static Set<Integer> getFeatureIdsForArticle(Article article) {
    ImmutableSet.Builder<Integer> setBuilder = ImmutableSet.builder();
    for (ArticleFeature articleFeature : article.getFeatureList()) {
      setBuilder.add(articleFeature.getFeatureId());
    }
    return setBuilder.build();
  }

  /**
   * Returns the top 2 keywords for an article, as tuned to the current user.
   */
  public static Iterable<ArticleKeyword> getBestKeywords(
      Article article, Set<String> userKeywordSet, Set<Integer> userIndustryFeatureIdIds) {
    // Denotes whether this article matches up with any of the user's industries.
    // If it does, we should include people matches.  If it doesn't, we shouldn't
    // include people because they're probably false matches.
    boolean isIndustryMatch =
        !Collections.disjoint(userIndustryFeatureIdIds, getFeatureIdsForArticle(article));

    TopList<ArticleKeyword, Integer> topUserKeywords = new TopList<>(2);
    TopList<ArticleKeyword, Integer> topNonUserKeyword = new TopList<>(1);
    for (ArticleKeyword keyword : article.getKeywordList()) {
      EntityType entityType = EntityType.fromValue(keyword.getType());
      if (isIndustryMatch && userKeywordSet.contains(keyword.getKeyword().toLowerCase())) {
        if (keyword.getStrength() >= KeywordCanonicalizer.STRENGTH_FOR_FIRST_PARAGRAPH_MATCH
            || entityType.isA(EntityType.PERSON) || entityType == EntityType.THING) {
          topUserKeywords.add(keyword, keyword.getStrength());
        }
      } else if (keyword.getKeyword().length() < 25
          && keyword.getStrength() >= KeywordCanonicalizer.STRENGTH_FOR_FIRST_PARAGRAPH_MATCH
          && !entityType.isA(EntityType.PLACE)) {
        // Only include small-ish keywords because the super long ones are often
        // crap.
        topNonUserKeyword.add(keyword, keyword.getStrength());
      }
    }
    return Iterables.limit(Iterables.concat(topUserKeywords, topNonUserKeyword), 2);
  }

  public static String getClientDate(Article article) {
    String time;
    long age = System.currentTimeMillis() - Articles.getPublishedTime(article);
    if (age < TimeUnit.MINUTES.toMillis(60)) {
      time = Long.toString(TimeUnit.MINUTES.convert(age, TimeUnit.MILLISECONDS)) + "m";
    } else if (age < TimeUnit.HOURS.toMillis(24)) {
      time = Long.toString(TimeUnit.HOURS.convert(age, TimeUnit.MILLISECONDS)) + "h";
    } else {
      time = Long.toString(TimeUnit.DAYS.convert(age, TimeUnit.MILLISECONDS)) + "d";
    }
    return time;
  }

  public static void main(String args[])
      throws DatabaseSchemaException, FetchException, ParserException, RequiredFieldException,
          DatabaseRequestException, BiznessException {
    User user = Users.getByEmail(args[0]);
    User user2 = Users.getByUserId(args[0]);
    if (user == null) {
      user = user2;
    }
    if (user == null) {
      System.out.println("User not found: " + args[0]);
      System.exit(-1);
    }

    Article article = Interpreter.interpret(Url.newBuilder().setUrl(args[1]).build()).getArticle();
    Set<String> userKeywordSet = getUserKeywordSet(user, false, false);
    Set<Integer> userIndustryFeatureIdIds = UserInterests.getUserIndustryFeatureIdIds(user);
    JSONObject o = serialize(article, userKeywordSet, userIndustryFeatureIdIds, 0);
    System.out.println(o.toString(2));
  }
}
