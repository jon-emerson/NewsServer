package com.janknspank.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.janknspank.bizness.EntityType;

public class DbpediaInstanceTypeTest {
  @Test
  public void testInstanceType() throws Exception {
    DbpediaInstanceTypeLine it = new DbpediaInstanceTypeLine(
        "<http://dbpedia.org/resource/Albert_Einstein> "
        + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
        + "<http://dbpedia.org/ontology/Person> .");
    assertEquals("Albert Einstein", it.getTopic());
    assertNull(it.getSubtopic());
    assertEquals(EntityType.PERSON, it.getEntityType());

    it = new DbpediaInstanceTypeLine(
        "<http://dbpedia.org/resource/Algorithms_(journal)> "
        + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
        + "<http://dbpedia.org/ontology/AcademicJournal> .");
    assertEquals("Algorithms", it.getTopic());
    assertEquals("journal", it.getSubtopic());
    assertTrue(it.getEntityType().isA(EntityType.WRITTEN_WORK));

    it = new DbpediaInstanceTypeLine(
        "<http://dbpedia.org/resource/Asociaci%C3%B3n_Alumni> "
        + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
        + "<http://dbpedia.org/ontology/Organisation> .");
    assertEquals("Asociación Alumni", it.getTopic());
    assertNull(it.getSubtopic());
    assertEquals(EntityType.ORGANIZATION, it.getEntityType());

    it = new DbpediaInstanceTypeLine(
        "<http://dbpedia.org/resource/The_Best_of_the_Pink_Floyd_/_Masters_of_Rock> "
        + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
        + "<http://dbpedia.org/ontology/Work> .");
    assertEquals("The Best of the Pink Floyd / Masters of Rock", it.getTopic());
    assertNull(it.getSubtopic());
    assertEquals(EntityType.WORK, it.getEntityType());

    it = new DbpediaInstanceTypeLine(
        "<http://dbpedia.org/resource/Staurolite> "
        + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
        + "<http://dbpedia.org/ontology/ChemicalSubstance> .");
    assertEquals("Staurolite", it.getTopic());
    assertNull(it.getSubtopic());
    assertEquals(null, it.getEntityType());
  }
}
