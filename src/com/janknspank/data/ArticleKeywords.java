package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.janknspank.dom.InterpretedData;
import com.janknspank.proto.Core;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.ArticleKeyword;

/**
 * Helper class that manages storing and retrieving which keywords are
 * associated with which articles.
 */
public class ArticleKeywords {
  public static final int MAX_KEYWORD_LENGTH;
  static {
    int keywordLength = 0;
    for (FieldDescriptor field :
        ArticleKeyword.getDefaultInstance().getDescriptorForType().getFields()) {
      if (JavaType.STRING == field.getJavaType()) {
        if ("keyword".equals(field.getName())) {
          keywordLength = field.getOptions().getExtension(Core.stringLength);
        }
      }
    }
    if (keywordLength == 0) {
      throw new IllegalStateException("Could not find length of keyword field");
    }
    MAX_KEYWORD_LENGTH = keywordLength;
  }

  private static final Pattern NUMBER_PATTERN_1 = Pattern.compile("^[0-9]+$");
  private static final Pattern NUMBER_PATTERN_2 = Pattern.compile("^[0-9]+\\-");
  private static final Set<String> BLACKLIST = Sets.newHashSet();
  static {
    for (String keyword : new String[] {
        "and",
        "apartments",
        "business school",
        "business schools",
        "bw business schools page",
        "capitals",
        "ceo",
        "commodities",
        "company",
        "corrections",
        "economy",
        "economic",
        "education",
        "financial calculator",
        "financial calculators",
        "graduate reviews",
        "hedge funds",
        "internet",
        "inequality",
        "insurance",
        "jobs",
        "leadership",
        "lifestyle",
        "litigation",
        "markets",
        "media",
        "mobile",
        "news",
        "party",
        "pm",
        "profile",
        "restaurants",
        "retail",
        "retailing",
        "shopping",
        "show",
        "sports",
        "stocks",
        "stock market",
        "teams",
        "tech",
        "technology",
        "that",
        "the",
        "their",
        "there",
        "they",
        "top business schools worldwide",
        "university",
        "up",
        "video",
        "web",
        "why",
        "with",
        "yet"}) {
      BLACKLIST.add(keyword.toLowerCase());
    }
  }

  @VisibleForTesting
  static boolean isValidKeyword(String keyword) {
    keyword = keyword.trim().toLowerCase();
    if (keyword.length() < 2 ||
        NUMBER_PATTERN_1.matcher(keyword).find() ||
        NUMBER_PATTERN_2.matcher(keyword).find() ||
        keyword.contains("…") ||
        keyword.startsWith("#") ||
        keyword.startsWith("bloomberg ") ||
        keyword.startsWith("mba ") ||
        keyword.endsWith("@bloomberg") ||
        keyword.endsWith(" jobs") ||
        keyword.endsWith(" news") ||
        keyword.endsWith(" profile") ||
        keyword.endsWith(" restaurants") ||
        keyword.endsWith(" trends") ||
        (keyword.contains("&") && keyword.contains(";")) || // XML entities.
        keyword.length() > MAX_KEYWORD_LENGTH) {
      return false;
    }
    return !BLACKLIST.contains(keyword);
  }

  @VisibleForTesting
  static String cleanKeyword(String keyword) {
    keyword = keyword.trim();
    if (keyword.endsWith(",") || keyword.endsWith(";") || keyword.endsWith("-")) {
      keyword = keyword.substring(0, keyword.length() - 1);
    }
    if (keyword.endsWith(".") && StringUtils.countMatches(keyword, ".") == 1) {
      keyword = keyword.substring(0, keyword.length() - 1);
    }
    if (keyword.endsWith("-based")) {
      keyword = keyword.substring(0, keyword.length() - "-based".length());
    }

    // Since we want to canonicalize as much as we can, and have nice short
    // keywords, for now convert possessive objects to only their posessive
    // part, dropping the rest.  This does mean we lose significant information
    // about the keyword, but perhaps(?) the canonicalization is worth it.
    if (keyword.contains("’")) {
      keyword = keyword.substring(0, keyword.indexOf("’"));
    }
    if (keyword.contains("'")) {
      keyword = keyword.substring(0, keyword.indexOf("'"));
    }

    if (!Character.isUpperCase(keyword.charAt(0))) {
      keyword = WordUtils.capitalizeFully(keyword);
      keyword = keyword.replaceAll("Aol", "AOL");
      keyword = keyword.replaceAll("Ios", "iOS");
      keyword = keyword.replaceAll("Ipad", "iPad");
      keyword = keyword.replaceAll("Iphone", "iPhone");
      keyword = keyword.replaceAll("Iwatch", "iWatch");
      keyword = keyword.replaceAll("Ipod", "iPod");
    }

    return keyword;
  }

