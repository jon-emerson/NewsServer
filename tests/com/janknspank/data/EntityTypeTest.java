package com.janknspank.data;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.janknspank.common.Asserts;

public class EntityTypeTest {
  @Test
  public void testUniqueValuesAndOntologies() throws Exception {
    Set<String> values = Sets.newHashSet();
    Set<String> ontologies = Sets.newHashSet();
    for (EntityType et : EntityType.values()) {
      Asserts.assertTrue(!values.contains(et.toString()),
          "Entity type value should be unique: " + et.toString());
      Asserts.assertTrue(!ontologies.contains(et.getOntology()),
          "Entity type ontology should be unique: " + et.getOntology());
      values.add(et.toString());
      ontologies.add(et.getOntology());
    }
  }
}
