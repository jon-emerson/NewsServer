package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.api.client.util.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.database.Database;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.Entity.Source;

public class ImportAngelListEntities {
  private static class CsvToken {
    private final String token;
    private final boolean endLine;
    private final boolean endDocument;

    private CsvToken(String token, boolean endLine, boolean endDocument) {
      this.token = token;
      this.endLine = endLine;
      this.endDocument = endDocument;
    }
  }

  private static CsvToken readToken(BufferedReader reader) {
    try {
      int i = reader.read();
      if (i == -1) {
        return new CsvToken("", true, true);
      }
      char c = (char) i;
      StringBuilder sb = new StringBuilder();
      if (c == '"') {
        // Read until we find a quote and comma or quote and endline.
        boolean inQuote = true;
        while (true) {
          i = reader.read();
          if (i == -1) {
            return new CsvToken(sb.toString(), true, true);
          }
          c = (char) i;
          if (!inQuote && c == ',') {
            return new CsvToken(sb.toString(), false, false);
          } else if (!inQuote && c == '\n') {
            return new CsvToken(sb.toString(), true, false);
          } else if (!inQuote && c == '"') {
            sb.append("\"").append(c);
            inQuote = true;
          } else if (inQuote && c == '"') {
            inQuote = false;
          } else {
            sb.append(c);
          }
        }
      } else if (c == '\n') {
        return new CsvToken("", true, false);
      } else if (c == ',') {
        return new CsvToken("", false, false);
      } else {
        // Read until we find a comma or endline.
        sb.append(c);
        while (true) {
          i = reader.read();
          if (i == -1) {
            return new CsvToken(sb.toString(), true, true);
          }
          c = (char) i;
          if (c == ',') {
            return new CsvToken(sb.toString(), false, false);
          } else if (c == '\n') {
            return new CsvToken(sb.toString(), true, false);
          }
          sb.append(c);
        }
      }
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  @VisibleForTesting
  static List<String> readLine(BufferedReader reader) {
    List<String> components = Lists.newArrayList();
    while (true) {
      CsvToken line = readToken(reader);
      components.add(line.token);
      if (line.endDocument) {
        return (components.size() == 1 && line.token.length() == 0) ? null : components;
      }
      if (line.endLine) {
        return components;
      }
    }
  }

  private static String getCleanCompanyName(List<String> line) {
    String companyName = line.get(1).trim();
    if (companyName.indexOf(",") > 0) {
      companyName = companyName.substring(0, companyName.indexOf(",")).trim();
    }
    if (companyName.indexOf("(") > 0) {
      companyName = companyName.substring(0, companyName.indexOf("(")).trim();
    }
    return companyName;
  }

  public static void main(String args[]) throws Exception {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader("angellist/startups.csv"));
      List<String> line = readLine(reader);
      line = readLine(reader); // Ignore the first line.
      List<Entity> entitiesToCreate = Lists.newArrayList();
      while (line != null) {
        if (line.get(2).length() > 0) {
          String companyName = getCleanCompanyName(line);
          Entity existingEntity = Database.with(Entity.class).getFirst(
              new QueryOption.WhereLike("keyword", companyName + "%"));
          if (existingEntity == null) {
            Entity entity = Entity.newBuilder()
                .setId(GuidFactory.generate())
                .setAngelListId(Long.parseLong(line.get(0)))
                .setKeyword(companyName)
                .setType(EntityType.COMPANY.toString())
                .setSource(Source.ANGELLIST)
                .build();
            entitiesToCreate.add(entity);
          }
        }
        if (entitiesToCreate.size() > 100) {
          Database.insert(entitiesToCreate);
          entitiesToCreate.clear();
          System.out.print(".");
        }
        line = readLine(reader);
      }
      Database.insert(entitiesToCreate);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
