package com.janknspank.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.janknspank.classifier.DocumentVector;
import com.janknspank.proto.Core.Article;
import com.janknspank.proto.Core.WordArticleOccurrence;

public class WordArticleOccurrences {
  public static List<WordArticleOccurrence> getContainingWords(Iterable<String> words)
      throws DataInternalException {
    return Database.with(WordArticleOccurrence.class).get(
        new QueryOption.WhereEquals("word", words));
  }
  
  private static Map<String, ArrayList<String>> generateWordArticleOccurrencesFromCorpus() 
      throws DataInternalException {
    System.out.println("Warning: calling expensive function: generateWordArticleOccurrencesFromCorpus");
    Map<String, ArrayList<String>> wordArticleIndex = new HashMap<>();
//    Map<String, Integer> wordDocumentFrequency = new HashMap<>();
    int limit = 1000;
    int offset = 0;
    
    List<Article> articles;
    Set<String> words;
//    Integer frequency;
    ArrayList<String> occurrences;
    do {
      articles = Articles.getPageOfArticles(limit, offset);
      System.out.println("Generating Word Article Occurrences - article offest: " + offset);
      for (Article article : articles) {
        words = new DocumentVector(article).getWordsInDocument();
        for (String word : words) {
          // WordArticleOccurrence calculations
          occurrences = wordArticleIndex.get(word);
          if (occurrences == null) {
            occurrences = new ArrayList<String>();
          }
          occurrences.add(article.getUrlId());
          wordArticleIndex.put(word, occurrences);
          
          // WordDocumentFrequency calculations
//          frequency = wordDocumentFrequency.get(word);
//          if (frequency == null) {
//            wordDocumentFrequency.put(word, new Integer(1));
//          }
//          else {
//            wordDocumentFrequency.put(word, new Integer(frequency.intValue() + 1));
//          }
        }
      }
      offset += limit;
    //TODO uncomment:
    //} while (articles.size() == limit);
    } while (offset < 1000);
      
    return wordArticleIndex;
  }
  
  private static void saveWordArticleOccurrencesToServer(Map<String, ArrayList<String>> wordOccurrences) 
      throws DataInternalException {
    String word;
    ArrayList<String> occurrences;
    WordArticleOccurrence wordArticleOccurrence;
    for (Map.Entry<String, ArrayList<String>> wordOccurence : wordOccurrences.entrySet()) {
      word = wordOccurence.getKey();
      occurrences = wordOccurence.getValue();
      
      for (String occurrence : occurrences) {
        wordArticleOccurrence = WordArticleOccurrence.newBuilder()
            .setWord(word)
            .setUrlId(occurrence)
            .build();
        try {
          Database.insert(wordArticleOccurrence);
        } catch (ValidationException | DataInternalException e) {
          System.out.println("Error saving word article occurrence to server for word: " + word);
          System.out.println("Skipping it.");
          //throw new DataInternalException("Error creating document frequency ", e);
        }
      }

    }
  }
  
  public static void main(String args[]) throws Exception {
    try {
      Database.with(WordArticleOccurrence.class).createTable();      
    } catch (DataInternalException e) {
      System.out.println("WordArticleOccurrence table already exists. No need to create it.");
    }
    // TODO: figure out when is appropriate to trigger a regeneration
    // of this table
    Map<String, ArrayList<String>> wordOccurrences = generateWordArticleOccurrencesFromCorpus();
    System.out.println("wordOccurrences.size: " + wordOccurrences.size());
    saveWordArticleOccurrencesToServer(wordOccurrences);
  }
}