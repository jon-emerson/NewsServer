package com.janknspank.utils;

/**
 * Utility for moving all the user actions in our system from MongoDB to
 * MySQL.
 */
public class UserActionsToMySQL {
//  private static MySQLUserAction.ActionType toMySQL(UserAction.ActionType actionType) {
//    switch (actionType) {
//      case FAVORITE:
//        return MySQLUserAction.ActionType.FAVORITE;
//      case X_OUT:
//        return MySQLUserAction.ActionType.X_OUT;
//      case TAP_FROM_STREAM:
//        return MySQLUserAction.ActionType.TAP_FROM_STREAM;
//      case SHARE:
//        return MySQLUserAction.ActionType.SHARE;
//      case READ_ARTICLE:
//        return MySQLUserAction.ActionType.READ_ARTICLE;
//      case VOTE_UP:
//        return MySQLUserAction.ActionType.VOTE_UP;
//      case UNVOTE_UP:
//        return MySQLUserAction.ActionType.UNVOTE_UP;
//      case SCROLL_PAST:
//        return MySQLUserAction.ActionType.SCROLL_PAST;
//    }
//    return MySQLUserAction.ActionType.UNKNOWN;
//  }
//
//  private static MySQLUserAction toMySQL(UserAction userAction) {
//    MySQLUserAction.Builder builder = MySQLUserAction.newBuilder()
//        .setId(userAction.getId())
//        .setUserId(userAction.getUserId())
//        .setActionType(toMySQL(userAction.getActionType()))
//        .setUrlId(userAction.getUrlId())
//        .setUrl(userAction.getUrl())
//        .setCreateTime(userAction.getCreateTime())
//        .setReadStartTime(userAction.getReadStartTime())
//        .setReadEndTime(userAction.getReadEndTime());
//
//    if (userAction.getActionType() == ActionType.VOTE_UP
//        || userAction.getActionType() == ActionType.X_OUT) {
//      for (Interest interest : userAction.getInterestList()) {
//        try {
//          builder.addInterest((Interest) Validator.assertValid(interest));
//        } catch (Exception e) {}
//      }
//    }
//    if (userAction.hasOnStreamForInterest()) {
//      builder.setOnStreamForInterest(userAction.getOnStreamForInterest());
//    }
//    return builder.build();
//  }
//
//  public static void main(String args[]) throws Exception {
//    Database.with(MySQLUserAction.class).createTable();
//    List<MySQLUserAction> mySqlUserActions = Lists.newArrayList();
//    int i = 0;
//    for (UserAction userAction : Database.with(UserAction.class).get()) {
//      i++;
//      if (userAction.getActionType() != ActionType.UNKNOWN
//          && userAction.getActionType() != ActionType.SCROLL_PAST) {
//        mySqlUserActions.add(toMySQL(userAction));
//      }
//      if (mySqlUserActions.size() > 100) {
//        Database.insert(mySqlUserActions);
//        mySqlUserActions.clear();
//        System.out.println(i + " actions ported");
//      }
//    }
//    Database.insert(mySqlUserActions);
//    System.out.println(i + " actions ported");
//  }
}
