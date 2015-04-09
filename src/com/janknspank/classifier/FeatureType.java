package com.janknspank.classifier;

public enum FeatureType {
  UNKNOWN(0, "Unkown"),
  INDUSTRY(2, "Industry classification"),
  SKILL(3, "Improves or is related to a skill"),
  CHARACTER(4, "Some text characteristic"),
  MANUAL_HEURISTIC(5, "A heuristic about something we built manually"),
  TOPIC(6, "Something topical, e.g. entertainment or sports");

  private final int type;
  private final String description;

  private FeatureType(int type, String description) {
    this.type = type;
    this.description = description;
  }

  public int getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return name();
  }
}
