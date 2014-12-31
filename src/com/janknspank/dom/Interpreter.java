package com.janknspank.dom;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

/**
 * Built off the power of SiteParser, this class further interprets a web page
 * by taking the paragraphs and breaking them down into sentences, tokens, and
 * then into people, organizations, and locations.
 */
public class Interpreter {
  private static final SentenceDetectorME SENTENCE_DETECTOR_ME;
  private static final opennlp.tools.tokenize.Tokenizer TOKENIZER;
  private static final List<NameFinderME> PERSON_FINDER_LIST = Lists.newArrayList();
  private static final List<NameFinderME> ORGANIZATION_FINDER_LIST = Lists.newArrayList();
  private static final List<NameFinderME> LOCATION_FINDER_LIST = Lists.newArrayList();
  static {
    try {
      InputStream sentenceModelInputStream = new FileInputStream("opennlp/en-sent.bin");
      SentenceModel sentenceModel = new SentenceModel(sentenceModelInputStream);
      SENTENCE_DETECTOR_ME = new SentenceDetectorME(sentenceModel);

      InputStream tokenizerModelInputStream = new FileInputStream("opennlp/en-token.bin");
      TokenizerModel tokenizerModel = new TokenizerModel(tokenizerModelInputStream);
      TOKENIZER = new TokenizerME(tokenizerModel);

      for (String model : new String[] { "newsserver", "ner" }) {
        InputStream personModelInputStream =
            new FileInputStream("opennlp/en-" + model + "-person.bin");
        TokenNameFinderModel personModel = new TokenNameFinderModel(personModelInputStream);
        PERSON_FINDER_LIST.add(new NameFinderME(personModel));

        InputStream organizationModelInputStream =
            new FileInputStream("opennlp/en-" + model + "-organization.bin");
        TokenNameFinderModel organizationModel = new TokenNameFinderModel(organizationModelInputStream);
        ORGANIZATION_FINDER_LIST.add(new NameFinderME(organizationModel));

        InputStream locationModelInputStream =
            new FileInputStream("opennlp/en-" + model + "-location.bin");
        TokenNameFinderModel locationModel = new TokenNameFinderModel(locationModelInputStream);
        LOCATION_FINDER_LIST.add(new NameFinderME(locationModel));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String[] getSentences(String paragraph) {
    return SENTENCE_DETECTOR_ME.sentDetect(paragraph);
  }

  public static String[] getTokens(String sentence) {
    return TOKENIZER.tokenize(sentence);
  }

  private final InterpretedData interpretedData;

  public Interpreter(InputStream inputStream, String url) throws ParseException {
    this(Iterables.transform(
        new SiteParser().getParagraphNodes(inputStream, url), new Function<Node, String>() {
          @Override
          public String apply(Node paragraphNode) {
            return paragraphNode.getFlattenedText();
          }
        }));
  }

  public Interpreter(Iterable<String> paragraphs) {
    InterpretedData.Builder interpretedDataBuilder = new InterpretedData.Builder();
    interpretedDataBuilder.setArticleBody(Joiner.on("\n").join(paragraphs));
    for (String paragraph : paragraphs) {
      for (String sentence : SENTENCE_DETECTOR_ME.sentDetect(paragraph)) {
        String[] tokens = TOKENIZER.tokenize(sentence);
        parsePeople(interpretedDataBuilder, tokens);
        parseOrganizations(interpretedDataBuilder, tokens);
        parseLocations(interpretedDataBuilder, tokens);
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
    interpretedData = interpretedDataBuilder.build();
  }

  /**
   * Removes possessives and other dirt from strings our parser found (since we
   * trained our parser to include them, so instead of ignoring them as false
   * negatives).
   */
  private String cleanString(String dirtyString) {
    // Pre-process.
    if (dirtyString.startsWith("“") || dirtyString.startsWith("\"")) {
      dirtyString = dirtyString.substring("“".length());
    }
    if (dirtyString.endsWith("”") ||
        dirtyString.endsWith("\"") ||
        dirtyString.endsWith(",") ||
        dirtyString.endsWith(".")) {
      return dirtyString.substring(0, dirtyString.length() - "”".length());
    }

    // Post-process.
    if (dirtyString.endsWith("’s") || dirtyString.endsWith("'s")) {
      return dirtyString.substring(0, dirtyString.length() - "’s".length());
    }
    if (dirtyString.endsWith("’") || dirtyString.endsWith("'")) {
      return dirtyString.substring(0, dirtyString.length() - "’".length());
    }

    return dirtyString;
  }

  private void parsePeople(InterpretedData.Builder interpretedDataBuilder, String[] tokens) {
    for (NameFinderME personFinderMe : PERSON_FINDER_LIST) {
      Span personSpans[] = personFinderMe.find(tokens);
      for (String person : Span.spansToStrings(personSpans, tokens)) {
        interpretedDataBuilder.addPerson(cleanString(person));
      }
    }
  }

  private void parseOrganizations(InterpretedData.Builder interpretedDataBuilder, String[] tokens) {
    for (NameFinderME organizationFinderMe : ORGANIZATION_FINDER_LIST) {
      Span organizationSpans[] = organizationFinderMe.find(tokens);
      for (String organization : Span.spansToStrings(organizationSpans, tokens)) {
        interpretedDataBuilder.addOrganization(cleanString(organization));
      }
    }
  }

  private void parseLocations(InterpretedData.Builder interpretedDataBuilder, String[] tokens) {
    for (NameFinderME locationFinderMe : LOCATION_FINDER_LIST) {
      Span locationSpans[] = locationFinderMe.find(tokens);
      for (String location : Span.spansToStrings(locationSpans, tokens)) {
        interpretedDataBuilder.addLocation(cleanString(location));
      }
    }
  }

  public InterpretedData getInterpretedData() {
    return interpretedData;
  }
}
