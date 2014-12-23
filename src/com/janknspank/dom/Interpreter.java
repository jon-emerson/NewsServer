package com.janknspank.dom;

import java.io.FileInputStream;
import java.io.InputStream;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

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
  private static final NameFinderME NAME_FINDER_ME;
  private static final NameFinderME ORGANIZATION_FINDER_ME;
  private static final NameFinderME LOCATION_FINDER_ME;
  static {
    try {
      String modelType = "newsserver"; // Use "ner" for built-in models.

      InputStream sentenceModelInputStream = new FileInputStream("opennlp/en-sent.bin");
      SentenceModel sentenceModel = new SentenceModel(sentenceModelInputStream);
      SENTENCE_DETECTOR_ME = new SentenceDetectorME(sentenceModel);

      InputStream tokenizerModelInputStream = new FileInputStream("opennlp/en-token.bin");
      TokenizerModel tokenizerModel = new TokenizerModel(tokenizerModelInputStream);
      TOKENIZER = new TokenizerME(tokenizerModel);

      InputStream personModelInputStream =
          new FileInputStream("opennlp/en-" + modelType + "-person.bin");
      TokenNameFinderModel personModel = new TokenNameFinderModel(personModelInputStream);
      NAME_FINDER_ME = new NameFinderME(personModel);

      InputStream organizationModelInputStream =
          new FileInputStream("opennlp/en-" + modelType + "-organization.bin");
      TokenNameFinderModel organizationModel = new TokenNameFinderModel(organizationModelInputStream);
      ORGANIZATION_FINDER_ME = new NameFinderME(organizationModel);

      InputStream locationModelInputStream =
          new FileInputStream("opennlp/en-" + modelType + "-location.bin");
      TokenNameFinderModel locationModel = new TokenNameFinderModel(locationModelInputStream);
      LOCATION_FINDER_ME = new NameFinderME(locationModel);

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
        parseNames(interpretedDataBuilder, tokens);
        parseOrganizations(interpretedDataBuilder, tokens);
        parseLocations(interpretedDataBuilder, tokens);
      }
    }
    NAME_FINDER_ME.clearAdaptiveData();
    ORGANIZATION_FINDER_ME.clearAdaptiveData();
    LOCATION_FINDER_ME.clearAdaptiveData();
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
    if (dirtyString.endsWith("”") || dirtyString.endsWith("\"")) {
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

  private void parseNames(InterpretedData.Builder interpretedDataBuilder, String[] tokens) {
    Span nameSpans[] = NAME_FINDER_ME.find(tokens);
    for (String person : Span.spansToStrings(nameSpans, tokens)) {
      interpretedDataBuilder.addPerson(cleanString(person));
    }
  }

  private void parseOrganizations(InterpretedData.Builder interpretedDataBuilder, String[] tokens) {
    Span organizationSpans[] = ORGANIZATION_FINDER_ME.find(tokens);
    for (String organization : Span.spansToStrings(organizationSpans, tokens)) {
      interpretedDataBuilder.addOrganization(cleanString(organization));
    }
  }

  private void parseLocations(InterpretedData.Builder interpretedDataBuilder, String[] tokens) {
    Span locationSpans[] = LOCATION_FINDER_ME.find(tokens);
    for (String location : Span.spansToStrings(locationSpans, tokens)) {
      interpretedDataBuilder.addLocation(cleanString(location));
    }
  }

  public InterpretedData getInterpretedData() {
    return interpretedData;
  }
}
