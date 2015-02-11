package com.janknspank.classifier;

import java.io.File;

public class IndustryFeature extends VectorFeature {
  private static final File VECTORS_DIRECTORY = new File("classifier/industry");

  @SuppressWarnings("unused")
  private String linkedInGroup;

  @SuppressWarnings("unused")
  private int linkedInId;

  public IndustryFeature(int id, String description, 
      FeatureType type, String group) {
    super(id, description, type);
    this.linkedInGroup = group;
    this.linkedInId = id; // For now the LinkedIn id matches the Feature id
  }

  @Override
  protected File getVectorsContainerDirectory() {
    return VECTORS_DIRECTORY;
  }
}
