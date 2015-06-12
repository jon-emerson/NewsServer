package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.commons.io.IOUtils;

import com.janknspank.bizness.EntityType;
import com.janknspank.database.Database;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Entity;

public class MakeSureTop1000CitiesArePlaceEntities {
  public static void main(String args[]) throws Exception {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader("rawdata/top1000citiesword.txt"));
      String line;
      while ((line = reader.readLine()) != null) {
        Entity entity = Database.with(Entity.class).getFirst(
            new QueryOption.WhereEquals("keyword", line));
        if (entity == null) {
          entity = Database.with(Entity.class).getFirst(
              new QueryOption.WhereEquals("keyword", line.split(",")[0]));
        }
        if (entity == null) {
          System.out.println(line + " -> null");
        } else {
          EntityType type = EntityType.fromValue(entity.getType());
          if (!type.isA(EntityType.PLACE)) {
            System.out.println("NOT A PLACE: " + line + " - " + type.toString()
                + " (" + entity.getId() + ")");
          } else if (type != EntityType.CITY) {
            System.out.println(line + " -> " + entity.getId() + " (" + entity.getType() + ")");
          }
        }
      }
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
