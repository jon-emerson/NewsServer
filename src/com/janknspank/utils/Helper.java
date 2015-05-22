package com.janknspank.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.Lists;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.SocialEngagements;
import com.janknspank.bizness.UserInterests;
import com.janknspank.classifier.Feature;
import com.janknspank.classifier.FeatureId;
import com.janknspank.classifier.FeatureType;
import com.janknspank.classifier.VectorFeature;
import com.janknspank.classifier.VectorFeatureCreator;
import com.janknspank.common.Averager;
import com.janknspank.common.TopList;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.crawler.social.ShareNormalizer;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.ArticleProto.SocialEngagement.Site;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserAction;
import com.janknspank.proto.UserProto.UserAction.ActionType;
import com.janknspank.rank.InputValuesGenerator;
import com.janknspank.rank.Personas;

public class Helper {
  public static void main4(String args[]) throws Exception {
    Averager launchFacebookAverager = new Averager();
    Averager launchTwitterAverager = new Averager();
    Averager notLaunchFacebookAverager = new Averager();
    Averager notLaunchTwitterAverager = new Averager();

    Set<String> launchArticleUrls = Sets.newHashSet();
//    launchArticleUrls.addAll(
//        UrlFinder.findUrls("http://mashable.com/category/launch/"));
//    launchArticleUrls.addAll(
//        UrlFinder.findUrls("http://mashable.com/category/apps-and-software/"));
//    launchArticleUrls.addAll(
//        UrlFinder.findUrls("http://mashable.com/category/startups/"));
    Map<String, Article> launchArticles = Maps.newHashMap(
        ArticleCrawler.getArticles(launchArticleUrls, true /* retain */));
    for (Article article : Database.with(Article.class).get(
        //new QueryOption.WhereEqualsNumber("feature.feature_id", FeatureId.INTERNET.getId()),
        new QueryOption.WhereLike("url", "http://readwrite.*"))) {
      launchArticles.put(article.getUrl(), article);
    }

    for (Article article : launchArticles.values()) {
      boolean isLaunch =
          article.getUrl().contains("launch")
          || ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_LAUNCHES) > 0.1;
      if (isLaunch) {
        System.out.println(article.getUrl());
      }
      SocialEngagement twitterEngagement =
          SocialEngagements.getForArticle(article, Site.TWITTER);
      if (twitterEngagement != null) {
        if (isLaunch) {
          launchTwitterAverager.add(twitterEngagement.getShareCount());
        } else {
          notLaunchTwitterAverager.add(twitterEngagement.getShareCount());
        }
      }
      SocialEngagement facebookEngagement =
          SocialEngagements.getForArticle(article, Site.FACEBOOK);
      if (facebookEngagement != null) {
        if (isLaunch) {
          launchFacebookAverager.add(facebookEngagement.getShareCount());
        } else {
          notLaunchFacebookAverager.add(facebookEngagement.getShareCount());
        }
      }
    }
    System.out.println();
    System.out.println("launchFacebookAverager = " + launchFacebookAverager.get()
        + " from " + launchFacebookAverager.getCount() + " data points");
    System.out.println("notLaunchFacebookAverager = " + notLaunchFacebookAverager.get()
        + " from " + notLaunchFacebookAverager.getCount() + " data points");
    System.out.println("Facebook difference: "
        + launchFacebookAverager.get() / notLaunchFacebookAverager.get());
    System.out.println();
    System.out.println("launchTwitterAverager = " + launchTwitterAverager.get()
        + " from " + launchTwitterAverager.getCount() + " data points");
    System.out.println("notLaunchTwitterAverager = " + notLaunchTwitterAverager.get()
        + " from " + notLaunchTwitterAverager.getCount() + " data points");
    System.out.println("Twitter difference: "
        + launchTwitterAverager.get() / notLaunchTwitterAverager.get());
  }

  public static void main8(String args[]) throws Exception {
    int[] bucket = new int[100];
    for (int i = 0; i < bucket.length; i++) {
      bucket[i] = 0;
    }
    int count = 0;
    for (Article article : Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(10000))) {
      for (ArticleFeature articleFeature : article.getFeatureList()) {
        FeatureId featureId = FeatureId.fromId(articleFeature.getFeatureId());
        if (featureId != null && featureId.getFeatureType() == FeatureType.INDUSTRY) {
          count++;
          double similarity = articleFeature.getSimilarity();
          int bucketNum = (int) (similarity * 100);
          if (bucketNum == 100) {
            bucketNum = 99;
          }
          bucket[bucketNum]++;
        }
      }
    }
    for (int i = 0; i < bucket.length; i++) {
      System.out.println("0." + i + ": "
          + bucket[i] + " (" + ((double) bucket[i] / count) + "%)");
    }
  }

  public static void main5(String args[]) throws Exception {
    Iterable<Article> seedArticles =
        new VectorFeatureCreator(FeatureId.ARTS).getSeedArticles();
    VectorFeature ventureCapitalFeature =
        (VectorFeature) Feature.getFeature(FeatureId.ARTS);
    TopList<Article, Double> topArticles = new TopList<Article, Double>(2000);
    for (Article seedArticle : seedArticles) {
      topArticles.add(seedArticle, ventureCapitalFeature.rawScore(seedArticle));
    }
    System.out.println("Articles:");
    int i = 0;
    for (Article article : topArticles) {
      if (article.getUrl().equals("http://bits.blogs.nytimes.com/2014/10/28/youtube-weighing-new-subscription-service/")) {
      System.out.println(++i + ". " + topArticles.getValue(article) + ": " + article.getUrl()
          + " -> " + ventureCapitalFeature.score(article));
      }
    }

    System.out.println("10% quantile: " + ventureCapitalFeature.getSimilarityThreshold10Percent());
    System.out.println("50% quantile: " + ventureCapitalFeature.getSimilarityThreshold50Percent());
  }

  public static void main2(String args[]) throws Exception {
    for (String email : new String[] { "panaceaa@gmail.com" }) {
      Persona persona = Personas.getByEmail(email);
      Map<String, Article> goodArticles = ArticleCrawler.getArticles(persona.getGoodUrlList(), true);
      for (Article article : goodArticles.values()) {
        double internet = ArticleFeatures.getFeatureSimilarity(article, FeatureId.INTERNET);
        if (internet > 0.95) {
          System.out.println(article.getUrl());
        }
      }
    }
  }

  public static void main3(String args[]) throws DatabaseSchemaException {
    for (Article article : Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(10000))) {
      double featureSimilarity =
          ArticleFeatures.getFeatureSimilarity(article, FeatureId.MANUAL_HEURISTIC_IS_LIST);
      if (featureSimilarity > 0.1
          && ArticleFeatures.getFeatureSimilarity(article, FeatureId.ARCHITECTURE_AND_PLANNING) > 0.8) {
        System.out.println("\"" + article.getTitle() + "\" (" + featureSimilarity + ")");
        System.out.println(article.getUrl());
        System.out.println("First paragraph: \""
            + Iterables.getFirst(article.getParagraphList(), "") + "\"");
        System.out.println();
      }
    }
  }

  public static void main7(String args[]) throws Exception {
    for (Article article : Database.with(Article.class).get(
        new QueryOption.WhereLike("url", "http://uxmag.com/*"),
        new QueryOption.DescendingSort("published_time"))) {
      int ageInHours = (int) ((System.currentTimeMillis() - article.getPublishedTime())
          / TimeUnit.HOURS.toMillis(1));
      System.out.println(
          ArticleFeatures.getFeatureSimilarity(article, FeatureId.USER_EXPERIENCE)
          + " " + ageInHours + "h"
          + " " + article.getUrl());
    }
  }

  public static void main9(String args[]) throws Exception {
    for (String email : Personas.getPersonaMap().keySet()) {
      System.out.println(email + ":");
      Persona persona = Personas.getByEmail(email);
      User user = Personas.convertToUser(persona);
      Map<String, Article> goodArticles = ArticleCrawler.getArticles(persona.getGoodUrlList(), true);
      for (Article article : goodArticles.values()) {
        if (SocialEngagements.getForArticle(article, Site.FACEBOOK).getShareScore() < 0.05
            && SocialEngagements.getForArticle(article, Site.TWITTER).getShareScore() < 0.05) {
          System.out.println(" s " + article.getUrl());
        }
        if (InputValuesGenerator.relevanceToUserIndustries(user, article) == 0.5
            && InputValuesGenerator.relevanceToNonUserIndustries(user, article) > 0) {
          System.out.println(" i " + article.getUrl());
        }
      }
    }
  }

  public static void main(String args[]) throws Exception {
    int i = 0;
    for (Article article : Database.with(Article.class).get(
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(50000))) {
      List<SocialEngagement> updatedEngagements = Lists.newArrayList();
      for (SocialEngagement engagement : article.getSocialEngagementList()) {
        Site site = engagement.getSite();
        updatedEngagements.add(engagement.toBuilder()
            .setShareScore(ShareNormalizer.getInstance(site).getShareScore(
                article.getUrl(),
                engagement.getShareCount(),
                engagement.getCreateTime() - Articles.getPublishedTime(article) /* ageInMillis */))
            .build());
      }
      Database.update(article
          .toBuilder()
          .clearSocialEngagement()
          .addAllSocialEngagement(updatedEngagements)
          .build());
      if (++i % 1000 == 0) {
        System.out.println(i);
      }
    }
  }

  public static void main10(String args[]) throws Exception {
    Multiset<String> facebookInterests = HashMultiset.create();
    Multiset<String> userInterests = HashMultiset.create();
    for (User user : Database.with(User.class).get()) {
      ImmutableSet<FeatureId> enabledFeatureIds =
          ImmutableSet.copyOf(UserInterests.getUserIndustryFeatureIds(user));
      for (Interest interest : user.getInterestList()) {
        if (interest.getType() == InterestType.INDUSTRY) {
          FeatureId featureId = FeatureId.fromId(interest.getIndustryCode());
          if (featureId != null && enabledFeatureIds.contains(featureId)) {
            if (interest.getSource() == InterestSource.USER) {
              userInterests.add(featureId.getTitle());
            } else if (interest.getSource() == InterestSource.FACEBOOK_PROFILE) {
              facebookInterests.add(featureId.getTitle());
            }
          }
        }
      }
    }
    TopList<String, Integer> topUserInterests = new TopList<>(100);
    for (String userInterest : userInterests) {
      topUserInterests.add(userInterest, userInterests.count(userInterest));
    }
    for (String userInterest : topUserInterests) {
      System.out.println(userInterest + ": " + topUserInterests.getValue(userInterest) + " vs "
          + facebookInterests.count(userInterest) + " from Facebook login");
    }
  }

  public static void main11(String args[]) throws Exception {
    TopList<User, Long> topUsers = new TopList<>(50);
    for (User user : Database.with(User.class).get(new QueryOption.AscendingSort("create_time"))) {
      if (user.getLast5AppUseTimeCount() >= 5) {
        long readCount = Database.with(UserAction.class).getSize(
            new QueryOption.WhereEquals("user_id", user.getId()),
            new QueryOption.WhereEqualsEnum("action_type", ActionType.READ_ARTICLE));
        topUsers.add(user, readCount);
      }
    }
    for (User user : topUsers) {
      System.out.println(user.getFirstName() + " " + user.getLastName()
          + " (" + user.getEmail() + ") - " + topUsers.getValue(user) + " read articles");
      for (FeatureId industryFeatureId : UserInterests.getUserIndustryFeatureIds(user)) {
        System.out.println("  following " + industryFeatureId.getTitle());
      }
      int i = 0;
      for (Interest interest : UserInterests.getInterests(user)) {
        if (interest.getType() == InterestType.ENTITY) {
          i++;
        }
      }
      System.out.println("  and " + i + " companies");
      System.out.println();
    }
  }
}
