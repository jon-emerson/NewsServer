package com.janknspank.classifier;

import java.io.File;
import java.util.List;

import com.janknspank.proto.EnumsProto.IndustryCode;
import com.janknspank.proto.UserProto.UserIndustry;

public class StartupFeature extends VectorFeature {
  private static final File VECTORS_DIRECTORY = new File("classifier/startup");
  private IndustryCode[] relatedToIndustries;
  
  public StartupFeature(int id, String description, 
      FeatureType type, IndustryCode[] relatedToIndustries) {
    super(id, description, type);
    this.relatedToIndustries = relatedToIndustries;
  }
  
  public boolean isRelatedToIndustry(IndustryCode industryCode) {
    for (int i = 0; i < relatedToIndustries.length; i++) {
      if (relatedToIndustries[i].getId() == industryCode.getId()) {
        return true;
      }
    }
    return false;
  }
  
  public boolean isRelatedToIndustries(List<UserIndustry> userIndustries) {
    for (UserIndustry userIndustry : userIndustries) {
      if (isRelatedToIndustry(
          IndustryCodes.getFromIndustryCodeId(
              userIndustry.getIndustryCodeId()))) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected File getVectorsContainerDirectory() {
    // TODO Auto-generated method stub
    return VECTORS_DIRECTORY;
  }
}