  /**
   * Adds keywords found from the <meta> tags in an article's HTML.
   */
  public static void add(Article article, Set<String> keywords)
      throws DataInternalException, ValidationException {
    List<Message> articleKeywordList = Lists.newArrayList();
    for (String keyword : keywords) {
      if (isValidKeyword(keyword)) {
        articleKeywordList.add(ArticleKeyword.newBuilder()
            .setArticleId(article.getId())
            .setKeyword(cleanKeyword(keyword))
            .setStrength(1)
            .setType("k")
            .build());
      }
    }
    Database.insert(articleKeywordList);
  }

  /**
   * Adds keywords that were actually found by our NLP processor to the database
   * for the passed article.  The strengths given to these keywords are 5 times
   * their number of occurrences, capped at 20, so that they're significantly
   * stronger than <meta> keywords that were promoted (perhaps nefariously)
   * by the publisher.
   */
  public static void add(Article article, InterpretedData interpretedData)
      throws DataInternalException, ValidationException {
    List<Message> articleKeywordList = Lists.newArrayList();
    for (String location : interpretedData.getLocations()) {
      if (isValidKeyword(location)) {
        articleKeywordList.add(ArticleKeyword.newBuilder()
            .setArticleId(article.getId())
            .setKeyword(cleanKeyword(location))
            .setStrength(Math.max(20,
                interpretedData.getLocationCount(location) * 5))
            .setType("l")
            .build());
      }
    }
    for (String person : interpretedData.getPeople()) {
      if (isValidKeyword(person)) {
        articleKeywordList.add(ArticleKeyword.newBuilder()
            .setArticleId(article.getId())
            .setKeyword(cleanKeyword(person))
            .setStrength(Math.max(20,
                interpretedData.getPersonCount(person) * 5))
            .setType("p")
            .build());
      }
    }
    for (String organization : interpretedData.getOrganizations()) {
      if (isValidKeyword(organization)) {
        articleKeywordList.add(ArticleKeyword.newBuilder()
            .setArticleId(article.getId())
            .setKeyword(cleanKeyword(organization))
            .setStrength(Math.max(20,
                interpretedData.getOrganizationCount(organization) * 5))
            .setType("o")
            .build());
      }
    }
    Database.insert(articleKeywordList);
  }

  /**
   * Returns all of the ArticleKeywords associated with any of the passed-in
   * articles.
   */
  public static List<ArticleKeyword> get(List<Article> articleList)
      throws DataInternalException {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM " + Database.getTableName(ArticleKeyword.class) +
        " WHERE article_id IN (");
    for (int i = 0; i < articleList.size(); i++) {
      sql.append(i == articleList.size() - 1 ? "?" : "?, ");
    }
    sql.append(")");

    try {
      PreparedStatement stmt = Database.getConnection().prepareStatement(sql.toString());
      for (int i = 0; i < articleList.size(); i++) {
        stmt.setString(i + 1, articleList.get(i).getId());
      }
      ResultSet result = stmt.executeQuery();
      List<ArticleKeyword> keywordList = Lists.newArrayList();
      while (!result.isAfterLast()) {
        ArticleKeyword keyword = Database.createFromResultSet(result, ArticleKeyword.class);
        if (keyword != null) {
          keywordList.add(keyword);
        }
      }
      return keywordList;
    } catch (SQLException e) {
      throw new DataInternalException("Could not read article keywords: " + e.getMessage(), e);
    }
  }

  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(ArticleKeyword.class)).execute();
  }
}
