package com.janknspank.dom;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Multisets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import com.janknspank.dom.parser.Node;

public class InterpretedData {
  private final List<Node> articleNodes;
  private final SortedMultiset<String> locations;
  private final SortedMultiset<String> people;
  private final SortedMultiset<String> organizations;

  private InterpretedData(List<Node> articleNodes,
      SortedMultiset<String> locations,
      SortedMultiset<String> people,
      SortedMultiset<String> organizations) {
    this.articleNodes = articleNodes;
    this.locations = locations;
    this.people = people;
    this.organizations = organizations;
  }

  public static class Builder {
    private List<Node> articleNodes;
    private final SortedMultiset<String> locations;
    private final SortedMultiset<String> people;
    private final SortedMultiset<String> organizations;

    public Builder() {
      locations = TreeMultiset.create();
      people = TreeMultiset.create();
      organizations = TreeMultiset.create();
    }

    public Builder setArticleNodes(List<Node> articleNodes) {
      this.articleNodes = articleNodes;
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
      return new InterpretedData(articleNodes, locations, people, organizations);
    }
  }

  public List<Node> getArticleNodes() {
    return articleNodes;
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
