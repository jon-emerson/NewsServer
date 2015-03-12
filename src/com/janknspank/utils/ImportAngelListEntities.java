package com.janknspank.utils;

import java.io.FileReader;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.api.client.util.Lists;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.common.CsvReader;
import com.janknspank.database.Database;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.CoreProto.Entity.Source;

public class ImportAngelListEntities {

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
    FileReader fileReader = null;
    try {
      fileReader = new FileReader("angellist/startups.csv");
      CsvReader reader = new CsvReader(fileReader);
      List<String> line = reader.readLine();
      line = reader.readLine(); // Ignore the first line.
      List<Entity> entitiesToCreate = Lists.newArrayList();
      boolean pastBreakingPoint = false;
      while (line != null) {
        if (line.get(2).length() > 0) {
          String companyName = getCleanCompanyName(line);
          if (companyName.length() <= 100 && !pastBreakingPoint) {
            line = reader.readLine();
            continue;
          }
          if (companyName.length() > 100) {
            companyName = companyName.substring(0, 100);
          }
          pastBreakingPoint = true;
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
        line = reader.readLine();
      }
      Database.insert(entitiesToCreate);
    } finally {
      IOUtils.closeQuietly(fileReader);
    }
  }
}
