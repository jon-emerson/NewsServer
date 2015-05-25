package com.janknspank.nlp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.google.common.base.Function;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import com.janknspank.bizness.EntityType;
import com.janknspank.common.StringHelper;
import com.janknspank.crawler.ParagraphFinder;
import com.janknspank.crawler.RequiredFieldException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Validator;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.ArticleProto.ArticleFeature;
import com.janknspank.proto.ArticleProto.ArticleKeyword;
import com.janknspank.proto.ArticleProto.ArticleKeyword.Source;

/**
 * Finds all the keywords in an article, by looking at meta tags, as well as
 * using natural language processing to find keywords through our trained data
 * sets.
 */
public class KeywordFinder {
  private final SentenceDetectorME SENTENCE_DETECTOR_ME;
  private final opennlp.tools.tokenize.Tokenizer TOKENIZER;
  private final List<NameFinderME> PERSON_FINDER_LIST = Lists.newArrayList();
  private final List<NameFinderME> ORGANIZATION_FINDER_LIST = Lists.newArrayList();
  private final List<NameFinderME> LOCATION_FINDER_LIST = Lists.newArrayList();

  private static KeywordFinder instance = null;

  private KeywordFinder() {
    try {
      @SuppressWarnings("resource")
      InputStream sentenceModelInputStream = new FileInputStream("opennlp/en-sent.bin");
      SentenceModel sentenceModel = new SentenceModel(sentenceModelInputStream);
      SENTENCE_DETECTOR_ME = new SentenceDetectorME(sentenceModel);

      @SuppressWarnings("resource")
      InputStream tokenizerModelInputStream = new FileInputStream("opennlp/en-token.bin");
      TokenizerModel tokenizerModel = new TokenizerModel(tokenizerModelInputStream);
      TOKENIZER = new TokenizerME(tokenizerModel);

      for (String model : new String[] { "newsserver", "ner" }) {
        @SuppressWarnings("resource")
        InputStream personModelInputStream =
            new FileInputStream("opennlp/en-" + model + "-person.bin");
        TokenNameFinderModel personModel = new TokenNameFinderModel(personModelInputStream);
        PERSON_FINDER_LIST.add(new NameFinderME(personModel));

        @SuppressWarnings("resource")
        InputStream organizationModelInputStream =
            new FileInputStream("opennlp/en-" + model + "-organization.bin");
        TokenNameFinderModel organizationModel = new TokenNameFinderModel(organizationModelInputStream);
        ORGANIZATION_FINDER_LIST.add(new NameFinderME(organizationModel));

        @SuppressWarnings("resource")
        InputStream locationModelInputStream =
            new FileInputStream("opennlp/en-" + model + "-location.bin");
        TokenNameFinderModel locationModel = new TokenNameFinderModel(locationModelInputStream);
        LOCATION_FINDER_LIST.add(new NameFinderME(locationModel));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a KeywordFinder singleton.
   */
  public static synchronized KeywordFinder getInstance() {
    if (instance == null) {
      instance = new KeywordFinder();
    }
    return instance;
  }

  /**
   * Top level method: Finds all the keywords in an article, whether they be
   * in the article body, meta tags, wherever!
   * @throws RequiredFieldException 
   */
  public Iterable<ArticleKeyword> findKeywords(
      String urlId,
      String title,
      DocumentNode documentNode,
      Iterable<ArticleFeature> articleFeatures)
      throws RequiredFieldException {
    List<ArticleKeyword> keywords = new ArrayList<ArticleKeyword>() {
      @Override
      public boolean add(ArticleKeyword keyword) {
        try {
          return super.add((ArticleKeyword) Validator.assertValid(keyword));
        } catch (DatabaseSchemaException | DatabaseRequestException e) {
          throw new IllegalArgumentException("Bad keyword: " + keyword.toString(), e);
        }
      }
    };
    Iterables.addAll(keywords, findKeywordsInMetaTags(urlId, documentNode));
    Iterables.addAll(keywords, findKeywordsFromHypertext(urlId, documentNode));
    Iterables.addAll(keywords, KeywordCanonicalizer.getArticleKeywordsFromText(title, 0));

    // Special handling for the first paragraph: Brute force search for
    // important keywords.
    Iterable<String> paragraphs = ParagraphFinder.getParagraphs(documentNode);
    if (!Iterables.isEmpty(paragraphs)) {
      Iterables.addAll(keywords, KeywordCanonicalizer.getArticleKeywordsFromText(
          Iterables.getFirst(paragraphs, null), 1));
    }

    // Use natural language processing to find more keywords.  Only look at the
    // top 2/3rds of the article, since sites tend to put click-bait words in
    // their articles towards the end.
    paragraphs = Iterables.limit(paragraphs,
        (int) Math.ceil(((double) Iterables.size(paragraphs)) * 2 / 3)); 
    Iterables.addAll(keywords, findParagraphKeywords(urlId, title, paragraphs));

    return KeywordCanonicalizer.canonicalize(keywords, articleFeatures);
  }

  private synchronized Iterable<ArticleKeyword> findParagraphKeywords(
      String urlId, String title, Iterable<String> paragraphs) {
    List<ArticleKeyword> keywords = Lists.newArrayList();

    // Go through each paragraph, find keywords with the natural language
    // processor, then insert them into keywords.  While doing so, remember
    // which paragraph we're looking at.
    // NOTE: The natural language processor is a piece of crap and often
    // misses keywords in one paragraph only to find them later.  Considering
    // paragraph number is very important, we need to remember where we've
    // seen various tokens so we can fix-up paragraph numbers once keywords
    // are finally detected.
    Map<String, Integer> tokenToFirstParagraph = Maps.newHashMap();
    int paragraphNumber = 0;
    for (String paragraph : Iterables.concat(ImmutableList.of(title + "."), paragraphs)) {
      for (String sentence : SENTENCE_DETECTOR_ME.sentDetect(paragraph)) {
        String[] tokens = TOKENIZER.tokenize(sentence);
        for (ArticleKeyword keyword : Iterables.concat(
            findPeople(urlId, tokens, paragraphNumber),
            findOrganizations(urlId, tokens, paragraphNumber),
            findLocations(urlId, tokens, paragraphNumber))) {
          if (tokenToFirstParagraph.containsKey(keyword.getKeyword().toLowerCase())) {
            keywords.add(keyword.toBuilder()
                .setParagraphNumber(tokenToFirstParagraph.get(keyword.getKeyword().toLowerCase()))
                .build());
          } else {
            // The NLP engine is also stupid about tokenizing company names with concatenated
            // words into multiple tokens.  E.g. LinkedIn -> linked in, HotWheels -> hot wheels.
            String whitespacelessKeyword = keyword.getKeyword().toLowerCase().replaceAll("(\\s|\u00A0)", "");
            if (tokenToFirstParagraph.containsKey(whitespacelessKeyword)) {
              keywords.add(keyword.toBuilder()
                  .setParagraphNumber(tokenToFirstParagraph.get(whitespacelessKeyword))
                  .build());
            } else {
              keywords.add(keyword);
            }
          }
        }
        for (String token : tokens) {
          if (!tokenToFirstParagraph.containsKey(token)) {
            tokenToFirstParagraph.put(token.toLowerCase(), paragraphNumber);
          }
        }
      }
      paragraphNumber++;
    }
    for (NameFinderME personFinderMe : PERSON_FINDER_LIST) {
      personFinderMe.clearAdaptiveData();
    }
    for (NameFinderME organizationFinderMe : ORGANIZATION_FINDER_LIST) {
      organizationFinderMe.clearAdaptiveData();
    }
    for (NameFinderME locationFinderMe : LOCATION_FINDER_LIST) {
      locationFinderMe.clearAdaptiveData();
    }

    return keywords;
  }

  private synchronized Iterable<ArticleKeyword> findKeywords(
      String urlId,
      String[] tokens,
      int paragraphNumber,
      List<NameFinderME> finders,
      EntityType type,
      int strengthMultiplier,
      int maxStrength) {
    Map<String, ArticleKeyword.Builder> keywordMap = Maps.newHashMap();
    for (NameFinderME nameFinderMe : finders) {
      Span keywordSpans[] = nameFinderMe.find(tokens);
      for (String keywordStr : Span.spansToStrings(keywordSpans, tokens)) {
        keywordStr = KeywordUtils.cleanKeyword(keywordStr);
        if (KeywordUtils.isValidKeyword(keywordStr)) {
          if (keywordMap.containsKey(keywordStr)) {
            keywordMap.get(keywordStr).setStrength(
                Math.min(maxStrength, keywordMap.get(keywordStr).getStrength() + strengthMultiplier));
          } else {
            keywordMap.put(keywordStr, ArticleKeyword.newBuilder()
                .setKeyword(keywordStr)
                .setStrength(Math.max(1, strengthMultiplier - paragraphNumber))
                .setType(type.toString())
                .setSource(Source.NLP)
                .setParagraphNumber(paragraphNumber));
          }
        }
      }
    }
    return Iterables.transform(keywordMap.values(),
        new Function<ArticleKeyword.Builder, ArticleKeyword>() {
          @Override
          public ArticleKeyword apply(ArticleKeyword.Builder builder) {
            return builder.build();
          }
        });
  }

  private synchronized Iterable<ArticleKeyword> findPeople(
      String urlId, String[] tokens, int paragraphNumber) {
    return findKeywords(
        urlId,
        tokens,
        paragraphNumber,
        PERSON_FINDER_LIST,
        EntityType.PERSON,
        5 /* strengthMultiplier */,
        20 /* maxStrength */);
  }

  private synchronized Iterable<ArticleKeyword> findOrganizations(
      String urlId, String[] tokens, int paragraphNumber) {
    return findKeywords(
        urlId,
        tokens,
        paragraphNumber,
        ORGANIZATION_FINDER_LIST,
        EntityType.ORGANIZATION,
        5 /* strengthMultiplier */,
        20 /* maxStrength */);
  }

  private synchronized Iterable<ArticleKeyword> findLocations(
      String urlId, String[] tokens, int paragraphNumber) {
    return findKeywords(
        urlId,
        tokens,
        paragraphNumber,
        LOCATION_FINDER_LIST,
        EntityType.PLACE,
        3 /* strengthMultiplier */,
        15 /* maxStrength */);
  }

  /**
   * Returns a LOWERCASED Set of all the words in the specified article.
   * Only words in the top 2/3rds of the article are used, because we're using
   * this Set only for meta keyword validation, and if a meta keyword doesn't
   * reference something that's the primary topic of an article, we don't need
   * it.
   * @throws RequiredFieldException 
   */
  private Set<String> getWordsInArticle(DocumentNode documentNode)
      throws RequiredFieldException {
    Iterable<Node> articleNodes = ParagraphFinder.getParagraphNodes(documentNode);
    articleNodes = Iterables.limit(articleNodes,
        (int) Math.ceil(((double) Iterables.size(articleNodes)) * 2 / 3)); 
    Set<String> wordsInArticle = Sets.newHashSet();
    for (Node paragraphNode : articleNodes) {
      for (String word : getTokens(paragraphNode.getFlattenedText())) {
        wordsInArticle.add(KeywordUtils.cleanKeyword(word).toLowerCase());
      }
    }
    return wordsInArticle;
  }

  /**
   * Returns true if any of the words in the passed keyword exist in the
   * article.
   */
  private boolean isMetaKeywordRelevant(Set<String> wordsInArticle, String keyword) {
    for (String keywordPart : getTokens(keyword)) {
      if (wordsInArticle.contains(KeywordUtils.cleanKeyword(keywordPart).toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  private Multiset<String> getKeywordsFromMetaContent(DocumentNode documentNode, Node metaNode)
      throws RequiredFieldException {
    Set<String> wordsInArticle = getWordsInArticle(documentNode);

    // Find the best delimiter to split on based on which is more prevalent.
    // E.g. some sites use semicolons, others use commas.
    String rawKeywords = StringHelper.unescape(metaNode.getAttributeValue("content"));
    String delimiter =
        (StringUtils.countMatches(rawKeywords, ",") > StringUtils.countMatches(rawKeywords, ";"))
            ? "," : ";";

    Multiset<String> keywords = HashMultiset.create();
    for (String keywordStr : rawKeywords.split(delimiter)) {
      keywordStr = KeywordUtils.cleanKeyword(keywordStr);
      if (KeywordUtils.isValidKeyword(keywordStr) &&
          isMetaKeywordRelevant(wordsInArticle, keywordStr)) {
        keywords.add(keywordStr);
      }
    }
    return keywords;
  }

  private Iterable<ArticleKeyword> findKeywordsInMetaTags(
      final String urlId, DocumentNode documentNode)
      throws RequiredFieldException {
    final Multiset<String> keywords = HashMultiset.create();
    for (Node metaNode : documentNode.findAll(ImmutableList.of(
        "html > head > meta[name=\"keywords\"]",
        "html > head > meta[name=\"news_keywords\"]",
        "html > head > meta[name=\"sailthru.tags\"]",
        "html > head > meta[property=\"article:tag\"]"))) {
      keywords.addAll(getKeywordsFromMetaContent(documentNode, metaNode));
    }
    return Iterables.transform(Multisets.copyHighestCountFirst(keywords).elementSet(),
        new Function<String, ArticleKeyword>() {
          @Override
          public ArticleKeyword apply(String keyword) {
            return ArticleKeyword.newBuilder()
              .setKeyword(keyword)
              .setStrength(Math.min(3, keywords.count(keyword)))
              .setSource(Source.META_TAG)
              .setType(EntityType.THING.toString())
              .build();
          }
        });
  }

  /**
   * Uses the structure of the hypertext to try to figure out proper nouns and
   * other keywords in the article's paragraphs.  E.g. often times sites link
   * people names and companies to other web pages - use this to find entities!
   * @throws RequiredFieldException 
   */
  private static Iterable<ArticleKeyword> findKeywordsFromHypertext(
      final String urlId, DocumentNode documentNode) throws RequiredFieldException {
    // Find all the Nodes inside paragraphs that do not have any children.
    // E.g. if we had <p><a href="#">Michael Douglass</a> is awesome</p>,
    // this method would return the <a> node only.
    List<Node> childlessChildNodes = Lists.newArrayList();
    Map<Node, Integer> nodeParagraphNumberMap = Maps.newHashMap();
    int paragraphNumber = 0;
    List<Node> nodes = ParagraphFinder.getParagraphNodes(documentNode);
    for (Node paragraphNode : nodes) {
      paragraphNumber++;
      for (Node node : paragraphNode.findAll("*")) {
        if (!node.hasChildNodes()) {
          childlessChildNodes.add(node);
          nodeParagraphNumberMap.put(node, paragraphNumber);
        }
      }
    }

    // Find text that looks like keywords in all the childless nodes.
    Map<String, Integer> keywords = Maps.newHashMap();
    for (Node childlessChildNode : childlessChildNodes) {
      String possibleKeyword = childlessChildNode.getFlattenedText();

      // This text is capitalized like a proper noun, and has 5 or fewer
      // words in it - let's consider it a keyword!
      if (KeywordUtils.isValidKeyword(possibleKeyword) &&
          possibleKeyword.equals(WordUtils.capitalizeFully(possibleKeyword)) &&
          !possibleKeyword.equals(possibleKeyword.toUpperCase()) &&
          StringUtils.countMatches(possibleKeyword, " ") < 5) {
        keywords.put(possibleKeyword, nodeParagraphNumberMap.get(childlessChildNode));
      }
    }

    // Create a bunch of objects for the keywords we found.
    return Iterables.transform(keywords.entrySet(),
        new Function<Map.Entry<String, Integer>, ArticleKeyword>() {
          @Override
          public ArticleKeyword apply(Map.Entry<String, Integer> entry) {
            return ArticleKeyword.newBuilder()
                .setKeyword(entry.getKey())
                .setStrength(1)
                .setSource(Source.HYPERLINK)
                .setType(EntityType.THING.toString())
                .setParagraphNumber(entry.getValue())
                .build();
          }
        });
  }

  public synchronized String[] getSentences(String paragraph) {
    return SENTENCE_DETECTOR_ME.sentDetect(paragraph);
  }

  public synchronized String[] getTokens(String sentence) {
    return TOKENIZER.tokenize(sentence);
  }
}
