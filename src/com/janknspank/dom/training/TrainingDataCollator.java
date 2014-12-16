package com.janknspank.dom.training;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
      } else {
        if (child.getName().endsWith(".txt")) {
          children.add(child);
        }
      }
    }
    return children;
  }

  /**
   * Makes sure that the line is well-formed.
   */
  private static void validate(File file, String line) throws ValidationException {
    int depth = 0;
    try {
      for (String token : line.split(" ")) {
        if (token.startsWith("<START:")) {
          depth++;
          if (!token.endsWith(">")) {
            throw new ValidationException("Malformed <START> tag: " + token + "\n" +
                "Line: " + line + "\n" + "In file: " + file.getCanonicalPath());
          }
          if (depth != 1) {
            throw new ValidationException("<START> tag found while already in <START> tag\n" +
                "Line: " + line + "\n" + "In file: " + file.getCanonicalPath());
          }
        } else if (token.startsWith("<END")) {
          depth--;
          if (!token.equals("<END>")) {
            throw new ValidationException("Malformed <END> tag: " + token + "\n" +
                "Line: " + line + "\n" + "In file: " + file.getCanonicalPath());
          }
          if (depth != 0) {
            throw new ValidationException("<END> tag does not match <START>\n" +
                "Line: " + line + "\n" + "In file: " + file.getCanonicalPath());
          }
        } else if (token.contains("<START")) {
          throw new ValidationException("Error on line: <START... is not beginning of token.\n" +
              "Line: " + line + "\n" + "In file: " + file.getCanonicalPath());
        } else if (token.contains("<END")) {
          throw new ValidationException("Error on line: <END... is not beginning of token.\n" +
              "Line: " + line + "\n" + "In file: " + file.getCanonicalPath());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String args[]) throws Exception {
    BufferedWriter writer = new BufferedWriter(new FileWriter("trainingdata/collated.data"));
    int lineCount = 0;
    for (File file : getChildren(new File("trainingdata/"))) {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.startsWith("http://")) {
          validate(file, line);
          writer.write(line);
          writer.newLine();
          lineCount++;
        }
      }
      writer.newLine();
      reader.close();
    }
    writer.close();
    System.out.println("" + lineCount + " lines written");
  }
}
