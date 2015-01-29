package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.SqlConnection;
import com.janknspank.proto.Local.LongAbstract;

public class BuildLongAbstractTable {
  public static class Abstract {
    private final List<String> tokens = Lists.newArrayList();
    private final String topic;
    private final String subtopic;

    public Abstract(String nqLine) {
      StringBuffer currentToken = new StringBuffer();
      boolean inBlock = false;
      boolean inQuote = false;
      for (int i = 0; i < nqLine.length(); i++) {
        char c = nqLine.charAt(i);
        if (inBlock && c == '>') {
          inBlock = false;
          tokens.add(currentToken.toString());
          currentToken.setLength(0);
          continue;
        }
        if (inQuote && c == '"') {
          inQuote = false;
          try {
            tokens.add(StringEscapeUtils.unescapeJava(currentToken.toString()));
          } catch (Exception e) {
            tokens.add(currentToken.toString());
          }
          currentToken.setLength(0);
          while (i < nqLine.length() && nqLine.charAt(i) != ' ') {
            i++; // Skip the @en part.
          }
          continue;
        }
        if (!inBlock && !inQuote && c == '<') {
          inBlock = true;
          continue;
        }
        if (!inBlock && !inQuote && c == '"') {
          inQuote = true;
          continue;
        }
        if (inBlock || inQuote) {
          currentToken.append(c);
        }
      }
      if (currentToken.length() > 0) {
        tokens.add(currentToken.toString());
      }

      try {
        String rawTopic = StringUtils.replace(getArticleName(), "\\'", "'");
        rawTopic = StringUtils.replace(rawTopic, "_", " ");
        rawTopic = URLDecoder.decode(rawTopic, "UTF-8");
        Matcher subtopicMatcher = DbpediaInstanceTypeLine.SUBTOPIC_PATTERN.matcher(rawTopic);
        if (subtopicMatcher.matches()) {
          this.topic = subtopicMatcher.group(1).trim();
          this.subtopic = subtopicMatcher.group(2).trim();
        } else {
          this.topic = rawTopic.trim();
          this.subtopic = null;
        }
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }

    }

    public String getArticleName() {
      if (tokens.size() == 0 || tokens.get(0).length() == 0) {
        return null;
      }
      String token = tokens.get(0);
      if (!token.startsWith("http://dbpedia.org/resource/")) {
        throw new IllegalStateException("Invalid article name token: " + token);
      }
      return token.substring("http://dbpedia.org/resource/".length());
    }

    public String getTopic() {
      return topic;
    }

    public String getSubtopic() {
      return subtopic;
    }

    public boolean hasSubtopic() {
      return subtopic != null;
    }

    public String getText() {
      return (tokens.size() < 3) ? null : tokens.get(2);
    }
  }

  public static Set<String> getExistingArticleNames() {
    PreparedStatement stmt = null;
    try {
      Set<String> articleNames = Sets.newHashSet();
      stmt = SqlConnection.xXprepareStatement(
          "SELECT article_name from " + Database.with(LongAbstract.class).getTableName());
      ResultSet result = stmt.executeQuery();
      while (result.next()) {
        articleNames.add(result.getString("article_name"));
      }
      result.close();
      return articleNames;
    } catch (Exception e) {
      throw new Error(e);
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {}
      }
    }
  }

  public static void main(String args[]) {
    try {
      Database.with(LongAbstract.class).createTable();
    } catch (Exception e) {
      throw new Error(e);
    }

    BufferedReader reader = null;
    try {
      // We can do n-triples or n-quads here... For some reason I downloaded quads.
      List<LongAbstract> longAbstractsToInsert = Lists.newArrayList();
      Set<String> previousArticleNames = Sets.newHashSet();

      reader = new BufferedReader(new FileReader("dbpedia/long_abstracts_en.nq"));
      String line = reader.readLine();
      final Set<String> existingArticleNames = getExistingArticleNames();
      final int maxTopicLength =
          Database.with(LongAbstract.class).getStringLength("topic");
      final int maxArticleNameLength =
          Database.with(LongAbstract.class).getStringLength("article_name");
      final int maxTextLength =
          Database.with(LongAbstract.class).getStringLength("text");
      while (line != null) {
        if (line.startsWith("#")) {
          line = reader.readLine();
          continue;
        }

        Abstract abs = new Abstract(line);
        if (existingArticleNames.contains(abs.getArticleName())) {
          line = reader.readLine();
          continue;
        }
        if (previousArticleNames.contains(abs.getArticleName())) {
          System.out.println("Warning: Article name already found: " + abs.getArticleName()
              + ". Skipping.");
          line = reader.readLine();
          continue;
        }
        previousArticleNames.add(abs.getArticleName());

        if (abs.getTopic().length() < maxTopicLength &&
            abs.getArticleName().length() < maxArticleNameLength &&
            abs.getText().length() < maxTextLength) {
          longAbstractsToInsert.add(LongAbstract.newBuilder()
              .setArticleName(abs.getArticleName())
              .setTopic(abs.getTopic())
              .setText(abs.getText())
              .build());
        }

        // If we have enough entities to insert or update, do the deed.
        if (longAbstractsToInsert.size() > 500) {
          Database.insert(longAbstractsToInsert);
          longAbstractsToInsert.clear();
        }

        line = reader.readLine();
      }

      // Insert the remaining stragglers.
      Database.insert(longAbstractsToInsert);

    } catch (IOException | DatabaseSchemaException | DatabaseRequestException e) {
      e.printStackTrace();

    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
