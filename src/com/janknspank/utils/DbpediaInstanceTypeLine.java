package com.janknspank.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.janknspank.bizness.EntityType;

public class DbpediaInstanceTypeLine {
  static final Pattern SUBTOPIC_PATTERN = Pattern.compile("([^\\(]+)\\(([^\\)]+)\\)$");
  private final String[] tokens;
  private final String articleName;
  private String topic = null;
  private String subtopic = null;
  private EntityType entityType = null;

  public DbpediaInstanceTypeLine(String line) throws UnsupportedEncodingException {
    tokens = line.trim().split(" ");

    // Find the topic.
    if (!tokens[0].startsWith("<http://dbpedia.org/resource/")) {
      throw new RuntimeException("Illegal first token: " + line);
    }
    articleName =
        tokens[0].substring("<http://dbpedia.org/resource/".length(), tokens[0].length() - 1);
    if (articleName.matches(".+\\_\\_[0-9]+$")) {
      // Ignore versioned topics.  These are useless to us, they're just
      // older versions of articles that exist in other forms.  (I think
      // that's what they are?)
      return;
    }
    String rawTopic = StringUtils.replace(articleName, "\\'", "'");
    rawTopic = StringUtils.replace(rawTopic, "_", " ");
    rawTopic = URLDecoder.decode(rawTopic, "UTF-8");
    Matcher subtopicMatcher = SUBTOPIC_PATTERN.matcher(rawTopic);
    if (subtopicMatcher.matches()) {
      rawTopic = subtopicMatcher.group(1).trim();
      subtopic = subtopicMatcher.group(2).trim();
    }
    this.topic = rawTopic;

    this.entityType = EntityType.fromOntology(tokens[2].substring(1, tokens[2].length() - 1));
  }

  public boolean isValid() {
    return (tokens.length >= 3 && tokens.length <= 5) && topic != null;
  }

  public String getArticleName() {
    return articleName;
  }

  public String getTopic() {
    return topic;
  }

  public String getSubtopic() {
    return subtopic;
  }

  public EntityType getEntityType() {
    return entityType;
  }
}
