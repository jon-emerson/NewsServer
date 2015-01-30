package com.janknspank.classifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.ValidationException;

import com.google.common.collect.Iterables;
import com.janknspank.bizness.ArticleIndustryClassifications;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.IndustryCodes;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleIndustry;
import com.janknspank.proto.EnumsProto.IndustryCode;

public class IndustryClassifier {
  private static IndustryClassifier instance = null;
  private static Map<IndustryCode, IndustryVector> industryVectors;
  private static final double RELEVANCE_THRESHOLD = 0.01;

  private IndustryClassifier() {
    industryVectors = new HashMap<>();
    for (IndustryCode industryCode : IndustryCodes.INDUSTRY_CODE_MAP.values()) {
      try {
        industryVectors.put(industryCode, new IndustryVector(industryCode));
      } catch (BiznessException | DatabaseSchemaException e) {
        // Can't generate industry vector. Ignore it.
      }
    }
  }

  public static synchronized IndustryClassifier getInstance() {
    if (instance == null) {
      instance = new IndustryClassifier();
    }
    return instance;
  }

  /**
   * Returns a list of Article Industry Classifications
   * @param article
   * @return
   * @throws DatabaseSchemaException 
   * @throws BiznessException 
   * @throws DatabaseRequestException 
   * @throws DataInternalException
   * @throws IOException
   * @throws ValidationException
   */
  public Iterable<ArticleIndustry> classify(Article article)
      throws DatabaseSchemaException, BiznessException, DatabaseRequestException {
    DocumentVector articleVector = new DocumentVector(article);

    // See if the classification has already been computed
    Iterable<ArticleIndustry> classifications =
        ArticleIndustryClassifications.getFor(article);
    if (classifications != null && Iterables.size(classifications) > 0) {
      return classifications;
    }

    // Compute classifications from IndustryVectors
    List<ArticleIndustry> newClassifications = new ArrayList<>();
    for (Map.Entry<IndustryCode, IndustryVector> industry : industryVectors.entrySet()) {
      IndustryCode code = industry.getKey();
      IndustryVector vector = industry.getValue();
      double similarity = articleVector.cosineSimilarityTo(vector);
      // Only save industries that are closely related
      if (similarity >= RELEVANCE_THRESHOLD) {
        ArticleIndustry classification =
            ArticleIndustry.newBuilder()
                .setIndustryCodeId(code.getId())
                .setSimilarity(similarity)
                .build();
        newClassifications.add(classification);
      }
    }
    Database.set(article, "industry", newClassifications);
    return classifications;
  }
}
