package com.janknspank.utils;

import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.commons.io.IOUtils;

import com.janknspank.bizness.EntityType;
import com.janknspank.database.Database;
import com.janknspank.proto.CoreProto.Entity;

public class ShouldBeCities {
  public static void main(String args[]) throws Exception {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader("shouldbecities.txt"));
      String line = null;
      while ((line = reader.readLine()) != null) {
        Entity entity = Database.with(Entity.class).get(line);
        if (entity != null) {
          Database.update(entity.toBuilder()
              .setImportance(Math.max(1, entity.getImportance()))
              .setType(EntityType.CITY.toString())
              .build());
          System.out.println(".");
        } else {
          System.out.println("x");
        }
      }
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

}
