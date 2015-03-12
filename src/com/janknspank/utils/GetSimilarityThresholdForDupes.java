package com.janknspank.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.rank.Deduper;

/**
 * This is just a playground for trying different de-duping techniques.
 */
public class GetSimilarityThresholdForDupes {
  private static final List<Set<String>> DUPES = ImmutableList.<Set<String>>builder()
      .add(ImmutableSet.of(
          "http://techcrunch.com/2015/02/10/yelp-gulps-eat24/",
          "http://bits.blogs.nytimes.com/2015/02/10/yelp-buys-eat24-an-online-food-ordering-service-for-134-million/",
          "http://www.theverge.com/2015/2/10/8013053/yelp-acquires-eat24-for-134-million",
          "http://www.wired.com/2015/02/yelp-buys-food-delivery-service-eat24/",
          "http://www.bloomberg.com/news/articles/2015-02-10/yelp-buys-eat24-to-add-food-ordering-service-raises-forecasts",
          "http://www.sfgate.com/business/article/Yelp-buys-food-ordering-service-Eat24-6073522.php",
          "http://www.forbes.com/sites/briansolomon/2015/02/10/yelp-gobbles-up-eat24-for-134-million-to-fight-grubhub/",
          "http://mashable.com/2015/02/10/yelp-acquires-eat24/",
          "http://www.engadget.com/2015/02/10/yelp-buys-eat24/"))
      .add(ImmutableSet.of(
          "http://bits.blogs.nytimes.com/2015/02/11/microsoft-continues-mobile-push-with-sunrise-acquisition/",
          "http://techcrunch.com/2015/02/04/microsoft-sunrise/",
          "http://www.theverge.com/2015/2/11/7984603/microsoft-sunrise-acquisition-official",
          "https://gigaom.com/2015/02/11/its-true-microsoft-officially-acquires-mobile-calendar-app-sunrise/",
          "http://mashable.com/2015/02/04/microsoft-reportedly-buys-sunrise/"))
      .add(ImmutableSet.of(
          "http://techcrunch.com/2015/02/18/ubers-series-e-round-surges-to-2-2-billion/",
          "http://dealbook.nytimes.com/2015/02/18/uber-expands-funding-round-by-1-billion/",
          "http://recode.net/2015/02/22/capital-gains-uber-raises-2-8-billion-snapchats-valuation-nears-20-billion/",
          "http://www.businessinsider.com/uber-has-expanded-its-most-recent-round-of-funding-by-1-billion-2015-2",
          "http://www.redherring.com/finance/uber-expands-series-e-funding-round-1-billion/"))
      .add(ImmutableSet.of(
          "http://www.wired.com/2015/02/facebook-unveils-tool-sharing-data-malicious-botnets/",
          "http://www.slate.com/blogs/future_tense/2015/02/16/facebook_threatexchange_the_tool_for_sharing_data_on_malicious_botnets.single.html",
          "http://thenextweb.com/facebook/2015/02/11/facebook-launches-threatexchange-partners-combat-security-threats/",
          "http://www.theguardian.com/technology/2015/feb/12/facebook-twitter-web-botnets-threatexchange"))
      .add(ImmutableSet.of(
          "http://thenextweb.com/apps/2015/02/20/youtube-kids-launching-february-23/",
          "http://money.cnn.com/2015/02/20/technology/mobile/youtube-for-kids/",
          "http://www.wired.com/2015/02/youtube-kids/",
          "http://mashable.com/2015/02/20/youtube-kids-android/"))
      .add(ImmutableSet.of(
          "http://thenextweb.com/apps/2015/02/24/opera-max-now-allows-mobile-operators-to-offer-subscribers-free-access-to-android-apps/",
          "http://techcrunch.com/2015/02/24/opera-adds-free-apps-to-its-android-data-savings-app-opera-max/",
          "http://www.engadget.com/2015/02/24/opera-max-app-pass/",
          "https://gigaom.com/2015/02/24/opera-launches-app-pass-zero-rating-tool-for-carriers/",
          "http://www.channelnewsasia.com/news/technology/opera-offers-new-feature/1678006.html"))
      .add(ImmutableSet.of(
          "http://www.bbc.com/news/technology-31602891",
          "http://www.businessinsider.com/google-banning-porn-on-blogger-2015-2",
          "http://www.pcmag.com/article2/0,2817,2477317,00.asp",
          "http://techcrunch.com/2015/02/23/google-bans-sexually-explicit-content-from-websites-hosted-on-blogger/",
          "http://www.engadget.com/2015/02/24/google-bans-sexually-explicit-nudity-blogger/"))
      .add(ImmutableSet.of(
          "http://techcrunch.com/2015/02/24/twilio-acquires-two-factor-authentication-service-authy/",
          "http://recode.net/2015/02/24/twilio-in-deal-to-acquire-security-startup-authy/",
          "http://thenextweb.com/apps/2015/02/24/two-factor-authentication-service-authy-has-been-acquired-by-twilio/",
          "http://www.forbes.com/sites/benkepes/2015/02/24/twilio-makes-its-first-acquisition-scoops-up-authy-to-help-deliver-secure-comms/"))
      .add(ImmutableSet.of(
          "http://www.theverge.com/2015/2/24/8098379/apple-buys-camel-audio-synth-audio-plugins",
          "https://gigaom.com/2015/02/24/looks-like-apple-bought-the-company-behind-software-synth-alchemy/",
          "http://www.businessinsider.com/apple-acquires-music-plug-in-maker-camel-audio-london-2015-2"))
      .add(ImmutableSet.of(
          "http://www.engadget.com/2015/02/24/pebble-time/",
          "http://www.businessinsider.com/pebble-time-2015-2",
          "http://thenextweb.com/insider/2015/02/24/pebble-announcement/",
          "http://www.theverge.com/2015/2/24/8091175/pebble-time-watch-wearable-platform-eric-migicovsky-interview",
          "http://recode.net/2015/02/24/pebble-launches-new-smartwatch-says-its-gearing-up-for-battle-with-apple/"))
      .add(ImmutableSet.of(
          "http://www.engadget.com/2015/02/24/google-chromebook-pixel-2015/",
          "https://gigaom.com/2015/02/24/chromebook-pixel-2-specifications-price-expectations/",
          "http://www.theverge.com/2015/2/23/8097283/chromebook-pixel-2-coming-soon-maybe",
          "http://arstechnica.com/gadgets/2015/02/google-confirms-new-chromebook-pixel-is-coming-soon/",
          "http://gizmodo.com/yep-google-is-going-to-make-another-chromebook-pixel-1687566066",
          "http://techcrunch.com/2015/02/23/google-is-making-a-new-chromebook-pixel-soon/",
          "http://www.businessinsider.com/google-chromebook-pixel-2-release-rumors-2015-2"))
      .add(ImmutableSet.of(
          "http://techcrunch.com/2015/02/24/google-publishes-chrome-experiment-1000/",
          "http://thenextweb.com/apps/2015/02/24/google-celebrates-1000-chrome-experiments-with-a-new-experiment/"))
      .add(ImmutableSet.of(
          "http://techcrunch.com/2015/02/23/uber-safetipin/",
          "http://thenextweb.com/apps/2015/02/24/uber-teams-up-with-safetipin-to-collect-location-safety-data-in-new-delhi/",
          "http://www.businessinsider.com/uber-drivers-will-mount-cameras-on-their-cars-for-crowdsourced-neighborhood-safety-project-2015-2",
          "http://www.engadget.com/2015/02/24/uber-safetipin-partnership/"))
      .add(ImmutableSet.of(
          "http://www.nytimes.com/2015/02/25/us/politics/as-expected-obama-vetoes-keystone-xl-pipeline-bill.html",
          "http://www.washingtonpost.com/blogs/the-fix/wp/2015/02/24/president-obama-will-veto-the-keystone-xl-bill-today-theres-more-of-that-to-come/",
          "http://www.cnn.com/2015/02/24/politics/keystone-veto-pen/",
          "http://www.technologyreview.com/view/535416/does-obamas-keystone-veto-matter/",
          "http://www.cnn.com/2015/02/24/politics/obama-keystone-veto/",
          "http://www.theguardian.com/environment/2015/feb/24/obama-vetoes-keystone-xl-pipeline-bill",
          "http://www.bloomberg.com/news/articles/2015-02-24/obama-to-veto-today-republicans-bid-to-force-keystone-approval"))
      .add(ImmutableSet.of(
          "http://www.nytimes.com/2015/02/25/us/politics/mcconnell-offers-plan-to-avoid-shutdown-of-homeland-security-dept.html",
          "http://www.slate.com/blogs/the_slatest/2015/02/24/dhs_immigration_stalemate_mitch_mcconnell_is_getting_desperate_to_end_the.html",
          "http://www.cnn.com/2015/02/23/politics/dhs-shutdown-funding-vote/",
          "http://www.bloomberg.com/politics/articles/2015-02-24/will-house-republicans-follow-mcconnell-s-lead-on-clean-dhs-funding-",
          "http://www.washingtonpost.com/politics/senate-gop-leader-is-seeking-a-way-out-of-homeland-security-funding-impasse/2015/02/24/81281f7a-bc29-11e4-8668-4e7ba8439ca6_story.html"))
      .add(ImmutableSet.of(
          "http://www.nytimes.com/2015/02/25/us/justice-dept-wont-charge-george-zimmerman-in-trayvon-martin-killing.html",
          "http://www.theguardian.com/us-news/2015/feb/24/george-zimmerman-trayvon-martin-justice-department",
          "http://www.bloomberg.com/politics/articles/2015-02-24/u-s-said-not-to-seek-civil-rights-charges-over-trayvon-martin",
          "http://mashable.com/2015/02/24/george-zimmerman-trayvon-martin-no-charges/",
          "http://www.latimes.com/nation/la-na-george-zimmerman-civil-rights-20150224-story.html"))
      .add(ImmutableSet.of(
          "http://www.nytimes.com/2015/02/25/nyregion/chris-christie-of-new-jersey-budget-address.html",
          "http://www.bloomberg.com/politics/articles/2015-02-24/christie-proposes-33-8-billion-budget-that-cuts-pension-payment"))
      .add(ImmutableSet.of(
          "http://www.latimes.com/sports/lakers/lakersnow/la-sp-ln-kobe-bryant-not-amused-celebration-celtics-20150224-story.html",
          "http://www.washingtonpost.com/blogs/early-lead/wp/2015/02/24/kobe-bryant-is-not-amused-by-the-lakers-victory-joy/",
          "http://www.boston.com/sports/basketball/celtics/extras/celtics_blog/2015/02/kobe_bryant_gives_death_stare_after_watching_lakers_postgame.html"))
      .add(ImmutableSet.of(
          "http://www.nytimes.com/2015/02/25/sports/baseball/through-years-of-change-pawtucket-ri-always-had-mccoy-stadium.html",
          "http://www.boston.com/sports/columnists/wilbur/2015/02/in_pawtucket_angst_and_anger_over_the_red_sox_plan.html",
          "http://www.washingtonpost.com/national/loss-of-pawsox-will-devastate-city-officials-say/2015/02/24/9ddba8f4-bc5f-11e4-9dfb-03366e719af8_story.html",
          "http://www.washingtonpost.com/national/investors-purchasing-pawtucket-red-sox-may-move-the-team/2015/02/23/9b22fb70-bb8e-11e4-9dfb-03366e719af8_story.html"))
      .build();

