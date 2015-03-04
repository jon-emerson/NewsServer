package com.janknspank.utils;

import com.google.common.collect.Iterables;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.IosPushNotificationHelper;
import com.janknspank.database.Database;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.CoreProto.DeviceRegistration;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.NeuralNetworkScorer;

/**
 * Sends every user who's enabled iOS push notifications a notification about
 * the top article in their stream.  We'll run this once daily in the morning
 * to help with re-engagement.
 */
public class PushDailyNotifications {
  public static void main(String args[]) throws Exception {
    for (User user : Database.with(User.class).get()) {
      try {
        Iterable<DeviceRegistration> registrations =
            IosPushNotificationHelper.getDeviceRegistrations(user);
        if (!Iterables.isEmpty(registrations)) {
          Article bestArticle = Iterables.getFirst(
              Articles.getRankedArticles(user, NeuralNetworkScorer.getInstance(), 100), null);
          for (DeviceRegistration registration : registrations) {
            IosPushNotificationHelper.getInstance().sendArticle(bestArticle, registration);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
