package com.janknspank.rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.Users;
import com.janknspank.common.Asserts;
import com.janknspank.crawler.ArticleCrawler;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;

public class JonBenchmark {
  public static final List<String> GOOD_URLS = ImmutableList.of(
      // Retiring announcement: Because I used to report through him at Google,
      // and he's a leader in the field.
      "http://techcrunch.com/2015/02/03/alan-eustace-google/",
      // Because it's in the space I'm working in.
      "http://techcrunch.com/2013/04/23/google-buys-wavii-for-north-of-30-million/",
      "http://techcrunch.com/2014/01/07/yahoo-launches-news-digest-its-first-app-based-on-summly/",
      "http://allthingsd.com/20130325/yahoo-paid-30-million-in-cash-for-18-months-of-young-summly-entrepreneurs-time/",
      // Because I used to live in Seattle and I'm interested in start-up incubators.
      "http://techcrunch.com/2015/01/27/techstars-ventures-raises-150-million-as-it-expands-into-series-a-investments/",
      // Because I'm working on a text analysis start-up.
      "http://techcrunch.com/2015/01/20/microsoft-acquires-text-analysis-service-equivio/",
      // Because I'm friends with Chrys.
      "http://techcrunch.com/2015/01/29/secret-co-founder-chrys-bader-wechseler-steps-down-because-its-not-about-design-anymore/",
      // Because I used to work for Google and still own Google stock.
      "http://techcrunch.com/2015/01/29/google-q4-2014-earnings/",
      // Because I'm an entrepreneur and I thought about building an app in this space.
      "http://techcrunch.com/2015/01/26/favados-new-app-helps-you-find-the-best-grocery-deals/",
      // Arguable, but certainly a well researched article about things I'm interested in.
      "http://techcrunch.com/2015/01/29/google-glass-patrick-pichette/",
      // Because new platform launches are often good ins for launching complementary products.
      "http://mashable.com/2015/01/27/apple-watch-april/",
      // Because I want to be more innovative.
      "http://mashable.com/2015/01/30/innovative-at-work/",
      // Arguable... But I like'd reading it.
      "http://www.nytimes.com/aponline/2015/01/30/business/ap-us-google-money-management.html",
      // Arguable, but also interesting to read.
      "http://www.nytimes.com/aponline/2014/04/11/us/ap-us-salesforce-san-francisco-lease.html",
      "http://www.fastcompany.com/3041435/revolutionizing-work/startups-revolutionizing-work",
      "http://arstechnica.com/gadgets/2015/01/microsoft-to-invest-in-cyanogen-hopes-to-take-android-away-from-google/",
      "http://startupworkout.com/inbox-hero-how-to-write-hypnotizing-emails-that-convert-like-crazy/1/",
      "http://startupworkout.com/motivation-121-epic-business-quotes-to-inspire-your-success/",
      "http://www.slate.com/articles/health_and_science/science/2014/06/facebook_unethical_experiment_it_made_news_feeds_happier_or_sadder_to_manipulate.html",
      "http://recode.net/2014/04/24/exclusive-google-head-vic-gundotra-leaving-company/",
      "http://bits.blogs.nytimes.com/2014/04/24/vic-gundotra-google-plus-lead-departing/",
      "http://recode.net/2014/10/24/google-ceo-larry-page-reorgs-staff-anoints-sundar-pichai-as-new-product-czar/",
      "http://techcrunch.com/2014/09/23/gobble-launch/",
      // Most important currency event in X years.
      "http://www.nytimes.com/2015/01/16/business/swiss-national-bank-euro-franc-exchange-rate.html",
      "http://money.cnn.com/2014/02/19/technology/social/facebook-whatsapp/",
      // Most important geopolitical event of X years.
      "http://www.washingtonpost.com/world/russias-putin-prepares-to-annex-crimea/2014/03/18/933183b2-654e-45ce-920e-4d18c0ffec73_story.html",
      "http://arstechnica.com/security/2014/04/vicious-heartbleed-bug-bites-millions-of-android-phones-other-devices/",
      "http://techcrunch.com/2014/05/28/apple-buys-beats-electronics-for-3b/",
      "http://arstechnica.com/apple/2014/09/apple-announces-iphone-6-iphone-6-plus/",
      // Neat article, similar to what I'm doing with AI.
      "http://arstechnica.com/science/2015/01/have-a-scientific-problem-steal-an-answer-from-nature/",
      "http://www.cnn.com/2014/11/28/tech/innovation/smart-sports-equipment/index.html",
      "http://www.theverge.com/2014/10/15/6982167/google-android-5-0-l-lollipop-announcement-release",
      "http://techcrunch.com/2014/09/09/announces-mobile-payments-solution-called-apple-pay/",
      "http://techcrunch.com/2014/03/25/best-y-combinator-demo-day-startups/",
      "https://medium.com/@chrismessina/thoughts-on-google-8883844a9ca4",
      "http://www.nytimes.com/2014/10/27/business/media/how-facebook-is-changing-the-way-its-users-consume-journalism.html",
      "http://venturebeat.com/2015/01/30/google-ventures-releases-new-diy-guide-to-help-startups-design-better-products/",
      "http://venturebeat.com/2015/01/29/googles-eric-schmidt-has-a-10-year-prediction-of-how-tech-will-disrupt-whole-industries/",
      "http://www.pcmag.com/article2/0,2817,2430291,00.asp",
      "http://www.theverge.com/2014/2/7/5389668/bitcoin-exchange-mt-gox-halts-withdrawals-maintenance",
      "http://www.redherring.com/finance/uber-raises-another-1-2bn-staggering-40bn-valuation/",
      "http://www.redherring.com/finance/alibabas-road-largest-ipo-ever/",
      "http://www.wired.com/2015/01/facebook-making-news-feed-better-asking-real-people-direct-questions/",
      "http://www.wired.com/2015/01/smartest-richest-companies-cant-crack-mobile-future-belongs-anyone-can/",
      // Because I have been thinking about the Apple Watch as a launch platform.
      "http://money.cnn.com/2015/02/04/technology/apple-watch-flop/index.html",
      // Looking for avenues of funding / recognition / promotion.
      "http://techcrunch.com/2015/02/04/disrupt-ny-battlefield-applications-are-open-so-apply-now/",
      // Another startup's journey from idea to angel funding to relaunch.
      "http://techcrunch.com/2015/02/04/bunkr-is-now-the-definitive-modern-presentation-tool-for-the-web/",
      "http://techcrunch.com/2015/02/04/microsoft-sunrise/",
      // Because I feel statistics are critical for employees to make good decisions, and it seems
      // Twitter is acting against this, just like Google+ did.  Interesting to get their perspective.
      "http://recode.net/2015/02/05/twitter-cuts-off-employee-access-to-its-metrics/",
      // Because I'm working on software for business people, and LinkedIn is the behemoth in the space.
      "http://recode.net/2015/02/05/linkedin-beats-earnings-estimates-as-more-employers-seek-talent/",
      // Fun article about running a small VC firm as a start-up: Finding MVP, iterating.
      "http://recode.net/2015/02/06/getting-past-the-catch-22-of-venture-capital/",
      // Most important tech business news of the day: Two rivals found common ground.
      "http://techcrunch.com/2015/02/05/twitter-confirms-new-google-firehose-deal-to-distribute-traffic-to-logged-out-users/",
      // I use to manage the team Odysee's joining at Google.
      "http://techcrunch.com/2015/02/08/google-odysee/",
      // Big moves in AI.
      "http://techcrunch.com/2014/01/26/google-deepmind/",
      "http://www.technologyreview.com/news/524026/is-google-cornering-the-market-on-deep-learning/",
      // Complete crap rambling article about correlations with little insight inside rote tech-hate.
      "http://techcrunch.com/2015/02/08/startups-a-rich-mans-game/",
      "http://techcrunch.com/2015/02/10/yelp-gulps-eat24/",
      "http://bits.blogs.nytimes.com/2015/02/10/yelp-buys-eat24-an-online-food-ordering-service-for-134-million/",
      "http://techcrunch.com/2015/02/10/flipboard-web/",
      "http://techcrunch.com/2015/02/12/expedia-buys-orbitz-for-1-6b-in-cash-to-square-up-to-priceline/",
      "http://techcrunch.com/2015/02/12/linkedin-battens-down-the-hatches-on-api-use-limiting-full-access-to-partners/",
      "http://techcrunch.com/2015/02/13/google-pulls-its-helpouts-mobile-applications-from-the-app-stores/");

