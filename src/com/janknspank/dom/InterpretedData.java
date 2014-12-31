package com.janknspank.dom;

import java.util.Set;

import com.google.common.collect.Multisets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

public class InterpretedData {
  private final String articleBody;
  private final SortedMultiset<String> locations;
  private final SortedMultiset<String> people;
  private final SortedMultiset<String> organizations;

  private InterpretedData(String articleBody,
      SortedMultiset<String> locations,
      SortedMultiset<String> people,
      SortedMultiset<String> organizations) {
    this.articleBody = articleBody;
    this.locations = locations;
    this.people = people;
    this.organizations = organizations;
  }

  public static class Builder {
    private String articleBody;
    private final SortedMultiset<String> locations;
    private final SortedMultiset<String> people;
    private final SortedMultiset<String> organizations;

    public Builder() {
      locations = TreeMultiset.create();
      people = TreeMultiset.create();
      organizations = TreeMultiset.create();
    }

    public Builder setArticleBody(String articleBody) {
      this.articleBody = articleBody;
      return this;
    }

    public Builder addLocation(String location) {
      locations.add(location);
      return this;
    }

    public Builder addPerson(String person) {
      people.add(person);
      return this;
    }

    public Builder addOrganization(String organization) {
      organizations.add(organization);
      return this;
    }

    public InterpretedData build() {
      return new InterpretedData(articleBody, locations, people, organizations);
    }
  }

  public String getArticleBody() {
    return articleBody;
  }

  public Set<String> getLocations() {
    return Multisets.copyHighestCountFirst(locations).elementSet();
  }

  public int getLocationCount(String location) {
    return locations.count(location);
  }

  public Set<String> getPeople() {
    return Multisets.copyHighestCountFirst(people).elementSet();
  }

  public int getPersonCount(String person) {
    return people.count(person);
  }

  public Set<String> getOrganizations() {
    return Multisets.copyHighestCountFirst(organizations).elementSet();
  }

  public int getOrganizationCount(String organization) {
    return organizations.count(organization);
  }
}
