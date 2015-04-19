package com.janknspank.bizness;

import java.util.Arrays;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.janknspank.classifier.FeatureId;

/**
 * Canonicalization of various business industries.  Used for user and
 * article classifications.
 */
public enum Industry {
  DEFENSE_AND_SPACE(1, FeatureId.SPACE_EXPLORATION, "gov tech",
      "Defense & Space"),
  COMPUTER_HARDWARE(3, FeatureId.HARDWARE_AND_ELECTRONICS, "tech",
      "Computer Hardware"),
  COMPUTER_SOFTWARE(4, FeatureId.SOFTWARE, "tech",
      "Computer Software"),
  COMPUTER_NETWORKING(5, FeatureId.INTERNET, "tech",
      "Computer Networking"),
  INTERNET(6, FeatureId.INTERNET, "tech",
      "Internet"),
  SEMICONDUCTORS(7, FeatureId.HARDWARE_AND_ELECTRONICS, "tech",
      "Semiconductors"),
  TELECOMMUNICATIONS(8, FeatureId.INTERNET, "gov tech",
      "Telecommunications"),
  LAW_PRACTICE(9, FeatureId.LAW_PRACTICE, "leg",
      "Law Practice"),
  LEGAL_SERVICES(10, FeatureId.LAW_PRACTICE, "leg",
      "Legal Services"),
  MANAGEMENT_CONSULTING(11, FeatureId.MANAGEMENT, "corp",
      "Management Consulting"),
  BIOTECHNOLOGY(12, FeatureId.BIOTECHNOLOGY, "gov hlth tech",
      "Biotechnology"),
  MEDICAL_PRACTICE(13, FeatureId.MEDICINE, "hlth",
      "Medical Practice"),
  HOSPITAL_AND_HEALTH_CARE(14, FeatureId.MEDICINE, "hlth",
      "Hospital & Health Care"),
  PHARMACEUTICALS(15, FeatureId.BIOTECHNOLOGY, "hlth tech",
      "Pharmaceuticals"),
  VETERINARY(16, FeatureId.VETERINARY, "hlth",
      "Veterinary"),
  MEDICAL_DEVICES(17, FeatureId.MEDICINE, "hlth",
      "Medical Devices"),
  COSMETICS(18, FeatureId.CONSUMER_GOODS, "good",
      "Cosmetics"),
  APPAREL_AND_FASHION(19, FeatureId.APPAREL_AND_FASHION, "good",
      "Apparel & Fashion"),
  SPORTING_GOODS(20, FeatureId.APPAREL_AND_FASHION, "good rec",
      "Sporting Goods"),
  TOBACCO(21, FeatureId.TOBACCO, "good",
      "Tobacco"),
  SUPERMARKETS(22, FeatureId.SUPERMARKETS, "good",
      "Supermarkets"),
  FOOD_PRODUCTION(23, FeatureId.FARMING, "good man serv",
      "Food Production"),
  CONSUMER_ELECTRONICS(24, FeatureId.CONSUMER_ELECTRONICS, "good man",
      "Consumer Electronics"),
  CONSUMER_GOODS(25, FeatureId.CONSUMER_GOODS, "good man",
      "Consumer Goods"),
  FURNITURE(26, FeatureId.CONSUMER_GOODS, "good man",
      "Furniture"),
  RETAIL(27, FeatureId.CONSUMER_GOODS, "good man",
      "Retail"),
  ENTERTAINMENT(28, FeatureId.LEISURE_TRAVEL_AND_TOURISM, "med rec",
      "Entertainment"),
  GAMBLING_AND_CASINOS(29, FeatureId.LEISURE_TRAVEL_AND_TOURISM, "rec",
      "Gambling & Casinos"),
  LEISURE_TRAVEL_AND_TOURISM(30, FeatureId.LEISURE_TRAVEL_AND_TOURISM, "rec serv tran",
      "Leisure, Travel & Tourism"),
  HOSPITALITY(31, FeatureId.LEISURE_TRAVEL_AND_TOURISM, "rec serv tran",
      "Hospitality"),
  RESTAURANTS(32, FeatureId.RESTAURANTS, "rec serv",
      "Restaurants"),
  SPORTS(33, FeatureId.SPORTS, "rec",
      "Sports"),
  FOOD_AND_BEVERAGES(34, FeatureId.RESTAURANTS, "rec serv",
      "Food & Beverages"),
  MOTION_PICTURES_AND_FILM(35, FeatureId.VIDEO_PRODUCTION, "art med rec",
      "Motion Pictures and Film"),
  BROADCAST_MEDIA(36, FeatureId.VIDEO_PRODUCTION, "med rec",
      "Broadcast Media"),
  MUSEUMS_AND_INSTITUTIONS(37, FeatureId.ARTS,
      "art med rec",
      "Museums and Institutions"),
  FINE_ART(38, FeatureId.ARTS, "art med rec",
      "Fine Art"),
  PERFORMING_ARTS(39, FeatureId.ARTS, "art med rec",
      "Performing Arts"),
  RECREATIONAL_FACILITIES_AND_SERVICES(40, FeatureId.HEALTH_AND_FITNESS,
      "rec serv", "Recreational Facilities and Services"),
  BANKING(41, FeatureId.EQUITY_INVESTING, "fin",
      "Banking"),
  INSURANCE(42, FeatureId.EQUITY_INVESTING, "fin",
      "Insurance"),
  FINANCIAL_SERVICES(43, FeatureId.EQUITY_INVESTING, "fin",
      "Financial Services"),
  REAL_ESTATE(44, FeatureId.REAL_ESTATE, "cons fin good",
      "Real Estate"),
  INVESTMENT_BANKING(45, FeatureId.EQUITY_INVESTING, "fin",
      "Investment Banking"),
  INVESTMENT_MANAGEMENT(46, FeatureId.EQUITY_INVESTING, "fin",
      "Investment Management"),
  ACCOUNTING(47, FeatureId.ACCOUNTING, "corp fin",
      "Accounting"),
  CONSTRUCTION(48, FeatureId.CONSTRUCTION, "cons",
      "Construction"),
  BUILDING_MATERIALS(49, FeatureId.CONSTRUCTION, "cons",
      "Building Materials"),
  ARCHITECTURE_AND_PLANNING(50, FeatureId.ARCHITECTURE_AND_PLANNING, "cons",
      "Architecture & Planning"),
  CIVIL_ENGINEERING(51, FeatureId.CIVIL_ENGINEERING, "cons gov",
      "Civil Engineering"),
  AVIATION_AND_AEROSPACE(52, FeatureId.AVIATION, "gov man",
      "Aviation & Aerospace"),
  AUTOMOTIVE(53, FeatureId.AUTOMOTIVE, "man",
      "Automotive"),
  CHEMICALS(54, FeatureId.CHEMICALS, "man",
      "Chemicals"),
  MACHINERY(55, FeatureId.MECHANICAL_AND_INDUSTRIAL_ENGINEERING, "man",
      "Machinery"),
  MINING_AND_METALS(56, FeatureId.MINING_AND_METALS, "man",
      "Mining & Metals"),
  OIL_AND_ENERGY(57, FeatureId.OIL_AND_ENERGY, "man",
      "Oil & Energy"),
  SHIPBUILDING(58, FeatureId.CONSTRUCTION, "man",
      "Shipbuilding"),
  UTILITIES(59, FeatureId.UTILITIES, "man",
      "Utilities"),
  TEXTILES(60, FeatureId.CONSUMER_GOODS, "man",
      "Textiles"),
  PAPER_AND_FOREST_PRODUCTS(61, FeatureId.CONSUMER_GOODS, "man",
      "Paper & Forest Products"),
  RAILROAD_MANUFACTURE(62, FeatureId.CONSTRUCTION, "man",
      "Railroad Manufacture"),
  FARMING(63, FeatureId.FARMING, "agr",
      "Farming"),
  RANCHING(64, FeatureId.FARMING, "agr",
      "Ranching"),
  DAIRY(65, FeatureId.DAIRY, "agr",
      "Dairy"),
  FISHERY(66, FeatureId.FARMING, "agr",
      "Fishery"),
  PRIMARY_SECONDARY_EDUCATION(67, FeatureId.EDUCATION, "edu",
      "Primary/Secondary Education"),
  HIGHER_EDUCATION(68, FeatureId.EDUCATION, "edu",
      "Higher Education"),
  EDUCATION_MANAGEMENT(69, FeatureId.EDUCATION, "edu",
      "Education Management"),
  RESEARCH(70, FeatureId.EDUCATION, "edu gov",
      "Research"),
  MILITARY(71, FeatureId.MILITARY, "gov",
      "Military"),
  LEGISLATIVE_OFFICE(72, FeatureId.GOVERNMENT, "gov leg",
      "Legislative Office"),
  JUDICIARY(73, FeatureId.GOVERNMENT, "gov leg",
      "Judiciary"),
  INTERNATIONAL_AFFAIRS(74, FeatureId.GOVERNMENT, "gov",
      "International Affairs"),
  GOVERNMENT_ADMINISTRATION(75, FeatureId.GOVERNMENT, "gov",
      "Government Administration"),
  EXECUTIVE_OFFICE(76, FeatureId.GOVERNMENT, "gov",
      "Executive Office"),
  LAW_ENFORCEMENT(77, FeatureId.SOCIAL_GOOD, "gov leg",
      "Law Enforcement"),
  PUBLIC_SAFETY(78, FeatureId.SOCIAL_GOOD, "gov",
      "Public Safety"),
  PUBLIC_POLICY(79, FeatureId.SOCIAL_GOOD, "gov",
      "Public Policy"),
  MARKETING_AND_ADVERTISING(80, FeatureId.MARKETING_AND_ADVERTISING, "corp med",
      "Marketing and Advertising"),
  NEWSPAPERS(81, FeatureId.PUBLISHING, "med rec",
      "Newspapers"),
  PUBLISHING(82, FeatureId.PUBLISHING, "med rec",
      "Publishing"),
  PRINTING(83, FeatureId.PUBLISHING, "med rec",
      "Printing"),
  INFORMATION_SERVICES(84, FeatureId.PUBLISHING, "med serv",
      "Information Services"),
  LIBRARIES(85, FeatureId.PUBLISHING, "med rec serv",
      "Libraries"),
  ENVIRONMENTAL_SERVICES(86, FeatureId.ENVIRONMENT, "org serv",
      "Environmental Services"),
  PACKAGE_FREIGHT_DELIVERY(87, FeatureId.LOGISTICS, "serv tran",
      "Package/Freight Delivery"),
  INDIVIDUAL_AND_FAMILY_SERVICES(88, FeatureId.SOCIAL_GOOD, "org serv",
      "Individual & Family Services"),
  RELIGIOUS_INSTITUTIONS(89, FeatureId.SOCIAL_GOOD, "org serv",
      "Religious Institutions"),
  CIVIC_AND_SOCIAL_ORGANIZATION(90, FeatureId.SOCIAL_GOOD, "org serv",
      "Civic & Social Organization"),
  CONSUMER_SERVICES(91, FeatureId.CONSUMER_GOODS, "org serv",
      "Consumer Services"),
  TRANSPORTATION_TRUCKING_RAILROAD(92, FeatureId.LOGISTICS, "tran",
      "Transportation/Trucking/Railroad"),
  WAREHOUSING(93, FeatureId.LOGISTICS, "tran",
      "Warehousing"),
  AIRLINES_AVIATION(94, FeatureId.AVIATION, "man tech tran",
      "Airlines/Aviation"),
  MARITIME(95, FeatureId.LOGISTICS, "tran",
      "Maritime"),
  INFORMATION_TECHNOLOGY_AND_SCIENCE(96, FeatureId.INTERNET,
      "tech", "Information Technology and Services"),
  MARKET_RESEARCH(97, FeatureId.CONSUMER_GOODS, "corp",
      "Market Research"),
  PUBLIC_RELATIONS_AND_COMMUNICATIONS(98, FeatureId.PUBLISHING, "corp",
      "Public Relations and Communications"),
  DESIGN(99, FeatureId.ARTS, "art med",
      "Design"),
  NON_PROFIT_ORGANIZATION_MANAGEMENT(100, FeatureId.SOCIAL_GOOD, "org",
      "Non-Profit Organization Management"),
  FUND_RAISING(101, FeatureId.SOCIAL_GOOD, "org",
      "Fund-Raising"),
  PROGRAM_DEVELOPMENT(102, FeatureId.MANAGEMENT, "corp org",
      "Program Development"),
  WRITING_AND_EDITING(103, FeatureId.PUBLISHING, "art med rec",
      "Writing and Editing"),
  STAFFING_AND_RECRUITING(104, FeatureId.STAFFING_AND_RECRUITING, "corp",
      "Staffing and Recruiting"),
  PROFESSIONAL_TRAINING_AND_COACHING(105, FeatureId.MANAGEMENT, "corp",
      "Professional Training & Coaching"),
  VENTURE_CAPITAL(106, FeatureId.VENTURE_CAPITAL, "fin tech",
      "Venture Capital"),
  POLITICAL_ORGANIZATION(107, FeatureId.GOVERNMENT, "gov org",
      "Political Organization"),
  TRANSLATION_AND_LOCALIZATION(108, FeatureId.PUBLISHING, "corp gov serv",
      "Translation and Localization"),
  COMPUTER_GAMES(109, FeatureId.COMPUTER_GAMES, "med rec",
      "Computer Games"),
  EVENTS_SERVICES(110, FeatureId.EVENT_PLANNING, "corp rec serv",
      "Events Services"),
  ARTS_AND_CRAFTS(111, FeatureId.ARTS, "art med rec",
      "Arts and Crafts"),
  ELECTRICAL_ELECTRONIC_MANUFACTURING(112, FeatureId.ELECTRICAL_ENGINEERING,
      "good man", "Electrical/Electronic Manufacturing"),
  ONLINE_MEDIA(113, FeatureId.INTERNET, "med",
      "Online Media"),
  NANOTECHNOLOGY(114, FeatureId.HARDWARE_AND_ELECTRONICS, "gov man tech",
      "Nanotechnology"),
  MUSIC(115, FeatureId.MUSIC, "art rec",
      "Music"),
  LOGISTICS_AND_SUPPLY_CHAIN(116, FeatureId.LOGISTICS, "corp tran",
      "Logistics and Supply Chain"),
  PLASTICS(117, FeatureId.PLASTICS, "man",
      "Plastics"),
  COMPUTER_AND_NETWORK_SECURITY(118, FeatureId.NETWORK_SECURITY, "tech",
      "Computer & Network Security"),
  WIRELESS(119, FeatureId.INTERNET, "tech",
      "Wireless"),
  ALTERNATIVE_DISPUTE_RESOLUTION(120, FeatureId.LAW_PRACTICE, "leg org",
      "Alternative Dispute Resolution"),
  SECURITY_AND_INVESTIGATIONS(121, FeatureId.SOCIAL_GOOD, "corp org serv",
      "Security and Investigations"),
  FACILITIES_SERVICES(122, FeatureId.MANAGEMENT, "corp serv",
      "Facilities Services"),
  OUTSOURCING_OFFSHORING(123, FeatureId.MANAGEMENT, "corp",
      "Outsourcing/Offshoring"),
  HEALTH_WELLNESS_AND_FITNESS(124, FeatureId.HEALTH_AND_FITNESS, "hlth rec",
      "Health, Wellness and Fitness"),
  ALTERNATIVE_MEDICINE(125, FeatureId.HEALTH_AND_FITNESS, "hlth",
      "Alternative Medicine"),
  MEDIA_PRODUCTION(126, FeatureId.VIDEO_PRODUCTION, "med rec",
      "Media Production"),
  ANIMATION(127, FeatureId.ANIMATION, "art med",
      "Animation"),
  COMMERCIAL_REAL_ESTATE(128, FeatureId.REAL_ESTATE, "cons corp fin",
      "Commercial Real Estate"),
  CAPITAL_MARKETS(129, FeatureId.EQUITY_INVESTING, "fin",
      "Capital Markets"),
  THINK_TANKS(130, FeatureId.GOVERNMENT, "gov org",
      "Think Tanks"),
  PHILANTHROPY(131, FeatureId.SOCIAL_GOOD, "org",
      "Philanthropy"),
  E_LEARNING(132, FeatureId.EDUCATION, "edu org",
      "E-Learning"),
  WHOLESALE(133, FeatureId.LOGISTICS, "good",
      "Wholesale"),
  IMPORT_AND_EXPORT(134, FeatureId.LOGISTICS, "corp good tran",
      "Import and Export"),
  MECHANICAL_OR_INDUSTRIAL_ENGINEERING(135, FeatureId.MECHANICAL_AND_INDUSTRIAL_ENGINEERING,
      "cons gov man", "Mechanical or Industrial Engineering"),
  PHOTOGRAPHY(136, FeatureId.PHOTOGRAPHY, "art med rec",
      "Photography"),
  HUMAN_RESOURCES(137, FeatureId.HUMAN_RESOURCES, "corp",
      "Human Resources"),
  BUSINESS_SUPPLIES_AND_EQUIPMENT(138, FeatureId.BUSINESS_SUPPLIES_AND_EQUIPMENT, "corp man",
      "Business Supplies and Equipment"),
  MENTAL_HEALTH_CARE(139, FeatureId.MEDICINE, "hlth",
      "Mental Health Care"),
  GRAPHIC_DESIGN(140, FeatureId.ARTS, "art med",
      "Graphic Design"),
  INTERNATIONAL_TRADE_AND_DEVELOPMENT(141, FeatureId.GOVERNMENT,
       "gov org tran", "International Trade and Development"),
  WINE_AND_SPIRITS(142, FeatureId.WINE_AND_SPIRITS, "good man rec",
      "Wine and Spirits"),
  LUXURY_GOODS_AND_JEWELRY(143, FeatureId.CONSUMER_GOODS, "good",
      "Luxury Goods & Jewelry"),
  RENEWABLES_AND_ENVIRONMENT(144, FeatureId.ENVIRONMENT, "gov man org",
      "Renewables & Environment"),
  GLASS_CERAMICS_AND_CONCRETE(145, FeatureId.CONSTRUCTION, "cons man",
      "Glass, Ceramics & Concrete"),
  PACKAGING_AND_CONTAINERS(146, FeatureId.CONSUMER_GOODS, "good man",
      "Packaging and Containers"),
  INDUSTRIAL_AUTOMATION(147, FeatureId.MECHANICAL_AND_INDUSTRIAL_ENGINEERING, "cons man",
      "Industrial Automation"),
  GOVERNMENT_RELATIONS(148, FeatureId.GOVERNMENT, "gov",
      "Government Relations");

