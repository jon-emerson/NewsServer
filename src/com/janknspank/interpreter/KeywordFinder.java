package com.janknspank.interpreter;

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
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import com.janknspank.bizness.EntityType;
import com.janknspank.common.AssertionException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Validator;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.Node;
import com.janknspank.proto.Core.ArticleKeyword;
import com.janknspank.proto.Core.ArticleKeyword.Source;

/**
 * Finds all the keywords in an article, by looking at meta tags, as well as
 * using natural language processing to find keywords through our trained data
 * sets.
 */
public class KeywordFinder {
  private static final SentenceDetectorME SENTENCE_DETECTOR_ME;
  private static final opennlp.tools.tokenize.Tokenizer TOKENIZER;
  private static final List<NameFinderME> PERSON_FINDER_LIST = Lists.newArrayList();
  private static final List<NameFinderME> ORGANIZATION_FINDER_LIST = Lists.newArrayList();
  private static final List<NameFinderME> LOCATION_FINDER_LIST = Lists.newArrayList();
  static {
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

  private KeywordFinder() {}

  /**
   * Top level method: Finds all the keywords in an article, whether they be
   * in the article body, meta tags, wherever!
   */
  public static Iterable<ArticleKeyword> findKeywords(String urlId, DocumentNode documentNode) {
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
    List<Node> articleNodes = SiteParser.getParagraphNodes(documentNode);
    Iterables.addAll(keywords, findKeywordsInMetaTags(urlId, documentNode));
    Iterables.addAll(keywords, findKeywordsFromHypertext(urlId, documentNode));
    Iterables.addAll(keywords, findParagraphKeywords(urlId, Iterables.transform(articleNodes,
        new Function<Node, String>() {
          @Override
          public String apply(Node articleNode) {
            return articleNode.getFlattenedText();
          }
        })));
    return KeywordCanonicalizer.canonicalize(keywords);
  }

  public static Iterable<ArticleKeyword> findParagraphKeywords(
      String urlId, Iterable<String> paragraphs) {
    List<ArticleKeyword> keywords = Lists.newArrayList();

    for (String paragraph : paragraphs) {
      for (String sentence : SENTENCE_DETECTOR_ME.sentDetect(paragraph)) {
        String[] tokens = TOKENIZER.tokenize(sentence);
        Iterables.addAll(keywords, findPeople(urlId, tokens));
        Iterables.addAll(keywords, findOrganizations(urlId, tokens));
        Iterables.addAll(keywords, findLocations(urlId, tokens));
      }
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

    return KeywordCanonicalizer.canonicalize(keywords);
  }

  private static Iterable<ArticleKeyword> findKeywords(
      String urlId,
      String[] tokens,
      List<NameFinderME> finders,
      EntityType type,
      int strengthMultiplier,
      int maxStrength) {
    Map<String, ArticleKeyword.Builder> keywordMap = Maps.newHashMap();
    for (NameFinderME nameFinderMe : finders) {
      Span keywordSpans[] = nameFinderMe.find(tokens);
      for (String keywordStr : Span.spansToStrings(keywordSpans, tokens)) {
        keywordStr = KeywordUtils.cleanKeyword(keywordStr);
        try {
          if (KeywordUtils.isValidKeyword(keywordStr)) {
            if (keywordMap.containsKey(keywordStr)) {
              keywordMap.get(keywordStr).setStrength(
                  Math.min(maxStrength, keywordMap.get(keywordStr).getStrength() + strengthMultiplier));
            } else {
              keywordMap.put(keywordStr, ArticleKeyword.newBuilder()
                  .setUrlId(urlId)
                  .setKeyword(keywordStr)
                  .setStrength(strengthMultiplier)
                  .setType(type.toString())
                  .setSource(Source.NLP));
            }
          }
        } catch (AssertionException e) {
          e.printStackTrace();
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

  private static Iterable<ArticleKeyword> findPeople(String urlId, String[] tokens) {
    return findKeywords(
        urlId,
        tokens,
        PERSON_FINDER_LIST,
        EntityType.PERSON,
        5 /* strengthMultiplier */,
        20 /* maxStrength */);
  }

  private static Iterable<ArticleKeyword> findOrganizations(String urlId, String[] tokens) {
    return findKeywords(
        urlId,
        tokens,
        ORGANIZATION_FINDER_LIST,
        EntityType.ORGANIZATION,
        5 /* strengthMultiplier */,
        20 /* maxStrength */);
  }

  private static Iterable<ArticleKeyword> findLocations(String urlId, String[] tokens) {
    return findKeywords(
        urlId,
        tokens,
        LOCATION_FINDER_LIST,
        EntityType.PLACE,
        3 /* strengthMultiplier */,
        15 /* maxStrength */);
  }

  /**
   * Returns a LOWERCASED Set of all the words in the specified article.
   */
  private static Set<String> getWordsInArticle(DocumentNode documentNode) {
    Set<String> wordsInArticle = Sets.newHashSet();
    for (Node paragraphNode : SiteParser.getParagraphNodes(documentNode)) {
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
  private static boolean isMetaKeywordRelevant(Set<String> wordsInArticle, String keyword) {
    for (String keywordPart : getTokens(keyword)) {
      if (wordsInArticle.contains(KeywordUtils.cleanKeyword(keywordPart).toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  private static Multiset<String> getKeywordsFromMetaContent(
      DocumentNode documentNode, Node metaNode) {
    Set<String> wordsInArticle = getWordsInArticle(documentNode);

    // Find the best delimiter to split on based on which is more prevalent.
    // E.g. some sites use semicolons, others use commas.
    String rawKeywords = ArticleCreator.unescape(metaNode.getAttributeValue("content"));
    String delimiter =
        (StringUtils.countMatches(rawKeywords, ",") > StringUtils.countMatches(rawKeywords, ";"))
            ? "," : ";";

    Multiset<String> keywords = HashMultiset.create();
    for (String keywordStr : rawKeywords.split(delimiter)) {
      keywordStr = KeywordUtils.cleanKeyword(keywordStr);
      try {
        if (KeywordUtils.isValidKeyword(keywordStr) &&
            isMetaKeywordRelevant(wordsInArticle, keywordStr)) {
          keywords.add(keywordStr);
        }
      } catch (AssertionException e) {
        e.printStackTrace();
      }
    }
    return keywords;
  }

  private static Iterable<ArticleKeyword> findKeywordsInMetaTags(
      final String urlId, DocumentNode documentNode) {
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
              .setUrlId(urlId)
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
   */
  private static Iterable<ArticleKeyword> findKeywordsFromHypertext(
      final String urlId, DocumentNode documentNode) {
    // Find all the Nodes inside paragraphs that do not have any children.
    // E.g. if we had <p><a href="#">Michael Douglass</a> is awesome</p>,
    // this method would return the <a> node only.
    Iterable<Node> childlessChildNodes = Iterables.concat(
        Iterables.transform(
            SiteParser.getParagraphNodes(documentNode),
            new Function<Node, Iterable<Node>>() {
              @Override
              public Iterable<Node> apply(Node paragraph) {
                return Iterables.filter(paragraph.findAll("*"),
                    new Predicate<Node>() {
                      @Override
                      public boolean apply(Node child) {
                        return !child.hasChildNodes();
                      }
                    });
              }
            }));

    // Find text that looks like keywords in all the childless nodes.
    Set<String> keywords = Sets.newHashSet();
    for (Node childlessChildNode : childlessChildNodes) {
      String possibleKeyword = childlessChildNode.getFlattenedText();

      // This text is capitalized like a proper noun, and has 5 or fewer
      // words in it - let's consider it a keyword!
      if (possibleKeyword.equals(WordUtils.capitalizeFully(possibleKeyword)) &&
          !possibleKeyword.equals(possibleKeyword.toUpperCase()) &&
          StringUtils.countMatches(possibleKeyword, " ") < 5) {
        keywords.add(possibleKeyword);
      }
    }

    // Create a bunch of objects for the keywords we found.
    return Iterables.transform(keywords,
        new Function<String, ArticleKeyword>() {
          @Override
          public ArticleKeyword apply(String keyword) {
            return ArticleKeyword.newBuilder()
              .setUrlId(urlId)
              .setKeyword(keyword)
              .setStrength(4)
              .setSource(Source.HYPERLINK)
              .setType(EntityType.THING.toString())
              .build();
          }
        });
  }

  public static String[] getSentences(String paragraph) {
    return SENTENCE_DETECTOR_ME.sentDetect(paragraph);
  }

  public static String[] getTokens(String sentence) {
    return TOKENIZER.tokenize(sentence);
  }
}
