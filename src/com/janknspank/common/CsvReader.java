package com.janknspank.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import com.google.api.client.util.Lists;

/**
 * Class that reads CSV files and provides a .readLine() method that returns
 * each line as a List of component Strings.
 *
 * It is assumed that the first line of the CSV file is part of the input.
 * (That is, we don't handle it specially, it is a line.)
 */
public class CsvReader {
  private final BufferedReader reader;

  /**
   * @param reader The input to read the CSV from.  This is usually a
   *     FileReader.
   */
  public CsvReader(Reader reader) {
    this.reader = new BufferedReader(reader);
  }

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
          if (c == '\r') {
            // Ignore it.
          } else if (!inQuote && c == ',') {
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
          } else if (c == '\r') {
            continue; // Ignore it.
          }
          sb.append(c);
        }
      }
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  /**
   * Reads a line from the CSV file.  If the CSV file has reached its end, then
   * null is returned.
   */
  public List<String> readLine() {
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
}
