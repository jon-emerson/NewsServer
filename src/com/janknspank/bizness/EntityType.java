package com.janknspank.bizness;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public enum EntityType {
  THING("t", null, "http://www.w3.org/2002/07/owl#Thing"),
  WORK("work", THING, "http://dbpedia.org/ontology/Work"),
  AGENT("agent", THING, "http://dbpedia.org/ontology/Agent"),
  PERSON("p", AGENT, "http://dbpedia.org/ontology/Person"),
  MUSICAL_WORK("mw", WORK, "http://dbpedia.org/ontology/MusicalWork"),
  ORGANIZATION("org", AGENT, "http://dbpedia.org/ontology/Organisation"),
  GROUP("group", ORGANIZATION, "http://dbpedia.org/ontology/Group"),
  ALBUM("album", MUSICAL_WORK, "http://dbpedia.org/ontology/Album"),
  ARTIST("art", PERSON, "http://dbpedia.org/ontology/Artist"),
  BAND("band", GROUP, "http://dbpedia.org/ontology/Band"),
  WRITTEN_WORK("ww", WORK, "http://dbpedia.org/ontology/WrittenWork"),
  BOOK("b", WRITTEN_WORK, "http://dbpedia.org/ontology/Book"),
  COMICS("comic", WRITTEN_WORK, "http://dbpedia.org/ontology/Comics"),
  COMPANY("c", ORGANIZATION, "http://dbpedia.org/ontology/Company"),
  EVENT("evt", THING, "http://dbpedia.org/ontology/Event"),
  FILM("film", WORK, "http://dbpedia.org/ontology/Film"),
  MUSICAL("muscl", WORK, "http://dbpedia.org/ontology/Musical"),
  MUSICAL_ARTIST("mart", ARTIST, "http://dbpedia.org/ontology/MusicalArtist"),
  PERIODICAL_LITERATURE("pli", WRITTEN_WORK, "http://dbpedia.org/ontology/PeriodicalLiterature"),
  NEWSPAPER("newsp", PERIODICAL_LITERATURE, "http://dbpedia.org/ontology/Newspaper"),
  POEM("poem", WRITTEN_WORK, "http://dbpedia.org/ontology/Poem"),
  PLAY("play", WRITTEN_WORK, "http://dbpedia.org/ontology/Play"),
  SOFTWARE("sw", WORK, "http://dbpedia.org/ontology/Software"),
  WEBSITE("web", WORK, "http://dbpedia.org/ontology/Website"),
  SPORTS_TEAM("steam", ORGANIZATION, "http://dbpedia.org/ontology/SportsTeam"),
  TELEVISION_SHOW("ts", WORK, "http://dbpedia.org/ontology/TelevisionShow"),
  VIDEO_GAME("vgame", SOFTWARE, "http://dbpedia.org/ontology/VideoGame"),
  JOURNALIST("j", PERSON, "http://dbpedia.org/ontology/Journalist"),
  WRITER("wr", ARTIST, "http://dbpedia.org/ontology/Writer"),
  MILITARY_PERSON("mp", PERSON, "http://dbpedia.org/ontology/MilitaryPerson"),
  PLACE("place", null, "http://dbpedia.org/ontology/Place"),
  ARCHITECTURAL_STRUCTURE("as", PLACE, "http://dbpedia.org/ontology/ArchitecturalStructure"),
  MILITARY_STRUCTURE("mils", ARCHITECTURAL_STRUCTURE,
      "http://dbpedia.org/ontology/MilitaryStructure"),
  INFRASTRUCTURE("is", ARCHITECTURAL_STRUCTURE, "http://dbpedia.org/ontology/Infrastructure"),
  POPULATED_PLACE("pp", PLACE, "http://dbpedia.org/ontology/PopulatedPlace"),
  SETTLEMENT("s", POPULATED_PLACE, "http://dbpedia.org/ontology/Settlement"),
  SOCIETAL_EVENT("sevt", EVENT, "http://dbpedia.org/ontology/SocietalEvent"),
  MILITARY_CONFLICT("mc", SOCIETAL_EVENT, "http://dbpedia.org/ontology/MilitaryConflict"),
  MILITARY_UNIT("mu", ORGANIZATION, "http://dbpedia.org/ontology/MilitaryUnit"),
  SPORTS_EVENT("spevt", SOCIETAL_EVENT, "http://dbpedia.org/ontology/SportsEvent"),
  REGION("re", POPULATED_PLACE, "http://dbpedia.org/ontology/Region"),
  NATURAL_PLACE("np", PLACE, "http://dbpedia.org/ontology/NaturalPlace"),
  CITY("city", SETTLEMENT, "http://dbpedia.org/ontology/City"),
  COUNTRY("cou", POPULATED_PLACE, "http://dbpedia.org/ontology/Country"),
  ISLAND("islnd", POPULATED_PLACE, "http://dbpedia.org/ontology/Island"),
  TERRITORY("terr", POPULATED_PLACE, "http://dbpedia.org/ontology/Territory"),
  CONTINENT("cont", POPULATED_PLACE, "http://dbpedia.org/ontology/Continent"),
  STATE("state", POPULATED_PLACE, "http://dbpedia.org/ontology/State"),
  STREET("str", POPULATED_PLACE, "http://dbpedia.org/ontology/Street"),
  TOWN("town", SETTLEMENT, "http://dbpedia.org/ontology/Town"),
  SCIENTIST("sc", PERSON, "http://dbpedia.org/ontology/Scientist"),
  STATION("sta", INFRASTRUCTURE, "http://dbpedia.org/ontology/Station"),
  AIRPORT("ap", INFRASTRUCTURE, "http://dbpedia.org/ontology/Airport"),
  POLITICAL_PARTY("ppa", ORGANIZATION, "http://dbpedia.org/ontology/PoliticalParty"),
  POLITICIAN("po", PERSON, "http://dbpedia.org/ontology/Politician"),
  ARCHITECT("arch", PERSON, "http://dbpedia.org/ontology/Architect"),
  OFFICE_HOLDER("oh", PERSON, "http://dbpedia.org/ontology/OfficeHolder"),
  ATHLETE("athl", PERSON, "http://dbpedia.org/ontology/Athlete"),
  ROYALTY("royal", PERSON, "http://dbpedia.org/ontology/Royalty"),
  BODY_OF_WATER("bow", NATURAL_PLACE, "http://dbpedia.org/ontology/BodyOfWater"),
  RECORD_LABEL("rl", COMPANY, "http://dbpedia.org/ontology/RecordLabel"),
  BROADCASTER("bc", ORGANIZATION, "http://dbpedia.org/ontology/Broadcaster"),
  RADIO_STATION("rs", BROADCASTER, "http://dbpedia.org/ontology/RadioStation"),
  MEMBER_OF_PARLIAMENT("mop", POLITICIAN, "http://dbpedia.org/ontology/MemberOfParliament"),
  PROTECTED_AREA("pa", PLACE, "http://dbpedia.org/ontology/ProtectedArea"),
  ADMINISTRATIVE_REGION("areg", REGION, "http://dbpedia.org/ontology/AdministrativeRegion"),
  BUILDING("build", ARCHITECTURAL_STRUCTURE, "http://dbpedia.org/ontology/Building"),
  CLERIC("cle", PERSON, "http://dbpedia.org/ontology/Cleric"),
  LEGISLATURE("leg", ORGANIZATION, "http://dbpedia.org/ontology/Legislature"),
  AIRLINE("air", COMPANY, "http://dbpedia.org/ontology/Airline"),
  ACADEMIC_JOURNAL("aj", PERIODICAL_LITERATURE, "http://dbpedia.org/ontology/AcademicJournal"),
  HOLIDAY("h", THING, "http://dbpedia.org/ontology/Holiday");

  private final String value;
  private final EntityType parent;
  private final String ontology;
  private final int depth;
  private static final Map<EntityType, Set<EntityType>> SELF_AND_CHILDREN;
  static {
    ImmutableMap.Builder<EntityType, Set<EntityType>> mapBuilder = ImmutableMap.builder();
    for (EntityType entityType : values()) {
      ImmutableSet.Builder<EntityType> setBuilder = ImmutableSet.builder();
      for (EntityType possibleSelfOrChildEntityType : values()) {
        if (possibleSelfOrChildEntityType.isA(entityType)) {
          setBuilder.add(possibleSelfOrChildEntityType);
        }
      }
      mapBuilder.put(entityType, setBuilder.build());
    }
    SELF_AND_CHILDREN = mapBuilder.build();
  }

  private EntityType(String value, EntityType parent, String ontology) {
    this.value = value;
    this.parent = parent;
    this.ontology = ontology;
    this.depth = (parent == null) ? 1 : parent.depth + 1;
  }

  public static EntityType fromValue(String value) {
    for (EntityType et : EntityType.values()) {
      if (et.value.equals(value)) {
        return et;
      }
    }
    return null;
  }

  public static EntityType fromOntology(String ontology) {
    for (EntityType et : EntityType.values()) {
      if (et.ontology.equals(ontology)) {
        return et;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return value;
  }

  public String getOntology() {
    return ontology;
  }

  public boolean isA(EntityType type) {
    EntityType testType = this;
    while (testType != null && testType != type) {
      testType = testType.parent;
    }
    return (testType == type);
  }

  public Set<EntityType> getAllVersions() {
    return SELF_AND_CHILDREN.get(this);
  }

  public int getDepth() {
    return depth;
  }
}
