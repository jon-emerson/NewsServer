package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.janknspank.bizness.Entities;
import com.janknspank.bizness.EntityType;
import com.janknspank.database.Database;
import com.janknspank.interpreter.KeywordUtils;
import com.janknspank.proto.Local.TokenToEntity;

/**
 * Using the local database, builds an indexed table of all Wikipedia entities.
 * The keys are the individual words from the Wikipedia entry.
 */
public class BuildLocalEntityMap {
  private static final Set<String> TOKEN_BLACKLIST = ImmutableSet.of(
      "of",
      "de",
      "and",
      "the",
      "in",
      "at",
      "for");

  private static boolean isValueableToken(String token) {
    return !TOKEN_BLACKLIST.contains(token.toLowerCase()) && token.length() > 2;
  }

  /** Helper method for creating the TokenToEntity table. */
  public static void main(String args[]) throws Exception {
    Database.with(TokenToEntity.class).createTable();

    BufferedReader reader = null;
    try {
      // We can do n-triples or n-quads here... For some reason I downloaded
      // quads.
      reader = new BufferedReader(new FileReader("dbpedia/instance_types_en.nt"));
      String line = reader.readLine();
      DbpediaInstanceType currentInstanceType = new DbpediaInstanceType();
      List<TokenToEntity> tokenToEntitiesToInsert = Lists.newArrayList();
      while (line != null) {
        if (line.startsWith("#")) {
          line = reader.readLine();
          continue;
        }
        DbpediaInstanceTypeLine instanceTypeLine = new DbpediaInstanceTypeLine(line);
        if (instanceTypeLine.isValid()) {
          if (!currentInstanceType.isLineRelevant(instanceTypeLine)) {
            if (currentInstanceType.isValuableEntity()) {
              String topicStr = currentInstanceType.getTopic();
              EntityType type = currentInstanceType.getEntityType();
              if (GetKeywordsFromDbpediaAbstracts.isRelevantEntityType(type) &&
                  topicStr.length() <= Entities.MAX_KEYWORD_LENGTH) {
                for (String token : topicStr.split("\\s+")) {
                  token = KeywordUtils.cleanKeyword(token);
                  if (isValueableToken(token)) {
                    tokenToEntitiesToInsert.add(TokenToEntity.newBuilder()
                        .setToken(token)
                        .setEntityKeyword(topicStr)
                        .setEntityType(type.toString())
                        .build());
                  }
                }
              }
            }
            currentInstanceType = new DbpediaInstanceType();
          }
          currentInstanceType.addLine(instanceTypeLine);

          if (tokenToEntitiesToInsert.size() > 500) {
            Database.insert(tokenToEntitiesToInsert);
            tokenToEntitiesToInsert.clear();
          }
        }
        line = reader.readLine();
      }

      Database.insert(tokenToEntitiesToInsert);

      // There's an off-by-1 error here: We lose the last entity.
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }
}
