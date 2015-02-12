package com.janknspank.classifier;

public enum FeatureType {
  UNKNOWN(0, "Unkown"),
  SERVES_INTENT(1, "Serves a user intent"),
  INDUSTRY(2, "Industry classification"),
  SKILL(3, "Improves or is related to a skill"),
  CHARACTER(4, "Some text characteristic");

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
}