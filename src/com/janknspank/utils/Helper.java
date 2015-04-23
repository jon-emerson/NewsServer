package com.janknspank.utils;

import com.janknspank.bizness.UserInterests;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserAction;
import com.janknspank.proto.UserProto.UserAction.ActionType;

public class Helper {
  public static void main(String args[]) throws DatabaseSchemaException {
//    for (Article article : Database.with(Article.class).get(
//        new QueryOption.DescendingSort("published_time"),
//        new QueryOption.Limit(5000))) {
//      ArticleFeature launchFeature =
//          ArticleFeatures.getFeature(article, FeatureId.MANUAL_HEURISTIC_LAUNCHES);
//      if (launchFeature.getSimilarity() > 0.1) {
//        System.out.println("\"" + article.getTitle() + "\" (" + launchFeature.getSimilarity() + ")");
//        System.out.println(article.getUrl());
//        System.out.println("First paragraph: \""
//            + Iterables.getFirst(article.getParagraphList(), "") + "\"");
//        System.out.println();
//      }
//    }
    User virenUser = Database.with(User.class).get("550084f2e4b02da05c56c73c");
    for (Interest interest : UserInterests.getInterests(virenUser)) {
      System.out.println("Interest: " + interest);
    }

    for (UserAction userAction : Database.with(UserAction.class).get(
        new QueryOption.WhereEquals("user_id", "550084f2e4b02da05c56c73c"))) {
      if (userAction.getActionType() == ActionType.X_OUT) {
        System.out.println("bad_url: \"" + userAction.getUrl() + "\"");
      }
    }
    for (UserAction userAction : Database.with(UserAction.class).get(
        new QueryOption.WhereEquals("user_id", "550084f2e4b02da05c56c73c"))) {
      if (userAction.getActionType() == ActionType.VOTE_UP) {
        System.out.println("good_url: \"" + userAction.getUrl() + "\"");
      }
    }
  }
}
