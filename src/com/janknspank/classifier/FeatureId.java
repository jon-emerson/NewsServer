package com.janknspank.classifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of all features we support.
 * @see Feature
 */
public enum FeatureId {
  INTERNET(10501, FeatureType.INDUSTRY, "Internet"),
  SOFTWARE(10502, FeatureType.INDUSTRY, "Software"),
  HARDWARE_AND_ELECTRONICS(10503, FeatureType.INDUSTRY, "Hardware and Electronics"),
  BIOTECHNOLOGY(10504, FeatureType.INDUSTRY, "Biotechnology"),
  VETERINARY(10505, FeatureType.INDUSTRY, "Veterinary"),
  APPAREL_AND_FASHION(10506, FeatureType.INDUSTRY, "Apparel and Fashion"),
  TOBACCO(10507, FeatureType.INDUSTRY, "Tobacco"),
  SUPERMARKETS(10508, FeatureType.INDUSTRY, "Supermarkets"),
  CONSUMER_ELECTRONICS(10509, FeatureType.INDUSTRY, "Consumer Electronics"),
  CONSUMER_GOODS(10510, FeatureType.INDUSTRY, "Consumer Goods"),
  LEISURE_TRAVEL_AND_TOURISM(10511, FeatureType.INDUSTRY, "Leisure, Travel and Tourism"),
  SPORTS(10512, FeatureType.INDUSTRY, "Sports"),
  VIDEO_PRODUCTION(10513, FeatureType.INDUSTRY, "Video Production"),
  MERGERS_AND_ACQUISITIONS(10514, FeatureType.INDUSTRY, "Mergers and Acquisitions"), ////////////
  ACCOUNTING(10515, FeatureType.INDUSTRY, "Accounting"),
  CONSTRUCTION(10516, FeatureType.INDUSTRY, "Construction"),
  ARCHITECTURE_AND_PLANNING(10517, FeatureType.INDUSTRY, "Architecture"),
  CIVIL_ENGINEERING(10518, FeatureType.INDUSTRY, "Civil Engineering"),
  AVIATION(10519, FeatureType.INDUSTRY, "Aviation"),
  AUTOMOTIVE(10520, FeatureType.INDUSTRY, "Automotive"),
  CHEMICALS(10521, FeatureType.INDUSTRY, "Chemistry"),
  DAIRY(10522, FeatureType.INDUSTRY, "Dairy"),
  MANAGEMENT(10523, FeatureType.INDUSTRY, "Management"),
  COMPUTER_GAMES(10524, FeatureType.INDUSTRY, "Computer Games"),
  ARTS(10525, FeatureType.INDUSTRY, "Arts"),
  NETWORK_SECURITY(10526, FeatureType.INDUSTRY, "Network Security"),
  ANIMATION(10527, FeatureType.INDUSTRY, "Animation"),
  PHOTOGRAPHY(10528, FeatureType.INDUSTRY, "Photography"),
  BUSINESS_SUPPLIES_AND_EQUIPMENT(10529, FeatureType.INDUSTRY, "Business Supplies and Equipment"),
  WINE_AND_SPIRITS(10530, FeatureType.INDUSTRY, "Wine and Spirits"),
  SPACE_EXPLORATION(10531, FeatureType.INDUSTRY, "Space Exploration"),
  LAW_PRACTICE(10532, FeatureType.INDUSTRY, "Law Practice"),
  MEDICINE(10533, FeatureType.INDUSTRY, "Medicine"),
  FOREIGN_EXCHANGE(10534, FeatureType.INDUSTRY, "Foreign Exchange"),
  EQUITY_INVESTING(10535, FeatureType.INDUSTRY, "Equity Investing"),
  REAL_ESTATE(10536, FeatureType.INDUSTRY, "Real Estate"),
  MINING_AND_METALS(10537, FeatureType.INDUSTRY, "Mining and Metals"),
  OIL_AND_ENERGY(10538, FeatureType.INDUSTRY, "Oil and Energy"),
  UTILITIES(10539, FeatureType.INDUSTRY, "Utilities"), 
  FARMING(10540, FeatureType.INDUSTRY, "Farming"),
  EDUCATION(10541, FeatureType.INDUSTRY, "Education"),
  MILITARY(10542, FeatureType.INDUSTRY, "Military"),
  GOVERNMENT(10543, FeatureType.INDUSTRY, "Government"),
  PUBLISHING(10544, FeatureType.INDUSTRY, "Publishing"),
  ENVIRONMENT(10545, FeatureType.INDUSTRY, "Environment"),
  SOCIAL_GOOD(10546, FeatureType.INDUSTRY, "Social Good"),
  EVENT_PLANNING(10548, FeatureType.INDUSTRY, "Event Planning"),
  MUSIC(10549, FeatureType.INDUSTRY, "Music"),
  HEALTH_AND_FITNESS(10552, FeatureType.INDUSTRY, "Health and Fitness"),
  HUMAN_RESOURCES(10554, FeatureType.INDUSTRY, "Human Resources"),
  RESTAURANTS(10556, FeatureType.INDUSTRY, "Restaurants"),
  MARKETING_AND_ADVERTISING(10557, FeatureType.INDUSTRY, "Marketing and Advertising"),
  VENTURE_CAPITAL(10558, FeatureType.INDUSTRY, "Venture Capital"),
  ELECTRICAL_ENGINEERING(10559, FeatureType.INDUSTRY, "Electrical Engineering"),
  USER_EXPERIENCE(10560, FeatureType.INDUSTRY, "User Experience"),
  ARCHAEOLOGY(10561, FeatureType.INDUSTRY, "Archaeology"),
  STARTUPS(20000, FeatureType.TOPIC, "Related to tech startups"),
  MANUAL_HEURISTIC_ACQUISITIONS(30000, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks for whether an article is about an acquisition"),
  MANUAL_HEURISTIC_LAUNCHES(30001, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks for whether an article is about a product launch"),
  MANUAL_HEURISTIC_FUNDRAISING(30002, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks for whether an article is about a funding round"),
  MANUAL_HEURISTIC_BIG_MONEY(30003, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks if an article has to do with some big sum of money"),
  MANUAL_HEURISTIC_QUARTERLY_EARNINGS(30004, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks if an article is about a quarterly earnings announcement"),
  MANUAL_HEURISTIC_IPO(30005, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks if an article has to do with an IPO"),
  MANUAL_HEURISTIC_S1_FILING(30006, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks if an article has to do with an S1 Filing"),
  MANUAL_HEURISTIC_BIG_SWING_IN_STOCK_PRICE(30007, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks if an article has to do with big stock price change"),
  MANUAL_HEURISTIC_IS_LIST(30008, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks for if an article is a buzz-feed like list of things"),
  TOPIC_SPORTS(40000, FeatureType.TOPIC,
      "Sports"),
  TOPIC_ENTERTAINMENT(40001, FeatureType.TOPIC,
      "Entertainment: Movies, music, TV shows, Broadway, etc."),
  TOPIC_POLITICS(40002, FeatureType.TOPIC,
      "Politics, both local, national and international."),
  TOPIC_MURDER_CRIME_WAR(40003, FeatureType.TOPIC,
      "Gross stuff: Crimes being committed, people hurt, people killed...");

  private final int id;
  private final FeatureType featureType;
  private final String title;

  private FeatureId(int id, FeatureType featureType, String title) {
    this.id = id;
    this.featureType = featureType;
    this.title = title;
  }

  public int getId() {
    return id;
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return id + ":" + featureType + ":" + title;
  }

  public static FeatureId fromId(int rawFeatureId) {
    for (FeatureId featureId : FeatureId.values()) {
      if (featureId.id == rawFeatureId) {
        return featureId;
      }
    }
    return null;
  }

  public static Iterable<FeatureId> getByType(FeatureType type) {
    List<FeatureId> features = new ArrayList<>();
    for (FeatureId featureId : FeatureId.values()) {
      if (featureId.getFeatureType() == type) {
        features.add(featureId);
      }
    }
    return features;
  }
}
