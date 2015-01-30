package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

import com.google.common.collect.Lists;
import com.janknspank.database.Database;
import com.janknspank.proto.Core.Entity;

public class BuildEntityTable {
  /** Helper method for creating the Article table. */
  public static void main(String args[]) throws Exception {
    BufferedReader reader = null;
    int entityCount = 0;
    try {
      reader = new BufferedReader(new FileReader("dbpedia/instance_types_en.nt"));
      String line = reader.readLine();
      DbpediaInstanceType currentInstanceType = new DbpediaInstanceType();
      List<Entity> entitiesToInsert = Lists.newArrayList();
      while (line != null) {
        if (line.startsWith("#")) {
          line = reader.readLine();
          continue;
        }
        DbpediaInstanceTypeLine instanceTypeLine = new DbpediaInstanceTypeLine(line);
        if (instanceTypeLine.isValid()) {
          if (!currentInstanceType.isLineRelevant(instanceTypeLine)) {
            if (currentInstanceType.isValuableEntity()) {
              entityCount++;
//              if (entityCount % 10000 == 0) {
//                System.out.print(".");
//              }
              entitiesToInsert.add(currentInstanceType.createEntity());
            }
            currentInstanceType = new DbpediaInstanceType();
          }
          currentInstanceType.addLine(instanceTypeLine);
        }
        if (entitiesToInsert.size() == 250) {
          System.out.println(Database.insert(entitiesToInsert) + " rows inserted");
          entitiesToInsert.clear();
        }
        line = reader.readLine();
      }
      if (currentInstanceType.isValuableEntity()) {
        entitiesToInsert.add(currentInstanceType.createEntity());
      }
      System.out.println(Database.insert(entitiesToInsert) + " rows inserted");
      System.out.println("Total row count: " + entityCount);
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }
}
