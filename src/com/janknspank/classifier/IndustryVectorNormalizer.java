package com.janknspank.classifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.proto.CoreProto.IndustryVectorNormalizationData;
import com.janknspank.rank.DistributionBuilder;

public class IndustryVectorNormalizer {
  private final IndustryVectorNormalizationData data;
  private final double quantileFor10PercentSeed;
  private final double quantileFor50PercentSeed;

  private IndustryVectorNormalizer(IndustryVectorNormalizationData data) {
    this.data = data;

    Distribution distribution = data.getDistribution();
    quantileFor10PercentSeed =
        DistributionBuilder.projectQuantile(distribution, data.getSimilarityThreshold10Percent());
    quantileFor50PercentSeed =
        DistributionBuilder.projectQuantile(distribution, data.getSimilarityThreshold50Percent());
  }

  public Distribution getDistribution() {
    return data.getDistribution();
  }

  double getSimilarityThreshold10Percent() {
    return data.getSimilarityThreshold10Percent();
  }

  double getSimilarityThreshold50Percent() {
    return data.getSimilarityThreshold50Percent();
  }

  /**
   * This normalizes an article's score against its industry, accounting for
   * both overall distribution and overall rarity of articles to be about a
   * given industry.  Articles that score as well as the top 90% of seed article
   * URLs are given scores 0.8 and above.  Articles that score as well as the
   * top 50% of seed articles are given scores 0.9 and above.  All other
   * articles are given scores between 0 and 0.78, depending on their relative
   * cosine similarities with the respective industry.
   */
  public double getNormalizedScore(double rawScore, int boost) {
    double quantile = DistributionBuilder.projectQuantile(getDistribution(), rawScore);

    // THIS SHOULD BE THE ONLY PLACE WHERE BOOSTS ARE CONSIDERED!!!
    quantile = Math.max(0, Math.min(1, quantile + (boost / 100.0)));

    if (quantile < quantileFor10PercentSeed) {
      // This is the range, between 0 and 1, that the given quantile lives
      // between [0, quantileFor10PercentSeed].
      double positionBetween0And10PercentSeed =
          (quantileFor10PercentSeed == 0)
              ? 0
              : quantile / quantileFor10PercentSeed;

      // Now, we normalize scores in this range to be in [0, 0.78].
      return 0.78 * positionBetween0And10PercentSeed;
    } else if (quantile < quantileFor50PercentSeed) {
      // This is the range, between 0 and 1, that the given quantile lives
      // between [quantileFor10PercentSeed, quantileFor50PercentSeed].
      double positionBetween10And50PercentSeed;
      if ((quantile - quantileFor10PercentSeed) >= (quantileFor50PercentSeed - quantileFor10PercentSeed)) {
        positionBetween10And50PercentSeed = 1;
      } else if (quantile <= quantileFor10PercentSeed) {
        positionBetween10And50PercentSeed = 0;
      } else {
        positionBetween10And50PercentSeed = (quantile - quantileFor10PercentSeed)
            / (quantileFor50PercentSeed - quantileFor10PercentSeed);
      }

      // Now, we normalize scores in this range to be in [0.78, 0.925].
      return 0.78 + 0.145 * positionBetween10And50PercentSeed;
    }

    // OK, we're in the "Top 50%" bracket.  This score is really good!
    // We normalize scores in this range to be in [0.925, 1].
    double positionBetween50PercentSeedAnd1;
    if (quantileFor50PercentSeed == 1) {
      positionBetween50PercentSeedAnd1 = 1;
    } else {
      positionBetween50PercentSeedAnd1 =
          (quantile - quantileFor50PercentSeed) / (1 - quantileFor50PercentSeed);
    }
    return 0.925 + 0.075 * positionBetween50PercentSeedAnd1;
  }

  public static IndustryVectorNormalizer fromFile(File normalizerFile)
      throws ClassifierException {
    InputStream inputStream = null;
    try {
      inputStream = new GZIPInputStream(new FileInputStream(normalizerFile));
      return new IndustryVectorNormalizer(IndustryVectorNormalizationData.parseFrom(inputStream));
    } catch (IOException e) {
      throw new ClassifierException("Could not read file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public static void writeToFile(IndustryVectorNormalizationData data, File normalizerFile)
      throws ClassifierException {
    OutputStream outputStream = null;
    try {
      outputStream = new GZIPOutputStream(new FileOutputStream(normalizerFile));
      data.writeTo(outputStream);
    } catch (IOException e) {
      throw new ClassifierException("Could not write file: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }
}
