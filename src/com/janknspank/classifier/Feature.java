package com.janknspank.classifier;

import java.util.concurrent.ExecutionException;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.janknspank.proto.ArticleProto.ArticleOrBuilder;

/**
 * A business class that scores news articles against a particular attribute,
 * such as relevance to an industry or relevance to a user's intent.  Every
 * Feature must have a FeatureId and an implementation of the
 * {@code #score(Article)} method.  The implementation of the score method can
 * be decided on a case-by-case subclass basis.
 */
public abstract class Feature {
  private static final LoadingCache<FeatureId, Feature> FEATURE_CACHE =
      CacheBuilder.newBuilder().maximumSize(1000).build(new FeatureLoader());
  private static final Iterable<FeatureId> VALID_FEATURE_IDS;
  static {
    // Pre-warm the cache.
    ImmutableList.Builder<FeatureId> validFeatureIdsBuilder = ImmutableList.<FeatureId>builder();
    for (FeatureId featureId : FeatureId.values()) {
      try {
        FEATURE_CACHE.get(featureId);
      } catch (ExecutionException e) {
        continue;
      }
      validFeatureIdsBuilder.add(featureId);
    }
    VALID_FEATURE_IDS = validFeatureIdsBuilder.build();
    System.out.println(FEATURE_CACHE.size() + " features loaded");
  }

  /**
   * Loader for the Guava cache that returns a Feature for each FeatureId.  This
   * is where new Feature type instantiations should go.
   */
  private static class FeatureLoader extends CacheLoader<FeatureId, Feature> {
    @Override
    public Feature load(FeatureId featureId) throws ClassifierException {
      // NOTE(jonemerson): This section is going to get a lot more complicated
      // as we support more features!!  That's as designed, this is where that
      // logic is supposed to go!  But for now, we can assume the only things
      // around are vector features.
      return new VectorFeature(featureId);
    }
  }

  /**
   * Returns an implementation of the requested feature, constructing one from
   * vectors on disk (or wherever) as necessary.
   */
  public static Feature getFeature(FeatureId featureId) throws ClassifierException {
    try {
      return FEATURE_CACHE.get(featureId);
    } catch (ExecutionException e) {
      Throwables.propagateIfInstanceOf(e.getCause(), ClassifierException.class);
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns all the features that were successfully instantiated at class-loading time.
   */
  public static Iterable<Feature> getAllFeatures() {
    return FEATURE_CACHE.getAllPresent(VALID_FEATURE_IDS).values();
  }

  protected final FeatureId featureId;

  public Feature(FeatureId featureId) {
    this.featureId = featureId;
  }

  public FeatureId getId() {
    return featureId;
  }

  public final FeatureId getFeatureId() {
    return featureId;
  }

  public final String getDescription() {
    return featureId.getTitle();
  }

  /**
   * Computes a normalized relevance of this feature within the passed article.
   * @return [0-1]
   */
  public abstract double score(ArticleOrBuilder article) throws ClassifierException;
}