  public static final List<String> BAD_URLS = ImmutableList.of(
      // Fluff, doesn't actually address any actually difficult challenges.
      "http://techcrunch.com/2015/01/26/becoming-an-engineering-manager/",
      // Not relevant to me or my industry.
      "http://techcrunch.com/2015/01/30/canon-5ds-leaked-specs-reveal-a-high-megapixel-dslr-geared-for-color-accuracy/",
      // Not relevant to me.
      "http://techcrunch.com/2015/01/29/teslas-p85d-will-get-even-faster-thanks-to-a-software-update/",
      // Don't need YAUA (Yet another Uber article)
      "http://techcrunch.com/2015/01/30/will-uber-convertibles-run-out-of-gas/",
      "http://techcrunch.com/2015/01/30/uber-privacy-review/",
      // Total crap video promo about a panel I don't care about.
      "http://techcrunch.com/2015/01/30/gillmor-gang-live-01-30-15/",
      // Don't care about Inbox. (Not sure why, but I really don't.)  And usually I hate free stuff.
      "http://techcrunch.com/2015/01/29/google-offering-24-hours-of-inbox-invites-on-the-day-of-outlooks-ios-launch/",
      "http://mashable.com/2015/01/30/lego-marvel-helicarrier/",
      "http://mashable.com/2015/01/30/18-new-photos-game-of-thrones-season-5/",
      "http://mashable.com/2015/01/27/dad-rap/",
      "http://mashable.com/2015/01/27/robert-kraft-patriots-deflategate/",
      "http://mashable.com/2015/01/30/starbucks-seahawks-frappuccino/",
      "http://mashable.com/2015/01/30/ball-pond-adults-london/",
      "http://www.nytimes.com/2015/01/31/sports/football/no-1-debate-in-tampa-whether-to-draft-jameis-winston.html",
      "http://www.nytimes.com/2015/01/31/your-money/paying-for-college/the-wouldve-shouldve-and-couldve-of-taxing-529-plans.html",
      // Not actually about Google, but about travel.
      "http://www.nytimes.com/2015/02/01/travel/you-googled-it-we-answered-it.html",
      // Mentions Google, but is actually about a TV show.
      "http://www.nytimes.com/2015/02/01/magazine/black-mirror-and-the-horrors-and-delights-of-technology.html",
      // About parenting.
      "http://www.nytimes.com/2015/02/01/your-money/why-you-should-tell-your-kids-how-much-you-make.html",
      "http://www.slate.com/articles/technology/technology/2015/01/snapchat_why_teens_favorite_app_makes_the_facebook_generation_feel_old.html",
      "http://www.slate.com/blogs/the_slatest/2015/01/30/lindsey_graham_2016_south_carolina_senator_says_center_right_positions_make.html",
      "http://www.slate.com/articles/life/dear_prudence/2015/01/dear_prudence_i_want_to_stop_working_at_26.html",
      "http://arstechnica.com/gaming/2015/01/a-virtual-day-with-750-pinball-and-arcade-games-no-quarters-needed/",
      "http://arstechnica.com/business/2014/10/after-gamergate-tweet-adobe-distances-itself-from-gawker-bullying/",
      "http://arstechnica.com/gadgets/2012/04/ars-reviews-adobe-lightroom-4/",
      "http://arstechnica.com/information-technology/2012/09/adobes-continuing-revolution-pushes-the-cutting-edge-of-html5-development/",
      "http://arstechnica.com/business/2015/01/more-comcast-customers-write-in-report-name-changes-of-whore-dummy/",
      "http://arstechnica.com/tech-policy/2015/01/silk-road-trial-how-the-dread-pirate-roberts-embraced-violence/",
      "http://www.cnn.com/2014/12/16/tech/web/google-year-in-search-2014/index.html",
      "http://www.cnn.com/2015/01/30/entertainment/suge-knight-hit-and-run/index.html",
      "http://www.cnn.com/2015/01/30/asia/china-dragon-dinosaur/index.html",
      "http://www.nytimes.com/2015/02/01/nyregion/for-manoush-zomorodi-time-for-some-conscious-unplugging.html",
      "http://bits.blogs.nytimes.com/2015/01/30/verizon-wireless-to-allow-complete-opt-out-of-mobile-supercookies/",
      "http://venturebeat.com/2015/01/30/new-apple-and-ibm-enterprise-app-delivers-a-lot-of-utility/",
      "http://venturebeat.com/2015/01/30/tencent-grabs-the-digital-rights-to-nba-basketball-in-china/",
      "http://venturebeat.com/2015/01/30/here-are-the-predictions-for-who-will-win-the-super-bowl-of-ads/",
      "http://www.latimes.com/local/lanow/la-me-ln-measles-outbreak-hits-marin-county-20150130-story.html",
      "http://www.latimes.com/local/lanow/la-me-ln-lightning-rain-storms-southern-california-20150130-story.html",
      "http://www.theverge.com/2015/1/30/7948619/before-uber-revolutionizes-labor-its-going-to-have-to-explain-these",
      "http://www.theverge.com/2015/1/30/7952941/at-t-largest-bidder-fcc-aws-3-wireless-spectrum-auction",
      "http://www.theverge.com/2015/1/23/7876777/sonos-sound-wave-logo",
      "http://www.theverge.com/2015/1/15/7551569/harmony-api-use-case",
      "http://www.theverge.com/2015/1/21/7861645/finfisher-spyware-let-bahrain-government-hack-political-activist",
      "http://www.wired.com/2015/01/game-of-thrones-season-5-trailer/",
      "http://www.wired.com/2015/01/verizon-will-let-customers-sidestep-privacy-killing-perma-cookie/",
      "http://www.wired.com/2015/01/tech-time-warp-pizza/",
      "http://www.wired.com/2015/01/think-students-allowed-cheat/",
      "http://www.wired.com/2014/12/indispensable-vehicles-got-start-wwi/",
      "http://www.wired.com/2015/01/absurd-creature-of-the-week-barbados-threadsnake/",
      "http://www.wired.com/2015/01/nissan-juke-tracks-snow/",
      "http://www.wired.com/2015/01/new-3ds-zelda-majoras-mask/",
      "http://www.wired.com/2015/01/daniel-gebhart-de-koekkoek-bodybuilders/",
      "http://www.cnn.com/2015/02/04/asia/taiwan-plane-crash-transasia/index.html",
      "http://money.cnn.com/2015/02/04/autos/toyota-camry-lawsuit/index.html",
      "http://www.businessinsider.com/argentina-president-makes-racist-joke-in-china-2015-2",
      "http://www.businessinsider.com/alternative-uses-for-tinder-2015-2",
      "http://www.businessinsider.com/most-visited-cities-in-the-world-2015-1",
      // Australian politics, ya, don't care!! :)
      "http://www.abc.net.au/news/2015-01-19/matthewson-signs-of-mutiny-on-the-good-ship-abbott/6024180",
      // Don't care about random PC laptop launches...
      "http://recode.net/2015/02/06/dude-the-dell-xps-13-is-a-pretty-sweet-macbook-air-competitor/",
      // Sports...
      "http://www.latimes.com/sports/clippers/la-sp-clippers-cavaliers-20150206-story.html",
      "http://www.latimes.com/sports/sportsnow/la-sp-sn-michael-phelps-return-swimming-20150206-story.html",
      "http://www.latimes.com/sports/sportsnow/la-sp-sn-nba-playoff-format-changes-20150206-story.html",
      "http://www.latimes.com/sports/lakers/lakersnow/la-sp-ln-preview-lakers-at-orlando-magic-20150205-story.html",
      "http://www.latimes.com/sports/sportsnow/la-sp-sn-ducks-nashville-predators-nhl-capitals-boudreau-20150205-story.html",
      "http://www.latimes.com/sports/usc/uscnow/la-sp-usc-football-morning-report-spring-game-scheduled-20150205-story.html",
      "http://www.nytimes.com/2015/02/07/sports/in-pond-hockey-no-reason-to-have-one-big-fish.html",
      "http://www.nytimes.com/2015/02/06/sports/olympics/cost-is-big-theme-at-public-meeting-on-boston-bid.html",
      "http://www.nytimes.com/2015/02/06/sports/basketball/at-these-pickup-games-the-camaraderie-never-gets-old.html",
      "http://www.nytimes.com/2015/02/05/sports/baseball/as-wrigley-field-is-rebuilt-the-curious-take-a-look.html",
      "http://www.nytimes.com/2015/01/15/sports/football/nfl-roundup.html",
      "http://www.nytimes.com/2015/01/28/sports/football/in-seahawks-fierce-secondary-new-members-make-the-cut.html",
      // Random business articles about financing / the economy that are unimportant to me.
      "http://www.businessinsider.com/impact-of-mortgage-refinance-on-housing-2013-6",
      "http://www.businessinsider.com/refiners-gas-futures-2012-3",
      "http://www.businessinsider.com/afp-greek-finance-minister-says-eyeing-new-debt-deal-by-end-may-2015-2",
      "http://www.businessinsider.com/r-economist-varoufakis-says-to-become-new-greek-finance-minister-on-tuesday-2015-1",
      "http://www.businessinsider.com/north-korea-is-accepting-rubles-now-2015-1",
      "http://www.businessinsider.com/finance-is-a-strange-industry-2014-8",
      "http://www.businessinsider.com/r-russias-finance-minister-sees-economy-shrinking-4-percent-in-2015-2014-12",
      "http://www.businessinsider.com/financial-advisor-perspectives-october-6-2014-10",
      "http://www.businessinsider.com/r-china-corruption-watchdog-launches-inspections-eyes-sinopec-2014-11",
      // Random uninteresting tech.
      "http://techcrunch.com/2015/02/05/this-millennium-falcon-drone-can-make-the-kessel-run-in-less-than-twelve-parsecs/",
      "http://techcrunch.com/2015/02/01/the-pirate-bay-returns/",
      // Celebrity / entertainment news.
      "http://www.cnn.com/2015/02/08/entertainment/bruce-jenner-car-accident/index.html",
      "http://www.cnn.com/2015/02/08/entertainment/grammys-2015-winners-list-thr/index.html",
      "http://www.cnn.com/2015/02/07/entertainment/rosie-odonnell-leaving-the-view/index.html",
      "http://www.cnn.com/2015/02/05/entertainment/bobbi-kristina-brown-players-who-is-who/index.html",
      "http://www.cnn.com/2015/02/04/entertainment/feat-magic-mike-xxl-poster-trailer/index.html",
      "http://www.cnn.com/2015/02/03/entertainment/china-katy-perry-celebrity-nicknames/index.html",
      "http://www.cnn.com/2015/01/25/entertainment/feat-sag-awards-2015-wrap/index.html",
      "http://www.cnn.com/2015/01/25/entertainment/feat-sag-awards-andy-samberg-rashida-jones-speech/index.html",
      "http://techcrunch.com/2015/02/13/samsung-could-roll-back-its-own-software-and-embrace-microsoft-for-galaxy-s6/",
      "http://techcrunch.com/2015/02/13/google-and-mattel-revive-the-85-year-old-view-master-with-cardboard-vr/",
      "http://techcrunch.com/2015/02/13/watch-tim-cook-speak-at-president-obamas-summit-on-cybersecurity/",
      "http://techcrunch.com/2015/02/13/a-third-act-for-biofuels/",
      "http://techcrunch.com/2015/02/13/alf/",
      "http://techcrunch.com/2015/02/13/rockbot-anthem/");

