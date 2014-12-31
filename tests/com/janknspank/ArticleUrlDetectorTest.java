package com.janknspank;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ArticleUrlDetectorTest {
  private static final String[] POSITIVES = {
      "http://abcnews.go.com/2020/story?id=124078",
      "http://abcnews.go.com/2020/20-2020-surprising-celebrity-infomercials/story?id=13623616",
      "http://abcnews.go.com/ABCNews/cheerleader-fights-back-critics-big-game-hunting/story?id=24398963",
      "http://abcnews.go.com/ABC_Univision/ABC_Univision/trouble-paradise-us-brazil/story?id=20320361",
      "http://abcnews.go.com/ABC_Univision/cuban-blogger-yoani-snchezs-visit-brazil-sparks-protests/story?id=18556994",
      "http://abcnews.go.com/ABC_Univision/Entertainment/draco-rosa-livin-la-vida-loca-beating-cancer/story?id=18766491",
      "http://abcnews.go.com/Blotter/60-seconds-video-shows-us-secret-snatch-operation/story?id=22449318",
      "http://abcnews.go.com/Business/10-brands-changing-public-image/story?id=23160475",
      "http://abcnews.go.com/Business/story?id=89092",
      "http://abcnews.go.com/US/2014-fifa-world-cup-win-lose-draw-hows/story?id=24321155",
      "http://abcnews.go.com/US/wireStory/suspected-honduran-drug-kingpins-extradited-us-27726946",
      "http://abcnews.go.com/US/wireStory/top-news-2014-left-public-grasping-answers-27671210",
      "http://abcnews.go.com/US/woman-shot-killed-year-walmart/story?id=27907997",
      "http://abcnews.go.com/Weird/wireStory/boy-sends-allowance-save-uab-football-27716105",
      "http://abcnews.go.com/WN/DianeSawyer/diane-sawyers-biography/story?id=9380180",
      "http://america.aljazeera.com/articles/2014/12/30/venezuela-russiaoil.html",
      "http://arstechnica.com/apple/2005/05/305/",
      "http://arstechnica.com/apple/2005/09/nano/",
      "http://arstechnica.com/apple/2007/04/apple-going-to-offer-itunes-subscription-service-concentrate-and-ask-again/",
      "http://arstechnica.com/apple/2008/07/iphone3g-review/",
      "http://arstechnica.com/archive/news/1032300246.html",
      "http://arstechnica.com/cars/2014/12/california-dmv-will-miss-its-deadline-for-driverless-car-regulations/",
      "http://arstechnica.com/gadgets/2003/03/soundcathode/",
      "http://arstechnica.com/news.ars/post/20050427-4857.html",
      "http://arstechnica.com/telecom/news/2009/02/gop-strives-to-get-their-tech-on.ars",
      "http://arstechnica.com/the-multiverse/2014/12/battle-of-the-five-armies-is-a-soulless-end-to-the-flawed-hobbit-trilogy/",
      "http://arstechnica.com/uncategorized/2003/05/300-2/",
      "http://arstechnica.com/uncategorized/2008/03/broadcasters-blast-fcc-localism-proposals/",
      "http://blog.chron.com/sciguy/2014/12/an-engineer-who-recovered-eight-apollo-capsules-from-the-pacific-tells-of-fishing-orion-from-the-sea/",
      "http://blog.sfgate.com/49ers/2014/12/29/baalke-price-tag-was-too-high-to-trade-up-for-odell-beckham/",
      "http://blog.sfgate.com/49ers/2014/12/30/jed-york-on-knbr-winning-with-class-is-what-matters/",
      "http://blog.washingtonpost.com/capitalweathergang/2008/01/about_this_blog.html",
      "http://blog.washingtonpost.com/fact-checker/2007/09/about_the_fact_checker.html",
      "http://boston.com/community/blogs/crime_punishment/2014/02/political_rift_over_coakleys_s.html",
      "http://boston.com/news/local/massachusetts/2014/12/29/blackstone-mother-pleads-not-guilty-murder-infant-death-case/1pZ9tly1GUZ9w5Yl1tRpSP/story.html",
      "http://boston.com/sports/touching_all_the_bases/2014/12/_playing_nine_innings_while.html",
      "http://brainstormtech.blogs.fortune.cnn.com/2009/11/06/andreessen-on-skype/",
      "http://buzz.money.cnn.com/2013/01/25/herbalife-ackman-icahn/",
      "http://curbed.com/archives/2014/12/30/best-homes-architectural-digest-dwell-house-beautiful.php",
      "http://dealbook.nytimes.com/2014/02/09/the-new-normal-for-tech-companies-and-others-the-stealth-i-p-o/",
      "http://eatocracy.cnn.com/2010/05/12/chef-besh-eat-u-s-seafood-save-a-way-of-life/",
      "http://eatocracy.cnn.com/2014/08/07/summer-only-treats/",
      "http://edition.cnn.com/2013/02/04/opinion/opinion-boredom-work-mark-de-rond-route-to-the-top/index.html",
      "http://editors.bloomberg.com/news/2013-03-18/republicans-foil-what-most-u-s-wants-with-gerrymandering.html",
      "http://features.blogs.fortune.cnn.com/2012/02/27/general-motors-last-tango-in-paris/",
      "http://finance.boston.com/boston/news/read/20733509/zipcar_unveils_inaugural_future_metropolis_index_to_measure/",
      "http://finance.fortune.cnn.com/2011/05/10/yes-microsoft-is-buying-skype/",
      "http://go.bloomberg.com/tech-blog/2012-02-27-the-decline-of-social-gaming-on-facebook-may-be-exaggerated/",
      "http://insidescoopsf.sfgate.com/blog/2014/12/17/your-guide-to-the-progress-the-new-sequel-to-state-bird-provisions/",
      "http://knowmore.washingtonpost.com/2014/05/08/hardly-anyone-walks-to-work-especially-not-in-the-south/",
      "http://live.washingtonpost.com/ask-boswell-20141229.html",
      "http://management.fortune.cnn.com/2011/06/30/harvard-business-school-extreme-makeover/",
      "http://markets.cbsnews.com/AP-GfK-Poll-Americans-support-menu-labeling/017c0215bce7d92e/",
      "http://markets.cbsnews.com/Google-confirms-it-sold-barge-docked-in-Maine/39e802b6c46600b2/99589142/",
      "http://markets.cbsnews.com/Googles-pivotal-IPO-launched-a-decade-of-big-bets/43b914344536deee/3306396/",
      "http://money.cnn.com/2006/08/09/technology/webaroundtheworld.biz2/",
      "http://postcards.blogs.fortune.cnn.com/2011/10/20/buffett-rule/",
      "http://tech.fortune.cnn.com/2010/10/04/verizons-refund-is-just-the-start-of-a-shakeup-in-wireless/",
      "http://techcrunch.com/2006/10/31/2080/",
      "http://techcrunch.com/2014/12/30/what-the-hell-is-a-startup-anyway/",
      "http://travel.cnn.com/face-face-portraits-human-spirit-046291",
      "http://us.cnn.com/2014/03/02/africa/gallery/cnnee-vida-oscar-pistorius/index.html",
      "http://voices.washingtonpost.com/rawfisher/2007/07/butch_is_back_a_jazz_legend_re.html",
      "http://www.abc.net.au/catalyst/stories/4002580.htm",
      "http://www.abc.net.au/news/2014-12-26/world-war-i-relatives-indigenous-soldiers-fight-for-recognition/5954752",
      "http://www.arstechnica.com/news/posts/1080081775.html",
      "http://www.bbc.co.uk/news/business-25754888",
      "http://www.bbc.com/capital/story/20130821-six-tales-of-top-level-nepotism",
      "http://www.bbc.com/news/business-14897130",
      "http://www.bloomberg.com/bb/newsarchive/al3Z6pt5_TWY.html",
      "http://www.bloomberg.com/dataview/2014-02-25/bubble-to-bust-to-recovery.html",
      "http://www.bloomberg.com/news/2010-04-16/store-owners-seeking-premium-u-s-real-estate-wait-for-retailers-to-die-.html",
      "http://www.bloomberg.com/news/2014-12-17/carmike-to-pull-the-interview-from-theaters-following-threats.html",
      "http://www.bloomberg.com/now/2011-05-25/introducing-bloomberg-view-putting-the-world-in-focus/",
      "http://www.boston.com/2012/03/16/dating/nkfeF9xitUXoWteAwWX1QI/story.html",
      "http://www.boston.com/ae/blogs/mediaremix/2014/03/oprah_teases_lindsay_lohans_re.html",
      "http://www.boston.com/blogs/ae/restaurants/the-restaurant-hub/2014/05/boston-mexican-cantina-cinco-.html",
      "http://www.boston.com/jobs/news/2014/05/03/nomination-letters-amedisys-home-healthcare/9lKICo47FRLGHGIKR4kSJN/story.html",
      "http://www.boston.com/lifestyle/relationships/blogs/blissfullyinspired/2014/02/so_who_got_engaged_on_valentin.html",
      "http://www.breitbart.com/big-government/2014/12/28/anti-police-protesters-plan-to-disrupt-new-years-eve-celebrations/",
      "http://www.breitbart.com/Big-Hollywood/2014/12/03/investigation-lena-dunhams-republican-rapist-story-falls-apart-under-scrutiny",
      "http://www.breitbart.com/Breitbart-TV/2014/12/10/Breitbarts-Nolte-Lena-Dunham-Letting-Barry-One-Twist-in-the-Wind-with-Rape-Allegation",
      "http://www.businessweek.com/articles/2013-05-14/harvard-business-school-boat-parody-sinks",
      "http://www.businessweek.com/news/2011-10-08/bofa-hands-sallie-krawcheck-6-million-severance-after-ouster.html",
      "http://www.businessweek.com/the_thread/brandnewday/archives/2006/10/is_the_champagne_in_the_jay-z_video_for_real_its_complicated.html",
      "http://www.cbc.ca/m/news/canada/british-columbia/b-c-premier-christy-clark-defends-site-c-as-a-100-year-plan-1.2883064",
      "http://www.cbc.ca/news/business/biggest-marketing-fails-of-2014-apple-coors-and-more-1.2877875",
      "http://www.cbc.ca/news/world/black-ivory-coffee-canadian-s-elephant-poop-coffee-makes-a-pricey-cup-of-joe-1.2881812",
      "http://www.cbc.ca/newsblogs/yourcommunity/2014/04/rob-ford-crackathon-video-game-lights-up-the-web.html",
      "http://www.cbsnews.com/8301-204_162-57557287/french-scientists-warn-sperm-counts-falling-for-men/",
      "http://www.cbsnews.com/8301-3460_162-57454827/face-the-nation-transcripts-june-17-2012-gov-romney-senator-graham-gov-dean/?morningSubCatAsset&tag=showDoorSubcatGrid",
      "http://www.cbsnews.com/news/fbi-still-believes-north-korea-is-responsible-for-sony-hack/",
      "http://www.cbsnews.com/stories/2002/11/21/health/main530238.shtml",
      "http://www.channelnewsasia.com/news/asiapacific/three-more-bodies-found/1559694.html",
      "http://www.channelnewsasia.com/news/asiapacific/search-for-qz8501-at/1557408.html",
      "http://www.channelnewsasia.com/news/entertainment/local-celebs-show-support/1554884.html",
      "http://www.chron.com/disp/story.mpl/ap/world/5928101.html",
      "http://www.chron.com/life/home/design/article/Fulshear-home-is-Texas-country-style-with-modern-5973903.php",
      "http://www.chron.com/news/local/article/Breastaurant-CEO-offers-breast-implants-after-5985419.php",
      "http://www.chron.com/news/texas/article/Rick-Perry-Jobs-juggernaut-meets-Governor-Oops-5985481.php",
      "http://www.cleveland.com/business/index.ssf/2014/12/marc_krantz_managing_partner_o.html",
      "http://www.cleveland.com/sunnews/index.ssf/2012/09/read_your_news_from_your_commu.html",
      "http://www.cnbc.com/id/102301617",
      "http://www.cnbc.com/id/102301834/",
      "http://www.cnbc.com/id/102301896/asgasgag",
      "http://www.cnn.com/2003/HEALTH/02/16/false.memory.ap/index.html",
      "http://www.cnn.com/2005/TECH/science/05/12/new.species.ap/index.html",
      "http://www.cnn.com/2011/11/09/studentnews/black-in-america-silicon-valley-educator-parent-guide/index.html",
      "http://www.cnn.com/2011/SHOWBIZ/Music/02/16/grace.potter.soundcheck",
      "http://www.cnn.com/2014/07/14/sport/football/world-cup-team-of-the-tournament/index.html",
      "http://www.cnn.com/SPECIALS/2010/building.up.america/index.html",
      "http://www.latimes.com/books/jacketcopy/la-et-jc-sherlock-holmes-belongs-to-us-all-20141103-story.html",
      "http://www.latimes.com/business/hiltzik/la-fi-mh-issas-big-dud-20141224-column.html",
      "http://www.latimes.com/food/dailydish/la-dd-recipes-for-santa-or-yourself-on-christmas-eve-20141222-storygallery.html",
      "http://www.latimes.com/la-california-20141016-htmlstory.html",
      "http://www.latimes.com/la-me-aqueduct-comments-20131028,0,3652614.story",
      "http://www.latimes.com/local/crime/la-me-lapd-trust-20140828-story.html#page=1",
      "http://www.latimes.com/local/la-me-c1-aqueduct-bomber-20131030-dto,0,7855162.htmlstory",
      "http://www.latimes.com/local/lanow/la-me-ln-fire-homeless-20140504,0,756604.story",
      "http://www.latimes.com/news/politics/la-na-rove14aug14,1,1728147.story",
      "http://www.latimes.com/sports/sportsnow/la-sp-sn-nhl-all-star-zemgus-girgensons20141230-story.html",
      "http://www.mercurynews.com/business/ci_24450039/twitter-increases-ipo-price-range-following-facebooks-path",
      "http://www.mercurynews.com/ci_18730079",
      "http://www.mercurynews.com/ci_18730079?IADID=Search-www.mercurynews.com-www.mercurynews.com%0A",
      "http://www.mercurynews.com/bay-area-living/ci_27235425/phallic-looking-play-doh-toy-yanked-after-backlash",
      "http://www.money.cnn.com/gallery/investing/2013/04/28/worlds-top-stock-markets/index.html",
      "http://www.nytimes.com/1993/11/16/business/house-banking-chairman-increases-pressure-on-the-fed.html",
      "http://www.sfgate.com/49ers/article/49ers-say-goodbye-to-Jim-Harbaugh-and-a-5982751.php",
      "http://www.sfgate.com/entertainment/garchik/article/Back-of-the-Datebook-gang-pitches-in-to-rescue-5976457.php",
      "http://www.techcrunch.com/2008/11/03/old-friends-wink-and-reunioncom-reconnect-merge/",
      "http://www.washingtonpost.com/blogs/the-switch/wp/2013/12/23/heres-what-paul-krugman-doesnt-get-about-bitcoin/",
      "http://www.washingtonpost.com/news/morning-mix/wp/2014/12/16/hong-kongs-occupy-central-protest-is-no-more/",
      "http://www.washingtonpost.com/politics/house-majority-whip-scalise-confirms-he-spoke-to-white-nationalists-in-2002/2014/12/29/7f80dc14-8fa3-11e4-a900-9960214d4cd7_story.html",
      "http://www.washingtonpost.com/sports/highschools/prosecutors-allege-that-dc-assault-co-founder-curtis-malone-is-principle-character-in-wide-ranging-drug-conspiracy/2013/10/09/ff92d324-30f6-11e3-8627-c5d7de0a046b_story.html",
      "http://www.washingtonpost.com/world/asia_pacific/air-search-for-missing-airasia-jet-suspended/2014/12/28/a903067e-8e8c-11e4-a900-9960214d4cd7_story.html",
      "http://www1.channelnewsasia.com/premier/2014/12/26/swarovski-milner-endangered_species_portraits_mosaic/"
  };
  private static final String[] NEGATIVES = {
      "http://abc.net.au",
      "http://abc.net.au/btn/",
      "http://abcnews.go.com",
      "http://abcnews.go.com/2020",
      "http://abcnews.go.com/author/",
      "http://abcnews.go.com/author/andrew_springer",
      "http://abcnews.go.com/business",
      "http://abcnews.go.com/topics/",
      "http://abcnews.go.com/topics/business/airlines/aer-lingus.htm",
      "http://abcnews.go.com/topics/business/airlines/hawaiian-airlines.htm?mediatype=Image",
      "http://abcnews.go.com/topics/business/airlines/hawaiian-airlines.htm?mediatype=Video",
      "http://abcnews.go.com/topics/entertainment/music/justin-bieber.htm",
      "http://abcnews.go.com/topics/news/angola.htm",
      "http://archives.cbc.ca",
      "http://arstechnica.com",
      "http://arstechnica.com/2014-ars-childs-play-drive-official-sweepstakes-rules/",
      "http://arstechnica.com/apple/",
      "http://arstechnica.com/author/andrew-webster/",
      "http://arstechnica.com/contact-us/",
      "http://arstechnica.com/gadgets/",
      "http://arstechnica.com/gadgets/2008/09/guide-200809/2/",
      "http://arstechnica.com/apple/2008/07/iphone3g-review/1/",
      "http://arstechnica.com/the-multiverse/",
      "http://arstechnica.com/uncategorized/",
      "http://blog.sfgate.com/49ers/",
      "http://blog.sfgate.com/dailydish/",
      "http://blog.sfgate.com/dailydish/2005/08/",
      "http://bloomberg.com/careers",
      "http://bloomberg.com/company",
      "http://boston.com/ae/fun/",
      "http://boston.com/community/blogs/crime_punishment/",
      "http://boston.com/weather/",
      "http://buzz.money.cnn.com",
      "http://buzz.money.cnn.com/author/brooneycnn/",
      "http://cnn.com",
      "http://cnn.com/about/",
      "http://crime.latimes.com",
      "http://eatocracy.cnn.com",
      "http://eatocracy.cnn.com/category/5-things-to-know-for-your-new-day/",
      "http://eatocracy.cnn.com/category/news/business-and-farming-news/fast-food-business-and-farming-news/chick-fil-a/",
      "http://economy.money.cnn.com/author/cnnkurtz/",
      "http://edition.cnn.com/interactive_legal.html",
      "http://edition.cnn.com/LATINAMERICA",
      "http://finance.boston.com/boston/markets",
      "http://finance.sfgate.com",
      "http://futureforward.channelnewsasia.com",
      "http://go.bloomberg.com/market-now/",
      "http://go.bloomberg.com/tech-blog/2012/02/",
      "http://go.bloomberg.com/tech-blog/networking-equipment/",
      "http://healthyeating.sfgate.com",
      "http://investing.businessweek.com/research/company/overview/overview.asp",
      "http://investing.businessweek.com/research/markets/detail/marketdetail.asp?marketCode=CCMP%3AIND",
      "http://knowmore.washingtonpost.com",
      "http://latimesblogs.latimes.com/alltherage/",
      "http://markets.cbsnews.com/I:DJI/quote-324977/",
      "http://mobileworldcongress.edition.cnn.com/Event/Mobile_World_Congress_2014_As_it_happens",
      "http://primary.washingtonpost.com/blogs/right-turn/",
      "http://projects.washingtonpost.com/2008-presidential-candidates/",
      "http://startup.channelnewsasia.com/meet-the-judges",
      "http://tech.fortune.cnn.com/category/enterprise/",
      "http://techcrunch.com",
      "http://techcrunch.com/entertainment-2/",
      "http://techcrunch.com/topic/company/chatroulette/popular/",
      "http://topics.bloomberg.com/abu-dhabi/",
      "http://topics.cleveland.com/tag/fashion%20flash/index.html",
      "http://topics.cnn.com/topics/Bullying",
      "http://voices.washingtonpost.com/politics/campaigns.html",
      "http://washingtonpost.com/pb/philip-bump",
      "http://www.abc.net.au",
      "http://www.abc.net.au/7.30/",
      "http://www.abc.net.au/abc3/news/",
      "http://www.abc.net.au/copyright.htm",
      "http://www.abc.net.au/technology/techexplained/default.htm",
      "http://www.abc.net.au/tv/connect/messageboards.htm",
      "http://www.arstechnica.com/site/user-agreement.ars",
      "http://www.bbc.com/capital/tags/personal-finance",
      "http://www.bbc.co.uk/capital/tags/personal-finance",
      "http://www.bloomberg.com",
      "http://www.bloomberg.com/about/management/beth/",
      "http://www.bloomberg.com/bsustainable/",
      "http://www.bloomberg.com/notices/privacy.html",
      "http://www.bloomberg.com/now/2010/08/?post_type=press_release",
      "http://www.bloomberg.com/now/2010/07/page/2/?post_type=press_release", // Because of page 2.
      "http://www.bloomberg.com/solutions/bloomberg_enterprise/bloomberg_vault/",
      "http://www.boston.com",
      "http://www.boston.com/ae/",
      "http://www.boston.com/ae/blogs/mediaremix/",
      "http://www.boston.com/blogs/ae/restaurants/the-restaurant-hub/",
      "http://www.boston.com/lifestyle/relationships/blogs/blissfullyinspired/",
      "http://www.breitbart.com",
      "http://www.breitbart.com/california/",
      "http://www.breitbart.com/news/topic/science-tech/",
      "http://www.businessweek.com",
      "http://www.businessweek.com/features/design-issue-2014/",
      "http://www.businessweek.com/authors/1062-francesca-di-meglio",
      "http://www.businessweek.com/authors/1416-john-tozzi/page/117",
      "http://www.cbc.ca",
      "http://www.cbc.ca/aboutcbc/discover/privacy.html",
      "http://www.cbc.ca/newsblogs/politics/inside-politics-blog/author/kady-omalley/",
      "http://www.cbc.ca/services/consumer_recordings.html",
      "http://www.cbsnews.com/",
      "http://www.cbsnews.com/entertainment/",
      "http://www.cbsnews.com/team/steve-kroft/",
      "http://www.cbsnews.com/media/top-health-stories-of-2014/",
      "http://www.channelnewsasia.com",
      "http://www.channelnewsasia.com/changinglives",
      "http://www.channelnewsasia.com/news/about/terms",
      "http://www.cleveland.com",
      "http://www.cleveland.com/aboutus/",
      "http://www.cleveland.com/business/index.ssf/technology",
      "http://www.cleveland.com/religion/index.ssf/faith_on_the_fly/",
      "http://www.cnbc.com",
      "http://www.cnbc.com/account",
      "http://www.cnbc.com/id/10000066",
      "http://www.cnbc.com/id/17689937",
      "http://www.cnn.com",
      "http://www.cnn.com/tools/index.html",
      "http://www.latimes.com",
      "http://www.latimes.com/aboutus",
      "http://www.latimes.com/business/personalfinance/",
      "http://www.latimes.com/topic/crime-law-justice/law-enforcement/los-angeles-police-department-ORGOV000939.topic",
      "http://www.mercurynews.com/",
      "http://www.mercurynews.com/portlet/layout/html/privacypolicy/privacypolicy.jsp?siteId=568",
      "http://www.sfgate.com",
      "http://www.sfgate.com/entertainment/blogs/",
      "http://www.washingtonpost.com/",
      "http://www.washingtonpost.com/2011/05/23/AFPeNbAH_page.html",
      "http://www.washingtonpost.com/blogs/capital-weather-gang/about-and-faq/",
      "http://www.washingtonpost.com/blogs/capital-weather-gang/page/2/",
      "http://www.washingtonpost.com/niraj-chokshi/2014/06/10/6b40abf2-f0cb-11e3-9ebc-2ee6f81ed217_page.html",
      "http://www1.channelnewsasia.com/regionalevents/",
      "http://www1.channelnewsasia.com/premier/wp-admin/admin-ajax.php?action=wml_load_posts&shortcodeId=1"
  };

  @Test
  public void testUrlDetector() throws Exception {
    for (String positive : POSITIVES) {
      assertTrue("URL should be detected as an article: " + positive,
          ArticleUrlDetector.isArticle(positive));
    }
    for (String negative : NEGATIVES) {
      assertFalse("URL should NOT be detected as an article: " + negative,
          ArticleUrlDetector.isArticle(negative));
    }
  }
}
