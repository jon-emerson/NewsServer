package com.janknspank.crawler;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.api.client.util.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.janknspank.classifier.ClassifierException;
import com.janknspank.classifier.FeatureClassifier;
import com.janknspank.classifier.Vector;
import com.janknspank.common.DateParser;
import com.janknspank.common.StringHelper;
import com.janknspank.crawler.social.FacebookData;
import com.janknspank.crawler.social.SocialException;
import com.janknspank.crawler.social.TwitterData;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.nlp.KeywordFinder;
import com.janknspank.nlp.KeywordUtils;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.SocialEngagement;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.CrawlerProto.SiteManifest;

/**
 * ArticleCreator is a very high-level object in the crawling system: It's
 * responsible for creating Article objects from yet-uninterpreted
 * DocumentNodes.  ArticleCreator digs through the DocumentNode to find all
 * the Article's attributes, such as entities, industry and other
 * classification features, plus Facebook / other social engagement scores.
 */
class ArticleCreator {
  private static final int MAX_TITLE_LENGTH =
      Database.getStringLength(Article.class, "title");
  private static final int MAX_DESCRIPTION_LENGTH =
      Database.getStringLength(Article.class, "description");
  private static final int MAX_ORIGIN_LENGTH =
      Database.getStringLength(Article.class, "origin");
  private static final Set<String> IMAGE_URL_BLACKLIST = ImmutableSet.of(
      "http://media.cleveland.com/design/alpha/img/logo_cleve.gif",
      "http://www.sfgate.com/img/pages/article/opengraph_default.png",
      "http://www.telegraph.co.uk/template/ver1-0/i/telegraphFacebook.jpg",
      "http://thehill.com/sites/default/files/thehill_logo_200.jpg",
      "http://www.nationaljournal.com/nationaljournal-fb.png",
      "http://s.wsj.net/blogs/img/WSJ_Logo_BlackBackground_1200x630social",
      "http://www.buffalonews.com/images/BNSocialShareLG.jpg",
      "http://www.abc.net.au/news/linkableblob/6072216/data/abc-news.jpg?2",
      "http://images.forbes.com/media/assets/forbes_1200x1200.jpg",
      "http://images.rigzone.com/images/rz-facebook.jpg",
      "http://www.inc.com/images/incthumb250.png",
      "http://fm.cnbc.com/applications/cnbc.com/staticcontent/img/cnbc_logo.gif",
      "http://static.cdn-seekingalpha.com/uploads/2013/8/19/social_sa_logo.png",
      "http://idge.staticworld.net/ifw/IFW_logo_social_300x300.png",
      "http://www.scientificamerican.com/sciam/includes/themes/sciam/images/logo400x400.jpg",
      "http://mw1.wsj.net/MW5/content/images/logos/mw-social-logo.jpg",
      "http://www1.ibdcd.com/images/IBDicon_309171.png",
      "https://s0.wp.com/i/blank.jpg",
      "http://local.mercurynews.com/common/dfm/assets/logos/1200x627/mercurynews.png",
      "http://www.buffalonews.com/images/BNSocialShareLG.jpg",
      "http://www.accountingtoday.com/media/newspics/AT_180x180.jpg",
      "http://assets.fiercemarkets.net/public/opengraphimages/updated/opengraph_fierceenergy.jpg",
      "https://s0.wp.com/wp-content/themes/vip/recode/img/_default/default-thumbnail-post.png",
      "http://media.scmagazine.com/images/2013/02/19/sc_logo_21413_345884.png",
      "http://idge.staticworld.net/nww/nww_logo_300x300.png",
      "http://oystatic.ignimgs.com/src/core/img/widgets/global/page/ign-logo-100x100.jpg",
      "https://s0.wp.com/wp-content/themes/vip/time2014/img/time-logo-og.png",
      "http://www.usertesting.com/blog/wp-content/uploads/2014/05/Poll-FeaturedImage.png");
  private static final Set<Pattern> IMAGE_URL_BLACKLIST_PATTERNS = ImmutableSet.of(
      // A black "T", representing the NYTimes.
      // E.g. http://static01.nyt.com/images/icons/t_logo_291_black.png
      Pattern.compile("^http.*nyt[^\\/]*\\.com\\/.*_black\\.png$"),
      // Typically weird 3D renderings of currency conversion ticker symbols.
      // E.g. http://cdn.fxstreet.com/img/facebook/*/FXstreet-90x90.png
      Pattern.compile("^http:\\/\\/cdn\\.fxstreet\\.com\\/"),
      // Andreesen Horowitz logo.
      // E.g. http://d3n8a8pro7vhmx.cloudfront.net/bhorowitz/sites/1/meta_images/original/logo.png?1383577601
      Pattern.compile("\\/bhorowitz\\/sites\\/1\\/meta_images\\/original/logo.png(\\?.*)?$"),
      // Bloomberg logo.
      // E.g. http://cdn.gotraffic.net/politics/20150107201907/public/images/logos/FB-Sharing.73b07052.png
      Pattern.compile("\\/\\/cdn\\.gotraffic\\.net\\/.*FB-Sharing"),
      // E.g. http://www.abc.net.au/news/linkableblob/6072216/data/abc-news.jpg
      Pattern.compile("\\/\\/www\\.abc\\.net\\.au\\/.*\\/data\\/abc-news\\.jpg.*"),
      // E.g. https://dnqgz544uhbo8.cloudfront.net/_/fp/img/default-preview-image.IsBK38jFAJBlWifMLO4z9g.png
      Pattern.compile("\\/\\/[\\w]+\\.cloudfront\\.net\\/_\\/fp\\/img\\/default-preview-image\\."));
  private static final Pattern TEXT_TO_REMOVE_FROM_TITLES[] = new Pattern[] {
      Pattern.compile("<\\/?(i|b|em|strong)>"),
      Pattern.compile("^[a-zA-Z\\.]{3,15}\\s(\\||\\-\\-|\\-|\\–|\u2014)\\s"),
      Pattern.compile("\\s\\([A-Za-z]{2,15}(\\s[A-Za-z]{2,15})?\\)$"),
      Pattern.compile("\\s*(\\||\\-\\-|\\-|\\–|\u2014)\\s+([A-Z][A-Za-z]+\\.com)$"),
      Pattern.compile("\\s*(\\||\\-\\-|\\-|\\–|\u2014)\\s+[A-Z][A-Za-z\\s'']{2,25}$"),
      Pattern.compile("\\s+(\\||\\|\\|)\\s+[A-Za-z\\s'']{2,25}$"),
      Pattern.compile("\\s+(\\||\\|\\|)\\s+http:\\/\\/[A-Za-z\\.]+$"),
      Pattern.compile("\\s+\\[[A-Z]+\\]$"), // E.g. " [REVIEW]".
      Pattern.compile("^Daily Report: "), // NYTimes Bits Blog daily report.
      Pattern.compile(" - artnet News$")}; // ArtNet articles.
  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s|\\xA0)+");

  // It's a neat trick that stems of 4 characters are actually better than stems
  // closer to full-word lengths.  Do note, that stems of 5 or 6 characters are
  // generally worse.  You either want this value to be 4 or 10+.
  private static final int MAX_STEM_LENGTH = 4;

  public static Article create(Url url, Document document)
      throws RequiredFieldException {
    SiteManifest site = SiteManifests.getForUrl(document.baseUri());

    Article.Builder articleBuilder = Article.newBuilder();
    articleBuilder.setUrlId(url.getId());
    articleBuilder.setUrl(document.baseUri());

    // Paragraphs (required).
    articleBuilder.addAllParagraph(ParagraphFinder.getParagraphs(document));

    // Author.
    String author = getAuthor(document);
    if (author != null) {
      articleBuilder.setAuthor(author);
    }

    // Origin.
    String origin = getOrigin(site, author);
    if (origin != null) {
      articleBuilder.setOrigin(origin);
    }
    // Copyright.
    String copyright = getCopyright(document);
    if (copyright != null) {
      articleBuilder.setCopyright(copyright);
    }

    // Description (required).
    articleBuilder.setDescription(getDescription(document, site));

    // Image url.
    String imageUrl = getImageUrl(document);
    if (imageUrl != null) {
      articleBuilder.setImageUrl(imageUrl);
    }

    // Modified time.
    Long modifiedTime = getModifiedTime(document);
    if (modifiedTime != null) {
      articleBuilder.setModifiedTime(modifiedTime);
    }

    // Published time (required).
    articleBuilder.setPublishedTime(
        Math.min(System.currentTimeMillis(), getPublishedTime(document, url)));

    // Title.
    String title = getTitle(document, site);
    if (title != null) {
      articleBuilder.setTitle(title);
    }

    // Type.
    String type = getType(document);
    if (type != null) {
      articleBuilder.setType(type);
    }

    // Word count (required).
    articleBuilder.setWordCount(getWordCount(document));

    // Features
    Iterable<ArticleFeature> articleFeatures;
    try {
      articleFeatures = FeatureClassifier.classify(articleBuilder);
      articleBuilder.addAllFeature(articleFeatures);
    } catch (ClassifierException e) {
      e.printStackTrace();
      articleFeatures = ImmutableList.of();
    }

    // Keywords.
    articleBuilder.addAllKeyword(KeywordFinder.getInstance().findKeywords(
        url.getId(), title, document, articleFeatures));

    // Since many sites double-escape their HTML entities (why anyone would
    // do this is beyond me), do another escape pass on everything.
    if (articleBuilder.hasDescription()) {
      articleBuilder.setDescription(StringHelper.unescape(articleBuilder.getDescription()).trim());
    }
    if (articleBuilder.hasAuthor()) {
      articleBuilder.setAuthor(StringHelper.unescape(articleBuilder.getAuthor()));
    }
    if (articleBuilder.hasCopyright()) {
      articleBuilder.setCopyright(StringHelper.unescape(articleBuilder.getCopyright()));
    }
    if (articleBuilder.hasImageUrl()) {
      articleBuilder.setImageUrl(StringHelper.unescape(articleBuilder.getImageUrl()));
    }

    // Social Engagement
    try {
      SocialEngagement engagement = FacebookData.getEngagementForArticle(articleBuilder);
      if (engagement != null) {
        articleBuilder.addSocialEngagement(engagement);
      }
    } catch (SocialException e) {
      e.printStackTrace();
    }
    try {
      SocialEngagement engagement = TwitterData.getEngagementForArticle(articleBuilder);
      if (engagement != null) {
        articleBuilder.addSocialEngagement(engagement);
      }
    } catch (SocialException e) {
      e.printStackTrace();
    }

    // Deduping.
    articleBuilder.addAllDedupingStems(getDedupingStems(articleBuilder.getTitle()));

    // Timestamp.
    articleBuilder.setCrawlTime(System.currentTimeMillis());

    // Done!
    return articleBuilder.build();
  }

  public static String getAuthor(Document document) {
    Element metaNode = document.select("html > head meta[name=\"author\"]").first();
    return (metaNode != null) ? metaNode.attr("content") : null;
  }

  public static String getOrigin(SiteManifest site, String author) {
    return ("medium.com".equals(site.getRootDomain()))
        ? author.length() > MAX_ORIGIN_LENGTH ? author.substring(0, MAX_ORIGIN_LENGTH) : author
        : site.getShortName();
  }

  public static String getCopyright(Document document) {
    Element metaNode = document.select("html > head meta[name=\"copyright\"]").first();
    return (metaNode != null) ? metaNode.attr("content") : null;
  }

  private static String getFirstSignificantParagraphText(Document document)
      throws RequiredFieldException {
    Iterable<String> paragraphs = ParagraphFinder.getParagraphs(document);
    String bestBaragraph = "";
    for (String paragraph : paragraphs) {
      if (paragraph.length() >= 50) {
        bestBaragraph = paragraph;
        break;
      }
    }
    return bestBaragraph.length() > MAX_DESCRIPTION_LENGTH
        ? bestBaragraph.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "..."
        : bestBaragraph;
  }

  public static String getDescription(Document document, SiteManifest site)
      throws RequiredFieldException {
    if (site.getUseFirstParagraphAsDescription()) {
      return getFirstSignificantParagraphText(document);
    }

    Element metaEl = JsoupUtils.selectFirst(document, ImmutableList.of(
        "html > head meta[name=\"sailthru.description\"]", // Best, at least on techcrunch.com.
        "html > head meta[name=\"description\"]",
        "html > head meta[name=\"lp\"]",
        "html > head meta[property=\"rnews:description\"]",
        "html > head meta[property=\"og:description\"]",
        "html > head meta[itemprop=\"description\"]"));
    String description = null;
    if (metaEl != null) {
      description = metaEl.attr("content");
    }
    if (Strings.isNullOrEmpty(description)) {
      // Fall back to the first significant paragraph.
      description = getFirstSignificantParagraphText(document);
    }

    if (description.length() > MAX_DESCRIPTION_LENGTH) {
      System.out.println("Warning: Trimming description for url " + document.baseUri());
      description = description.substring(0, MAX_DESCRIPTION_LENGTH - 1) + "\u2026";
    }

    return description.trim();
  }

  @VisibleForTesting
  static boolean isValidImageUrl(String imageUrl) {
    if (imageUrl.isEmpty()
        || IMAGE_URL_BLACKLIST.contains(imageUrl)) {
      return false;
    }
    for (Pattern pattern : IMAGE_URL_BLACKLIST_PATTERNS) {
      if (pattern.matcher(imageUrl).find()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a ranking score for how good we think a meta node's thumbnail URL
   * is.  Higher scores are better.  General guideline:
   * - 1000+: Extremely good, probably the best.
   * - 100 - 1000: Pretty good, we'll think about it.
   * - 1 - 100: Probably crap but worth considering.
   * - Below 0: Not a valid URL.
   */
  private static int getImageUrlRank(Document document, Element metaEl) {
    String imageUrl = metaEl.absUrl("content");

    // Disallow empty and blacklisted URLs.
    if (!isValidImageUrl(imageUrl)) {
      return -1;
    }

    String urlString = document.baseUri();
    if (urlString.contains(".nytimes.com/")) {
      if (imageUrl.contains("-facebookJumbo")) {
        return 1000;
      }
      if (imageUrl.contains("-thumbLarge")) {
        return 200;
      }
    }

    // Once in a while we're graced with this information - super valuable!
    // TODO(jonemerson): Sometimes the width is specified in the thumbnail URL.
    // E.g. on techcrunch.com, it's done with the "w" parameter.
    int width = NumberUtils.toInt(metaEl.attr("width"), 0);
    if (width > 100) {
      return Math.min(1000, width);
    }

    // Sailthru metadata tends to be very high quality.
    if ("sailthru.image.full".equals(metaEl.attr("name"))) {
      return 500;
    }

    // Longer URLs are more likely to be specific to the current article
    // (actually I'm totally guessing).
    return Math.min(120, imageUrl.length()) - 20;
  }

  public static String getImageUrl(Document document) {
    String bestUrl = "";
    int bestUrlRank = 0;
    for (Element metaEl : JsoupUtils.selectAll(document, ImmutableList.of(
        "html > head meta[name=\"thumbnail\"]",
        "html > head meta[name=\"THUMBNAIL_URL\"]",
        "html > head meta[name=\"sailthru.image.full\"]",
        "html > head meta[property=\"og:image\"]",
        "html > head meta[property=\"rnews:thumbnailUrl\"]",
        "html > head meta[itemprop=\"thumbnailUrl\"]"))) {
      int rank = getImageUrlRank(document, metaEl);
      if (rank > bestUrlRank) {
        bestUrl = metaEl.absUrl("content");
        bestUrlRank = rank;
      }
    }

    if (bestUrl.isEmpty()) {
      // TODO(jonemerson): Probably surface this through .manifest files.
      // This is probably specific to ribaj.com.
      Element maybeImageEl = document.select(".article-content figure img").first();
      if (maybeImageEl != null) {
        bestUrl = maybeImageEl.absUrl("src");
      }
    }
    return bestUrl.isEmpty() ? null : bestUrl;
  }

  public static Long getModifiedTime(Document document)
      throws RequiredFieldException {
    Element metaEl = JsoupUtils.selectFirst(document, ImmutableList.of(
        "html > head meta[name=\"utime\"]",
        "html > head meta[itemprop=\"modificationDate\"]",
        "html > head meta[property=\"article:modified_time\"]",
        "html > head meta[itemprop=\"dateModified\"]"));
    return (metaEl != null)
        ? DateParser.parseDateTime(metaEl.attr("content")) : null;
  }

  public static int getWordCount(final Document document) throws RequiredFieldException {
    Pattern pattern = Pattern.compile("[\\s]+");
    int words = 0;
    for (String paragraph : ParagraphFinder.getParagraphs(document)) {
      Matcher matcher = pattern.matcher(paragraph);
      while (matcher.find()) {
        words++;
      }
      words++;
    }
    return words;
  }

  public static long getPublishedTime(Document document, Url url) throws RequiredFieldException {
    Element metaEl = JsoupUtils.selectFirst(document, ImmutableList.of(
        "html > head meta[name=\"ptime\"]", // Usually very precise.
        "html > head meta[name=\"date\"]",
        "html > head meta[name=\"Date\"]", // Abcnews.go.com.
        "html > head meta[name=\"date.release\"]",
        "html > head meta[name=\"pub_date\"]",
        "html > head meta[name=\"publish-date\"]",
        "html > head meta[name=\"OriginalPublicationDate\"]",
        "html > head meta[name=\"sailthru.date\"]",
        "html > head meta[name=\"cXenseParse:recs:publishtime\"]",
        "html > head meta[itemprop=\"datePublished\"]",
        "html > head meta[property=\"pubDate\"]",
        "html > head meta[property=\"article:published_time\"]",
        "html > head meta[property=\"rnews:datePublished\"]",
        "html > head meta[name=\"pdate\"]", // Usually day only.
        "html > head meta[name=\"DC.date.issued\"]", // Abcnews.go.com.
        "html meta[itemprop=\"datePublished\"]", // Cbsnews.com.
        "html > head meta[name=\"live_date\"]")); // Pcmag.com.
    if (metaEl != null && metaEl.hasAttr("content")) {
      return DateParser.parseDateTime(metaEl.attr("content"));
    } else if (metaEl != null && metaEl.hasAttr("value")) {
      return DateParser.parseDateTime(metaEl.attr("value"));
    }

    // Special handling for spectrum.ieee.org.
    if (document.baseUri().startsWith("http://spectrum.ieee.org/")) {
      Element dateTimeEl = document.select(".metadata label[datetime]").first();
      if (dateTimeEl != null) {
        return DateParser.parseDateTime(dateTimeEl.text());
      }
    }

    // Some sites (e.g. ribaj.com) bury a <date> tag into their HTML bodies.
    // E.g.
    // <time pubdate="pubdate" datetime="Fri Feb 27 2015 17:30:00 GMT+0000 (UTC)">
    //     27 February 2015</time>
    Element timeEl = document.select("time").first();
    if (timeEl != null) {
      Long maybeDate = DateParser.parseDateTime(timeEl.text());
      if (maybeDate != null) {
        return maybeDate;
      }
    }

    // See if we can parse a date out of the URL.
    Long date = DateParser.parseDateFromUrl(document.baseUri(), true /* allowMonth */);
    if (date != null) {
      return date;
    }

    // See if we can derive the date from the article contents.
    // NOTE(jonemerson): Maybe we can do this more heuristically.
    for (Element copyrightEl : document.select("body .copyright")) {
      Matcher matcher = Pattern.compile("(0?[1-9]|1[012])\\.[0123]?[0-9]\\.20[0-9]{2}")
          .matcher(copyrightEl.text());
      if (matcher.find()) {
        date = DateParser.parseDateTime(matcher.group());
        if (date != null) {
          return date;
        }
      }
    }

    // Geez, they're making it hard on us.  Why doesn't everyone just tell us
    // when their articles are published??  OK, let's ask Facebook...
    try {
      Long facebookPublishTime = FacebookData.getPublishTime(document.baseUri());
      if (facebookPublishTime != null) {
        return facebookPublishTime;
      }
    } catch (SocialException e) {}

    // Alright, fine.  You win.  You get our discovery date.
    return url.getDiscoveryTime();
  }

  /**
   * Removes cruft from the ends of article titles.
   */
  @VisibleForTesting
  static String cleanTitle(String title) {
    title = StringHelper.unescape(title);

    // Remove duplicative spaces.
    title = title.replaceAll("(\\s|\\xA0){2,100}", " ");

    for (Pattern pattern : TEXT_TO_REMOVE_FROM_TITLES) {
      Matcher matcher = pattern.matcher(title);
      while (matcher.find()) {
        title = title.substring(0, matcher.start()) + title.substring(matcher.end());
        matcher = pattern.matcher(title);
      }
    }
    if (title.length() > MAX_TITLE_LENGTH) {
      title = title.substring(0, MAX_TITLE_LENGTH - 1) + "\u2026";
    }
    if (title.length() > 0 && Character.isLowerCase(title.charAt(0))) {
      title = WordUtils.capitalize(title);
    }
    return title.trim();
  }

  public static String getTitle(Document document, SiteManifest site)
      throws RequiredFieldException {
    // First, see if the manifest tells us a specific place to check.
    if (site != null && site.getTitleSelectorCount() > 0) {
      Element titleEl = JsoupUtils.selectFirst(document, site.getTitleSelectorList());
      if (titleEl != null) {
        return cleanTitle(titleEl.text());
      }
    }

    // For most sites, we can get it from the meta keywords.  For
    // advice.careerbuilder.com, the meta keywords are crap, so we skip this
    // step.
    if (!document.baseUri().contains("//advice.careerbuilder.com/")
        && !document.baseUri().contains("//www.bhorowitz.com/")) {
      Element metaEl = JsoupUtils.selectFirst(document, ImmutableList.of(
          "html > head meta[name=\"fb_title\"]",
          "html > head meta[name=\"hdl\"]",
          "html > head meta[name=\"Headline\"]",
          "html > head meta[name=\"sailthru.title\"]",
          "html > head meta[property=\"og:title\"]",
          "html > head meta[property=\"rnews:headline\"]",
          "html > head meta[itemprop=\"alternativeHeadline\"]"));
      if (metaEl != null) {
        return cleanTitle(metaEl.attr("content"));
      }
    }

    // Failover to <title> tag.
    Element titleEl = document.select("title").first();
    if (titleEl != null) {
      return cleanTitle(StringHelper.unescape(titleEl.text()));
    }

    throw new RequiredFieldException("Could not find required field: title");
  }

  public static String getType(Document document) {
    Element metaEl = document.select("html > head meta[property=\"og:type\"]").first();
    return (metaEl != null) ? metaEl.attr("content") : null;
  }

  @VisibleForTesting
  static Set<String> getDedupingStems(String articleTitle) {
    Set<String> stems = Sets.newHashSet();
    for (String word : Splitter.on(WHITESPACE_PATTERN).split(articleTitle)) {
      String stemWord = KeywordUtils.cleanKeyword(word).toLowerCase();
      if (stemWord.length() >= 2 && !Vector.STOP_WORDS.contains(stemWord)) {
        stems.add(stemWord.length() > MAX_STEM_LENGTH
            ? stemWord.substring(0, MAX_STEM_LENGTH)
            : stemWord);
      }
    }
    return stems;
  }

  public static void main(String args[]) throws DatabaseSchemaException, DatabaseRequestException {
    Iterable<Article> articles = Database.with(Article.class).get();
    List<Article> articlesToUpdate = Lists.newArrayList();
    int i = 0;
    for (Article article : articles) {
      Database.with(Article.class).set(article, "deduping_stems", getDedupingStems(article.getTitle()));
      if (++i % 100 == 0) {
        System.out.println(i);
      }
    }
    Database.update(articlesToUpdate);
  }
}
