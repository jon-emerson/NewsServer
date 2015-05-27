package com.janknspank.bizness;

import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.janknspank.crawler.SiteManifests;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.notifications.nnet.NotificationNeuralNetworkTrainer;
import com.janknspank.proto.CoreProto.Distribution;
import com.janknspank.proto.CrawlerProto.SiteManifest;
import com.janknspank.proto.NotificationsProto.ClicksPerSite;
import com.janknspank.proto.NotificationsProto.ClicksPerSite.ClickType;
import com.janknspank.proto.NotificationsProto.Notification;
import com.janknspank.rank.DistributionBuilder;

/**
 * Tracks CTR for articles on a per-site basis.
 */
public class ClicksPerSites {
  /**
   * This is the CTR rating we give to sites with insufficient clicks to
   * calculate a reliable long-term CTR prediction.  This is slightly
   * below average (0.5 is average).
   */
  private static final double DEFAULT_SITE_CTR_RATING = 0.4;

  private static Map<String, ClicksPerSite> __map = null;

  private static synchronized Map<String, ClicksPerSite> getMap() {
    if (__map == null) {
      __map = Maps.newHashMap();
      try {
        for (ClicksPerSite clicksPerSite : Database.with(ClicksPerSite.class).get()) {
          __map.put(clicksPerSite.getRootDomain(), clicksPerSite);
        }
      } catch (DatabaseSchemaException e) {
        throw new Error(e);
      }
    }
    return __map;
  }

  public static double getCtrRating(SiteManifest site) {
    if (site == null) {
      return DEFAULT_SITE_CTR_RATING;
    }

    ClicksPerSite clicksPerSite = getMap().get(site.getRootDomain());

    // If we have no or little data for this site, return the default.
    if (clicksPerSite == null
        || clicksPerSite.getClickCount() < 5
        || clicksPerSite.getInstanceCount() < 50) {
      return DEFAULT_SITE_CTR_RATING;
    }

    return clicksPerSite.getCtrRating();
  }

  public static void main(String args[]) throws DatabaseSchemaException, DatabaseRequestException {
    Database.with(ClicksPerSite.class).createTable();

    // Gather a count of all the relevant notifications we've sent and how many
    // times they've been clicked.
    Multiset<String> notificationsPerRootDomain = HashMultiset.create();
    Multiset<String> clicksPerRootDomain = HashMultiset.create();
    for (Notification notification : NotificationNeuralNetworkTrainer.getApplicableNotifications()) {
      SiteManifest site = null;
      if (notification.hasUrl()) {
        site = SiteManifests.getForUrl(notification.getUrl());
      } else {
        if (notification.getText().contains(": ")) {
          site = SiteManifests.getForShortName(
              notification.getText().substring(0, notification.getText().indexOf(": ")));
        }
      }

      if (site != null) {
        notificationsPerRootDomain.add(site.getRootDomain());
        if (notification.hasClickTime()) {
          clicksPerRootDomain.add(site.getRootDomain());
        }
      }
    }

    // Build a distribution, so we know which sites really have the best
    // CTRs vs. our corpus.
    DistributionBuilder distributionBuilder = new DistributionBuilder();
    for (String rootDomain : notificationsPerRootDomain.elementSet()) {
      distributionBuilder.add(((double) clicksPerRootDomain.count(rootDomain))
          / notificationsPerRootDomain.count(rootDomain));
    }
    Distribution distribution = distributionBuilder.build();

    // Store everything we found into MySQL.
    List<ClicksPerSite> clicksPerSiteList = Lists.newArrayList();
    for (String rootDomain : notificationsPerRootDomain.elementSet()) {
      clicksPerSiteList.add(ClicksPerSite.newBuilder()
          .setRootDomain(rootDomain)
          .setClickType(ClickType.PUSH)
          .setClickCount(clicksPerRootDomain.count(rootDomain))
          .setInstanceCount(notificationsPerRootDomain.count(rootDomain))
          .setCtrRating(DistributionBuilder.projectQuantile(distribution,
              ((double) clicksPerRootDomain.count(rootDomain))
                  / notificationsPerRootDomain.count(rootDomain)))
          .build());
    }
    Database.insert(clicksPerSiteList);
  }
}
