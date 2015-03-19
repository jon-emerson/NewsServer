package com.janknspank.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.api.client.util.Lists;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.janknspank.common.CsvReader;
import com.janknspank.database.Database;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.KeywordToEntityId;

/**
 * Uses the canonical entity frequency (basically, how often entities we're
 * tracking occur in the news), Fortune 500, NYSE, Nasdaq data to give
 * importance to entities so that newsier and larger companies auto-complete
 * first when searching for companies.
 */
public class UpdateEntityImportances {
  private static Map<String, Integer> getFortune500Map() {
    Map<String, Integer> companyNames = Maps.newHashMap();
    FileReader fileReader = null;
    try {
      fileReader = new FileReader("rawdata/fortune-500.csv");
      CsvReader reader = new CsvReader(fileReader);
      List<String> line = reader.readLine();
      while (line != null) {
        companyNames.put(line.get(1), Integer.parseInt(line.get(0)));
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new Error(e);
    } finally {
      IOUtils.closeQuietly(fileReader);
    }
    return companyNames;
  }

  private static Map<String, Integer> getMostValuableBrands() {
    Map<String, Integer> companyNames = Maps.newHashMap();
    FileReader fileReader = null;
    try {
      fileReader = new FileReader("rawdata/mostvaluablebrands.csv");
      CsvReader reader = new CsvReader(fileReader);
      List<String> line = reader.readLine();
      while (line != null) {
        if (line.size() <= 1) {
          System.out.println("Skipping invalid line: " + Joiner.on(",").join(line));
          line = reader.readLine();
          continue;
        }
        companyNames.put(line.get(1), Integer.parseInt(line.get(0)));
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new Error(e);
    } finally {
      IOUtils.closeQuietly(fileReader);
    }
    return companyNames;
  }

  private static Set<String> getCompanyNames(String csvFilename, int companyNameIndex) {
    Set<String> companyNames = Sets.newHashSet();
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(csvFilename);
      CsvReader reader = new CsvReader(fileReader);
      List<String> line = reader.readLine();
      while (line != null) {
        if (line.size() <= companyNameIndex) {
          System.out.println("Skipping invalid line: " + Joiner.on(",").join(line));
          line = reader.readLine();
          continue;
        }
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

  public static void main2(String args[]) throws Exception {
    // Start with the Fortune 500.  The top company gets 550 importance points.
    // The bottom company gets 50.
    Map<String, Integer> companyScoreMap = getFortune500Map();
    for (String companyName : companyScoreMap.keySet()) {
      companyScoreMap.put(companyName, 551 - companyScoreMap.get(companyName));
    }

    // Add in everyone else whose listed on a stock exchange at 20 points.
    Map<String, Integer> mostValuableBrandsMap = getMostValuableBrands();
    for (String companyName : Iterables.concat(
        getCompanyNames("rawdata/nasdaq.csv", 1),
        getCompanyNames("rawdata/nyse.csv", 1),
        mostValuableBrandsMap.keySet())) {
      if (!companyScoreMap.containsKey(companyName)) {
        companyScoreMap.put(companyName, 20);
      }
    }
    List<String> companyNameList = Lists.newArrayList(companyScoreMap.keySet());

    // Boost most valuable brands by 1 to 100 based on how valuable they are.
    for (Map.Entry<String, Integer> mostValuableBrandEntry : mostValuableBrandsMap.entrySet()) {
      String companyName = mostValuableBrandEntry.getKey();
      int oldScore = companyScoreMap.get(companyName);
      int newScore = Math.max(100, oldScore) + (120 - mostValuableBrandEntry.getValue());
      companyScoreMap.put(companyName, newScore);
      System.out.println(companyName + " from " + oldScore + " to " + newScore);
    }

    int count = 0;
    for (int i = 0; i < companyNameList.size(); i += 100) {
      List<Entity> entitiesToUpdate = Lists.newArrayList();
      Iterable<Entity> entities = Database.with(Entity.class).get(
          new QueryOption.WhereEquals("keyword",
              companyNameList.subList(i, Math.min(companyNameList.size(), i + 100))));
      for (Entity entity : entities) {
        Integer companyScore = companyScoreMap.get(entity.getKeyword());
        if (companyScore != null) {
          entitiesToUpdate.add(entity.toBuilder()
              .setImportance(companyScore)
              .build());
          count++;
        }
      }
      System.out.println("going...");
      Database.update(entitiesToUpdate);
      entitiesToUpdate.clear();
    }
    System.out.println("Entities updated: " + count + ". Starting pass 2.");

    // Use the frequency of articles about each entity to boost their
    // importances.
    // NOTE(jonemerson): Having KeywordToEntityId be fresh is key to this
    // working well.
    Multiset<String> entityIdCount = HashMultiset.create();
    for (KeywordToEntityId keywordToEntityId : Database.with(KeywordToEntityId.class).get()) {
      if (keywordToEntityId.hasEntityId()) {
        entityIdCount.add(keywordToEntityId.getEntityId(), keywordToEntityId.getCount());
      }
    }
    List<Entity> entitiesToUpdate = Lists.newArrayList();
    for (Entity entity : Database.with(Entity.class).get(entityIdCount.elementSet())) {
      if (!entityIdCount.contains(entity.getId())) {
        continue;
      }
      entitiesToUpdate.add(entity.toBuilder()
          .setImportance(entity.getImportance() + (entityIdCount.count(entity.getId()) / 2) + 25)
          .build());
      if (entitiesToUpdate.size() > 100) {
        System.out.print("x");
        Database.update(entitiesToUpdate);
        entitiesToUpdate.clear();
      }
    }
    Database.update(entitiesToUpdate);
  }
}
