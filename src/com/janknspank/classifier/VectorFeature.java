package com.janknspank.classifier;

import java.io.File;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.rank.DistributionBuilder;

/**
 * Abstract Feature that judges articles' relevance to topics by using TF-IDF
 * word frequency vectors.  The feature's vectors and historical distributions
 * must have already been created by VectorFeatureCreator, and live in the
 * specified {@code vectorDirectory} as feature.vector and feature.distribution,
 * respectively.
 * @see {@link #score(ArticleOrBuilder)}
 */
public final class VectorFeature extends Feature {
  private static final Map<FeatureType, File> VECTOR_DIRECTORY_MAP =
      ImmutableMap.<FeatureType, File>builder()
          .put(FeatureType.INDUSTRY, new File("classifier/industry"))
          .put(FeatureType.SERVES_INTENT, new File("classifier/serves-intent"))
          .build();

  private final Vector vector;
  private final Distribution distribution;

  public VectorFeature(FeatureId featureId) throws ClassifierException {
    super(featureId);

    vector = Vector.fromFile(getVectorFile(featureId));
    distribution = DistributionBuilder.fromFile(getDistributionFile(featureId));
  }

  /**
   * Returns a distribution-adjusted score for this article's relevance to
   * this vector's feature.
   */
  @Override
  public double score(ArticleOrBuilder article) throws ClassifierException {
    return DistributionBuilder.projectQuantile(
        distribution,
        vector.getCosineSimilarity(
            UniverseVector.getInstance(), Vector.fromArticle(article)));
  }

  static File getVectorFile(FeatureId featureId) throws ClassifierException {
    return new File(getVectorDirectory(featureId), "/feature.vector");
  }

  static File getDistributionFile(FeatureId featureId) throws ClassifierException {
    return new File(getVectorDirectory(featureId), "/feature.distribution");
  }

  static File getVectorDirectory(FeatureId featureId) throws ClassifierException {
    int id = featureId.getId();
    File vectorDirectory = VECTOR_DIRECTORY_MAP.get(featureId.getFeatureType());
    for (String idFolderName : vectorDirectory.list()) {
      if (idFolderName.startsWith(id + "-")) {
        return new File(vectorDirectory, idFolderName);
      }
    }
    throw new ClassifierException("Could not find vector directory, vector ID=" + id);
  }
}
