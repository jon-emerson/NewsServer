package com.janknspank.classifier;

import com.janknspank.proto.EnumsProto.IndustryCode;

public enum ArticleFeatureEnum {
  START_TECH(1000, new StartupFeature(1000, 
      "Related to tech startups", 
      FeatureType.SERVES_INTENT, 
      new IndustryCode[] {
        IndustryCodes.INDUSTRY_CODE_MAP.get(4),
        IndustryCodes.INDUSTRY_CODE_MAP.get(6),
        IndustryCodes.INDUSTRY_CODE_MAP.get(109),
        IndustryCodes.INDUSTRY_CODE_MAP.get(118),
        IndustryCodes.INDUSTRY_CODE_MAP.get(12),
        IndustryCodes.INDUSTRY_CODE_MAP.get(24),
        IndustryCodes.INDUSTRY_CODE_MAP.get(3),
        IndustryCodes.INDUSTRY_CODE_MAP.get(5),
        IndustryCodes.INDUSTRY_CODE_MAP.get(127),
  })),
  START_TRADITIONAL(1001, new StartupFeature(1001, 
      "Related to traditional brick and mortar startups", 
      FeatureType.SERVES_INTENT,
      new IndustryCode[] {
        IndustryCodes.INDUSTRY_CODE_MAP.get(25),
        IndustryCodes.INDUSTRY_CODE_MAP.get(53),
  }));
  //PREDICTS_FUTURE(2, new CharacterFeature(2, "Future oriented article - predicitve.", ArticleFeature.Type.TEXT_CHARACTER);
  //TODO: copy all ArticleTypeCodes in

  private final int featureId;
  private final Feature feature;

  private ArticleFeatureEnum(int featureId, Feature feature) {
    this.featureId = featureId;
    this.feature = feature;
  }

  public int getFeatureId() {
    return featureId;
  }

  public Feature getFeature() {
    return feature;
  }
  
  public static ArticleFeatureEnum findById(int id) {
    for (ArticleFeatureEnum feature : ArticleFeatureEnum.values()) {
      if (feature.getFeatureId() == id) {
        return feature;
      }
    }
    return null;
  }
}