  public static Map<Article, Double> getScores(
      User user, Iterable<String> urlStrings, Scorer scorer) throws BiznessException {
    Map<Article, Double> scoreMap = Maps.newHashMap();
    Collection<Article> articles = ArticleCrawler.getArticles(urlStrings).values();
    for (Article article : articles) {
      // Use the holdback for the benchmark. The other 80% are used
      // to train the neural network.
      if (NeuralNetworkTrainer.isInTrainingHoldback(article)) {
        scoreMap.put(article, scorer.getScore(user, article));
      }
    }
    return scoreMap;
  }

  /**
   * Prints out a performance score (aka a "grade") for how well the Scorer did
   * at creating scores for the passed Article -> Score maps.
   */
  public static void grade(Map<Article, Double> goodScoreMap, Map<Article, Double> badScoreMap) {
    int positives = 0;
    int falseNegatives = 0;
    List<String> falseNegativesTitles = new ArrayList<>();
    for (Map.Entry<Article, Double> entry : goodScoreMap.entrySet()) {
      double score = entry.getValue();
      Article article = entry.getKey();
      if (score > 0.5) {
        positives++;
      } else {
        falseNegatives++;
        falseNegativesTitles.add(article.getTitle());
      }
    }
    int negatives = 0;
    int falsePositives = 0;
    for (Double score : badScoreMap.values()) {
      if (score <= 0.5) {
        negatives++;
      } else {
        falsePositives++;
      }
    }
    System.out.println("Positives: " + positives
        + " (" + (int) ((double) 100 * positives / goodScoreMap.size()) + "% correct)");
    System.out.println("False negatives: " + falseNegatives);
    System.out.println("False positives: " + falsePositives);
    System.out.println("Negatives: " + negatives
        + " (" + (int) ((double) 100 * negatives / badScoreMap.size()) + "% correct)");
    System.out.println("Percent correct: " +
        (int) (100 * (((double) positives + negatives)
            / (goodScoreMap.size() + badScoreMap.size()))) + "%");
    System.out.println("False negative titles:");
    for (int i = 0; i < falseNegativesTitles.size(); i++) {
      System.out.println("  " + falseNegativesTitles.get(i));
    }
  }