  public static final void main(String args[]) throws Exception {
    Iterable<String> allArticleUrls = Iterables.concat(DUPES);
    Map<String, Article> articles = ArticleCrawler.getArticles(allArticleUrls, true /* retain */);
    int positives = 0;
    int negatives = 0;
    int falseDupes = 0;
    int missedDupes = 0;
    for (Set<String> dupeSet : DUPES) {
      for (String currentUrl : dupeSet) {
        for (String compareToUrl : allArticleUrls) {
          if (currentUrl.equals(compareToUrl)) {
            continue;
          }

          if (dupeSet.contains(compareToUrl)) {
            // These articles should dupe.
            if (Deduper.isDupe(articles.get(currentUrl), articles.get(compareToUrl))) {
              positives++;
            } else {
              missedDupes++;
            }
          } else {
            // These articles should not dupe.
            if (Deduper.isDupe(articles.get(currentUrl), articles.get(compareToUrl))) {
              falseDupes++;
            } else {
              negatives++;
            }
          }
        }
      }
    }
    System.out.println("Positives = " + positives);
    System.out.println("Negatives = " + negatives);
    System.out.println("False dupes = " + falseDupes);
    System.out.println("Missed dupes = " + missedDupes);

    int score = positives * 10 - (5 * missedDupes + 2 * falseDupes);
    System.out.println("Score: " + score);
  }
}
