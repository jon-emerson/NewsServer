package com.janknspank.dom;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class Tokenizer {
  private static final SentenceDetectorME sentenceDetector;
  private static final opennlp.tools.tokenize.Tokenizer tokenizer;
  private static final NameFinderME nameFinder;
  private static final NameFinderME organizationFinder;
  private static final NameFinderME locationFinder;
  static {
    try {
      String modelType = "newsserver"; // Use "ner" for built-in models.

      InputStream sentenceModelInputStream = new FileInputStream("opennlp/en-sent.bin");
      SentenceModel sentenceModel = new SentenceModel(sentenceModelInputStream);
      sentenceDetector = new SentenceDetectorME(sentenceModel);

      InputStream tokenizerModelInputStream = new FileInputStream("opennlp/en-token.bin");
      TokenizerModel tokenizerModel = new TokenizerModel(tokenizerModelInputStream);
      tokenizer = new TokenizerME(tokenizerModel);

      InputStream personModelInputStream =
          new FileInputStream("opennlp/en-" + modelType + "-person.bin");
      TokenNameFinderModel personModel = new TokenNameFinderModel(personModelInputStream);
      nameFinder = new NameFinderME(personModel);

      InputStream organizationModelInputStream =
          new FileInputStream("opennlp/en-" + modelType + "-organization.bin");
      TokenNameFinderModel organizationModel = new TokenNameFinderModel(organizationModelInputStream);
      organizationFinder = new NameFinderME(organizationModel);

      InputStream locationModelInputStream =
          new FileInputStream("opennlp/en-" + modelType + "-location.bin");
      TokenNameFinderModel locationModel = new TokenNameFinderModel(locationModelInputStream);
      locationFinder = new NameFinderME(locationModel);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String[] getSentences(String paragraph) {
    return sentenceDetector.sentDetect(paragraph);
  }

  public static String[] getTokens(String sentence) {
    return tokenizer.tokenize(sentence);
  }

  public static String[] getNames(String paragraph) {
    List<String> names = new ArrayList<String>();
    for (String sentence : getSentences(paragraph)) {
      String[] tokens = getTokens(sentence);
      Span nameSpans[] = nameFinder.find(tokens);
      for (String s : Span.spansToStrings(nameSpans, tokens)) {
        names.add(s);
      }
    }
    return names.toArray(new String[0]);

    // TODO(jonemerson): We need to do this after each document!!
    // nameFinder.clearAdaptiveData();
  }

  public static String[] getOrganizations(String paragraph) {
    List<String> organizations = new ArrayList<String>();
    for (String sentence : getSentences(paragraph)) {
      String[] tokens = getTokens(sentence);
      Span organizationSpans[] = organizationFinder.find(tokens);
      for (String s : Span.spansToStrings(organizationSpans, tokens)) {
        organizations.add(s);
      }
    }
    return organizations.toArray(new String[0]);

    // TODO(jonemerson): We need to do this after each document!!
    // nameFinder.clearAdaptiveData();
  }

  public static String[] getLocations(String paragraph) {
    List<String> locations = new ArrayList<String>();
    for (String sentence : getSentences(paragraph)) {
      String[] tokens = getTokens(sentence);
      Span locationSpans[] = locationFinder.find(tokens);
      for (String s : Span.spansToStrings(locationSpans, tokens)) {
        locations.add(s);
      }
    }
    return locations.toArray(new String[0]);

    // TODO(jonemerson): We need to do this after each document!!
    // nameFinder.clearAdaptiveData();
  }
}
