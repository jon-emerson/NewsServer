package com.janknspank.opennlp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.janknspank.data.ValidationException;

/**
 * Reads all the individual training data files and writes them out as a single
 * file, so that it can be used by OpenNLP's TokenNameFinderTrainer.
 */
public class TrainingDataCollator {
  private static List<File> getChildren(File file) {
    List<File> children = Lists.newArrayList();
    for (File child : file.listFiles()) {
      if (child.isDirectory()) {
        children.addAll(getChildren(child));
      } else if (child.getName().endsWith(".txt")) {
        children.add(child);
      }
    }
    return children;
  }

  private static String getFilePath(File file) {
    try {
      return file.getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the given line of annotated training data with only the specified
   * training type <START> and <END> tags retained.  Additionally, this code
   * validates that the training data is correctly formed.
   * @param type the type of annotations that should be retained
   * @param line the line of text to parse, validate, and return a cleaned
   *     version of
   * @param file the file we're parsing, for error announcements
   */
  private static String getLineForType(String type, String line, File file)
      throws ValidationException {
    int depth = 0;
    List<String> relevantTokens = Lists.newArrayList();
    String startType = "";
    for (String token : line.split(" ")) {
      if (token.startsWith("<START:")) {
        depth++;
        if (!token.endsWith(">")) {
          throw new ValidationException("Malformed <START> tag: " + token + "\n"
              + "Line: " + line + "\n" + "In file: " + getFilePath(file));
        }
        if (depth != 1) {
          throw new ValidationException("<START> tag found while already in <START> tag\n"
              + "Line: " + line + "\n" + "In file: " + getFilePath(file));
        }
        startType = token.substring("<START:".length(), token.indexOf(">"));
        if (type.equals(startType)) {
          relevantTokens.add(token);
        }
      } else if (token.startsWith("<END")) {
        depth--;
        if (!token.equals("<END>")) {
          throw new ValidationException("Malformed <END> tag: " + token + "\n"
              + "Line: " + line + "\n" + "In file: " + getFilePath(file));
        }
        if (depth != 0) {
          throw new ValidationException("<END> tag does not match <START>\n"
              + "Line: " + line + "\n" + "In file: " + getFilePath(file));
        }
        if (type.equals(startType)) {
          relevantTokens.add(token);
        }
      } else if (token.contains("<START")) {
        throw new ValidationException("Error on line: <START... is not beginning of token.\n"
            + "Line: " + line + "\n" + "In file: " + getFilePath(file));
      } else if (token.contains("<END")) {
        throw new ValidationException("Error on line: <END... is not beginning of token.\n"
            + "Line: " + line + "\n" + "In file: " + getFilePath(file));
      } else {
        relevantTokens.add(token);
      }
    }
    if (depth != 0) {
      throw new ValidationException("<START> has no <END> to match!\n"
          + "Line: " + line + "\n" + "In file: " + getFilePath(file));
    }
    return Joiner.on(" ").join(relevantTokens);
  }

  public static void main(String args[]) throws Exception {
    String[] types = {
        "location",
        "organization",
        "person",
    };
    for (String type : types) {
      BufferedWriter writer = new BufferedWriter(new FileWriter(
          "trainingdata/en-newsserver-" + type + ".train"));
      int lineCount = 0;
      for (File file : getChildren(new File("trainingdata/"))) {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.startsWith("http://")) {
            line = getLineForType(type, line, file);
            writer.write(line);
            writer.newLine();
            lineCount++;
          }
        }
        writer.newLine();
        reader.close();
      }
      writer.close();
      System.out.println("" + lineCount + " lines written for type " + type);
    }
  }
}
