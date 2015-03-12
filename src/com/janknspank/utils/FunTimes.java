package com.janknspank.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Sets;
import com.janknspank.common.CsvReader;

public class FunTimes {
  private static Set<String> getCompanyNames(String csvFilename, int companyNameIndex) {
    Set<String> companyNames = Sets.newHashSet();
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(csvFilename);
      CsvReader reader = new CsvReader(fileReader);
      List<String> line = reader.readLine();
      while (line != null) {
        if (companyNames.contains(line.get(companyNameIndex))) {
          System.out.println("Internal dupe in " + csvFilename + ": " + line.get(companyNameIndex));
        }
        companyNames.add(line.get(companyNameIndex));
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new Error(e);
    } finally {
      IOUtils.closeQuietly(fileReader);
    }
    return companyNames;
  }

  public static void main(String args[]) {
    Set<String> bigCompanies = Sets.newHashSet();
    bigCompanies.addAll(getCompanyNames("rawdata/nasdaq.csv", 1));
    bigCompanies.addAll(getCompanyNames("rawdata/nyse.csv", 1));
    bigCompanies.addAll(getCompanyNames("rawdata/fortune-500.csv", 1));
    System.out.println("Big companies size: " + bigCompanies.size());
    for (String company : getCompanyNames("rawdata/inc-5000-list.csv", 9)) {
      if (!bigCompanies.contains(company)) {
        System.out.println("No listing for: \"" + company + "\"");
      }
    }
  }
}