  private static final Map<Integer, Industry> INDUSTRY_MAP = Maps.uniqueIndex(
      Arrays.asList(Industry.values()),
      new Function<Industry, Integer>() {
        @Override
        public Integer apply(Industry industry) {
          return industry.code;
        }
      });

  private final int code;
  private final FeatureId featureId;
  private final String group;
  private final String name;

  private Industry(int code, FeatureId featureId, String group, String name) {
    this.code = code;
    this.featureId = featureId;
    this.group = group;
    this.name = name;
  }

  public int getCode() {
    return code;
  }

  public FeatureId getFeatureId() {
    return featureId;
  }

  public String getGroup() {
    return group;
  }

  public String getName() {
    return name;
  }

  /**
   * It's awful that we have to do this, but unfortunately LinkedIn's Profile
   * response only includes English strings for people's current industries.
   */
  public static Industry fromDescription(String description) {
    for (Industry industry : INDUSTRY_MAP.values()) {
      if (industry.name.equals(description)) {
        return industry;
      }
    }
    return null;
  }

  public static Industry fromFeatureId(FeatureId featureId) {
    for (Industry industry : INDUSTRY_MAP.values()) {
      if (industry.featureId == featureId) {
        return industry;
      }
    }
    return null;
  }

  public static Industry fromCode(int code) {
    return INDUSTRY_MAP.get(code);
  }
}