  /**
   * Returns a multiset that counts how many scores are within each 1/10th
   * decimal increment.  Field "0" contains a count of scores between 0 and
   * 0.1, field "1" contains a count of scores between 0.1 and 0.2, etc.
   */
  private static Multiset<Integer> getHistogram(Iterable<Double> scores) {
    Multiset<Integer> histogram = HashMultiset.create();
    for (Double score : scores) {
      int bucket = (int) (score * 10);
      if (bucket < 0 || bucket >= 10) {
        bucket = -100;
      }
      histogram.add(bucket);
    }
    return histogram;
  }

  private static String createStars(int good, int bad) {
    return Joiner.on("").join(Iterables.limit(Iterables.cycle("g"), good)) +
        Joiner.on("").join(Iterables.limit(Iterables.cycle("B"), bad));
  }

  private static void printHistogram(
      Map<Article, Double> goodScores, Map<Article, Double> badScores) {
    Multiset<Integer> goodHistogram = getHistogram(goodScores.values());
    Multiset<Integer> badHistogram = getHistogram(badScores.values());
    for (int i = 9; i >= 0; i--) {
      String start = "0." + i;
      String end = (i == 9) ? "1.0" : "0." + (i + 1);
      System.out.println("* " + start + " to " + end + ": "
          + createStars(goodHistogram.count(i), badHistogram.count(i)));
    }
    System.out.println("* SCORE OUT OF RANGE: "
        + createStars(goodHistogram.count(-100), badHistogram.count(-100)));
  }

  public static void main(String args[]) throws Exception {
    User jonUser = Users.getByEmail("panaceaa@gmail.com");
    Asserts.assertNotNull(jonUser, "Could not read user");

    Map<Article, Double> goodScores =
        getScores(jonUser, GOOD_URLS, NeuralNetworkScorer.getInstance());
    Map<Article, Double> badScores =
        getScores(jonUser, BAD_URLS, NeuralNetworkScorer.getInstance());
    System.out.println("\nNEURAL NETWORK SCORER:");
    printHistogram(goodScores, badScores);
    grade(goodScores, badScores);

    goodScores = getScores(jonUser, GOOD_URLS, HeuristicScorer.getInstance());
    badScores = getScores(jonUser, BAD_URLS, HeuristicScorer.getInstance());
    System.out.println("\nHEURISTIC SCORER:");
    printHistogram(goodScores, badScores);
    grade(goodScores, badScores);

    System.out.println("\n");
  }
}
