package com.janknspank.classifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.Iterables;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.ArticleProto.ArticleIndustry;
import com.janknspank.proto.ArticleProto.ArticleKeyword;

public class IndustryClassifierTest {
  private static final Article ARTICLE = Article.newBuilder()
      .setUrlId("54cf4a9dd4c6f1c39089d0d5")
      .setUrl("http://www.cbsnews.com/news/super-bowl-2015-loss-stuns-seattle-fans/")
      .setTitle("Super Bowl 2015: Loss stuns Seattle fans")
      .setDescription("Many question play call that led to crushing late interception, "
          + "but post-game street gatherings were peaceful")
      .setImageUrl("http://cbsnews2.cbsistatic.com/hub/i/r/2015/02/02/"
          + "fa753e25-6527-4154-bcc1-66389d50073a/thumbnail/620x350/"
          + "29973a53e170ace0795b1ac14f16126d/462644988.jpg")
      .addParagraph("Last Updated Feb 2, 2015 5:01 AM EST")
      .addParagraph("SEATTLE -- People in Seattle poised to celebrate a second straight "
          + "Super Bowl win by the Seahawks were instead left stunned.")
      .addParagraph("\"I'm sad,\" said Rebe Wolverton, who was part of a crowd watching "
          + "Sunday's game on large screen televisions outside a restaurant near Century "
          + "Link Field.")
      .addParagraph("A late interception preserved New England's 28-24 victory.")
      .addParagraph("\"This hurts,\" said Wolverton, who was wearing a Seahawks winter "
          + "cap and holding a bag of Skittles, the favorite candy of the team's star "
          + "running back, Marshawn Lynch, who's known as \"The Beast.\"")
      .addParagraph("New England Patriots overtake Seattle Seahawks to become Super Bowl "
          + "2015 champions")
      .addParagraph("Moments before the turnover, Seattleites were certain their "
          + "team would score with a run from the 1-yard-line. The boisterous crowd in "
          + "the Pioneer Square neighborhood near where the Seahawks play home games was "
          + "instead left shocked.")
      .addParagraph("In the city's University District, police officers kept watch on a "
          + "crowd of dozens of fans, some waving \"12th Man\" flags.")
      .addParagraph("People had poured onto the streets as soon as the game ended, reports "
          + "CBS Seattle affiliate KIRO-TV.")
      .addParagraph("At first, there were fears things could turn ugly.")
      .addParagraph("\"I think people are going to be very angry about this game, because "
          + "I think they thought we could have won this game,\" said Jack Reinard.")
      .addParagraph("But after about 20 minutes of standing around, confusion seemed to "
          + "set in, KIRO says. There was no rioting, nor burning couches, as there was "
          + "after the Seahawks won the Super Bowl last year. There was also a large police "
          + "presence that quickly surrounded the crowd.")
      .addParagraph("In the end, the crowd disbursed after about 90 minutes of waiting "
          + "and wondering.")
      .addParagraph("The one thing everyone seem sure about by the time they left: Lynch "
          + "should been give the ball with the game on the line.")
      .addParagraph("\"I think they should have let Beast Mode go Beast Mode, and should "
          + "have let him take it in,\" Dustin Curtin said. \"Maybe we'd be celebrating "
          + "right now, maybe a couch would be on fire.\"")
      .addParagraph("In north Seattle, 46-year-old George Bunting was also mystified that "
          + "Seahawks coach Pete Carroll would make the \"wrong decision\" and decide to "
          + "throw instead of having Lynch take the ball.")
      .addParagraph("\"This is a major upset. He should've just used the man,\" Bunting "
          + "said, referring to Lynch.")
      .addParagraph("Emily Simpson and Steven Baily were all ready for another celebration.")
      .addParagraph("\"This is heartbreaking,\" the 25-year-old Simpson said. \"I didn't hear "
          + "any fire arms or fireworks or anything. But it's just a game.\"")
      .addParagraph("Baily called the Seahawks play calling \"just insane.\"")
      .addParagraph("In Boston, fans celebrating the Patriots victory moved around the city, "
          + "including a stop at the Boston Marathon finish line, reports CBS Boston. Hundreds "
          + "of fans took to the streets following the win. Late in the game Boston police shut "
          + "down the area of Kenmore Square to prevent fans from congregating.")
      .addParagraph("Shortly after the final seconds ticked off the clock, fans began to stream "
          + "out of bars and went to areas such as Boston Common before reaching the finish line.")
      .addParagraph("Police cruisers stopped traffic near the finish line as the fans moved "
          + "through the area.")
      .addParagraph("Boston police, who urged residents in tweets to \"make our city proud,\" "
          + "reported that fans celebrated in a \"smart, responsible & respectful manner.\"")
      .addParagraph("After the victory, Boston Mayor Marty Walsh said the city is ready for a "
          + "parade, tweeting \"cue the duck boats.\"")
      .addParagraph("The top of the Prudential Center was lit up red, white and blue after the "
          + "game.")
      .setPublishedTime(1422876840000L)
      .setWordCount(577)
      .addKeyword(ArticleKeyword.newBuilder()
          .setKeyword("Boston Common")
          .setStrength(11)
          .setType("place")
          .setSource(ArticleKeyword.Source.NLP))
      .addKeyword(ArticleKeyword.newBuilder()
          .setKeyword("Rebe Wolverton")
          .setStrength(11)
          .setType("p")
          .setSource(ArticleKeyword.Source.NLP))
      .addKeyword(ArticleKeyword.newBuilder()
          .setKeyword("Beast Mode")
          .setStrength(10)
          .setType("org")
          .setSource(ArticleKeyword.Source.NLP))
      .addKeyword(ArticleKeyword.newBuilder()
          .setKeyword("Pioneer Square")
          .setStrength(3)
          .setType("place")
          .setSource(ArticleKeyword.Source.NLP))
      .addKeyword(ArticleKeyword.newBuilder()
          .setKeyword("CBS Boston")
          .setStrength(5)
          .setType("org")
          .setSource(ArticleKeyword.Source.NLP))
      .addKeyword(ArticleKeyword.newBuilder()
          .setKeyword("Marty Walsh")
          .setStrength(5)
          .setType("p")
          .setSource(ArticleKeyword.Source.NLP))
      .addKeyword(ArticleKeyword.newBuilder()
          .setKeyword("Emily Simpson")
          .setStrength(5)
          .setType("p")
          .setSource(ArticleKeyword.Source.NLP))
      .addKeyword(ArticleKeyword.newBuilder()
          .setKeyword("Marshawn Lynch")
          .setStrength(5)
          .setType("p")
          .setSource(ArticleKeyword.Source.NLP))
      .addKeyword(ArticleKeyword.newBuilder()
          .setKeyword("Jack Reinard")
          .setStrength(10)
          .setType("p")
          .setSource(ArticleKeyword.Source.NLP))
      .build();

  @Test
  public void test() throws Exception {
    // Mock an article and verify that all the classification scores are
    // reasonable.
    Iterable<ArticleIndustry> industries = IndustryClassifier.getInstance().classify(ARTICLE);
    for (ArticleIndustry industry : industries) {
      assertNotNull("Classifier found industry that doesn't exist: " + industry.getIndustryCodeId(),
          IndustryVector.get(industry.getIndustryCodeId()));
      assertTrue("For industry " + industry.getIndustryCodeId() + ", similarity should be in range "
          + "[0, 1].  Instead found: " + industry.getSimilarity(),
          industry.getSimilarity() >= 0 && industry.getSimilarity() <= 1);
    }

    // Make sure that .classify doesn't return all classifications, but returns
    // at least some.
    assertTrue("No industries found", !Iterables.isEmpty(industries));
    assertTrue("All industries should not be returned",
        IndustryClassifier.industryVectors.size() > Iterables.size(industries));
  }

}
