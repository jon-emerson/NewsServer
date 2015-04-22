package com.janknspank.rank;

import org.neuroph.core.data.DataSetRow;

import com.google.common.primitives.Doubles;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

/**
 * Represents an Article used for neural network training.  Contained within is
 * the user who the Article relates to, and the score that should be given for
 * said user.
 */
public class TrainingArticle {
  private final Article article;
  private final User user;
  private final double score;
  private DataSetRow dataSetRow = null;

  public TrainingArticle(Article article, User user, double score) {
    this.article = article;
    this.user = user;
    this.score = score;
  }

  public Article getArticle() {
    return article;
  }

  public User getUser() {
    return user;
  }

  public double getScore() {
    return score;
  }

  public synchronized DataSetRow getDataSetRow() {
    if (dataSetRow == null) {
      dataSetRow = new DataSetRow(
          Doubles.toArray(NeuralNetworkScorer.generateInputNodes(user, article).values()),
          new double[] { score });
    }
    return dataSetRow;
  }
}
