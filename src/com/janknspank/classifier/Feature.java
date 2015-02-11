package com.janknspank.classifier;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;

import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.CoreProto.Distribution;

public abstract class Feature {
  protected final int id;
  protected final String description;
  protected final FeatureType type;
  
  public Feature(int id, String description, FeatureType type) {
    this.id = id;
    this.description = description;
    this.type = type;
  }
  
  /**
   * Generates the feature from root sources like seed lists
   * and the corpus of documents
   */
  protected abstract void generate() 
      throws ClassifierException, DatabaseSchemaException, BiznessException;
  
  /**
   * Computes the prevalence of the feature within the article
   * @param article
   * @return [0-1]
   * @throws ClassifierException
   */
  public abstract double getScore(ArticleOrBuilder article) 
      throws ClassifierException;
  
  /**
   * Returns a distribution for the feature used to normalize
   * scores
   * @return
   * @throws ClassifierException
   */
  public abstract Distribution getDistribution()
      throws ClassifierException;
  
  public int getId() {
    return id;
  }
  
  public String getDescription() {
    return description;
  }
  
  public FeatureType getType() {
    return type;
  }
  
  /**
   * Regenerates a feature with a given ID, or "all".  Feature IDs
   * are specified as integers, passed as command-line parameters to this
   * program.
   */
  public static void main(String args[]) throws Exception {
    // 1. Figure out which features to regenerate from args
    boolean regenerateAllFeatures = false;
    Set<Integer> featuresToRegenerate = new HashSet<>();
    if (args.length > 0) {
      if (args[0].equals("all")) {
        regenerateAllFeatures = true;
      } else {
        for (String featureId : args) {
          Integer id = NumberUtils.toInt(featureId, -1);
          if (id != -1) {
            featuresToRegenerate.add(id);
          }
        }
      }
    }
    
    // 2. Run through all features and regenerate where necessary
    for (ArticleFeatureEnum feature: ArticleFeatureEnum.values()) {
      if (regenerateAllFeatures || 
          featuresToRegenerate.contains(feature.getFeatureId())) {
        feature.getFeature().generate();
      }
    }
  }
}
