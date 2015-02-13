package com.janknspank.classifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import twitter4j.JSONException;
import twitter4j.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.janknspank.proto.UserProto.UserIndustry;

/**
 * Canonicalization of various business industries.  Used for user and
 * article classifications.
 */
public enum IndustryCode {
  DEFENSE_AND_SPACE(1, FeatureId.DEFENSE_AND_SPACE, "gov tech",
      "Defense & Space"),
  COMPUTER_HARDWARE(3, FeatureId.COMPUTER_HARDWARE, "tech",
      "Computer Hardware"),
  COMPUTER_SOFTWARE(4, FeatureId.COMPUTER_SOFTWARE, "tech",
      "Computer Software"),
  COMPUTER_NETWORKING(5, FeatureId.COMPUTER_NETWORKING, "tech",
      "Computer Networking"),
  INTERNET(6, FeatureId.INTERNET, "tech",
      "Internet"),
  SEMICONDUCTORS(7, FeatureId.SEMICONDUCTORS, "tech",
      "Semiconductors"),
  TELECOMMUNICATIONS(8, FeatureId.TELECOMMUNICATIONS, "gov tech",
      "Telecommunications"),
  LAW_PRACTICE(9, FeatureId.LAW_PRACTICE, "leg",
      "Law Practice"),
  LEGAL_SERVICES(10, FeatureId.LEGAL_SERVICES, "leg",
      "Legal Services"),
  MANAGEMENT_CONSULTING(11, FeatureId.MANAGEMENT_CONSULTING, "corp",
      "Management Consulting"),
  BIOTECHNOLOGY(12, FeatureId.BIOTECHNOLOGY, "gov hlth tech",
      "Biotechnology"),
  MEDICAL_PRACTICE(13, FeatureId.MEDICAL_PRACTICE, "hlth",
      "Medical Practice"),
  PHARMACEUTICALS(15, FeatureId.PHARMACEUTICALS, "hlth tech",
      "Pharmaceuticals"),
  VETERINARY(16, FeatureId.VETERINARY, "hlth",
      "Veterinary"),
  MEDICAL_DEVICES(17, FeatureId.MEDICAL_DEVICES, "hlth",
      "Medical Devices"),
  COSMETICS(18, FeatureId.COSMETICS, "good",
      "Cosmetics"),
  APPAREL_AND_FASHION(19, FeatureId.APPAREL_AND_FASHION, "good",
      "Apparel & Fashion"),
  SPORTING_GOODS(20, FeatureId.SPORTING_GOODS, "good rec",
      "Sporting Goods"),
  TOBACCO(21, FeatureId.TOBACCO, "good",
      "Tobacco"),
  SUPERMARKETS(22, FeatureId.SUPERMARKETS, "good",
      "Supermarkets"),
  FOOD_PRODUCTION(23, FeatureId.FOOD_PRODUCTION, "good man serv",
      "Food Production"),
  CONSUMER_ELECTRONICS(24, FeatureId.CONSUMER_ELECTRONICS, "good man",
      "Consumer Electronics"),
  CONSUMER_GOODS(25, FeatureId.CONSUMER_GOODS, "good man",
      "Consumer Goods"),
  FURNITURE(26, FeatureId.FURNITURE, "good man",
      "Furniture"),
  RETAIL(27, FeatureId.RETAIL, "good man",
      "Retail"),
  ENTERTAINMENT(28, FeatureId.ENTERTAINMENT, "med rec",
      "Entertainment"),
  GAMBLING_AND_CASINOS(29, FeatureId.GAMBLING_AND_CASINOS, "rec",
      "Gambling & Casinos"),
  LEISURE_TRAVEL_AND_TOURISM(30, FeatureId.LEISURE_TRAVEL_AND_TOURISM, "rec serv tran",
      "Leisure, Travel & Tourism"),
  HOSPITALITY(31, FeatureId.HOSPITALITY, "rec serv tran",
      "Hospitality"),
  RESTAURANTS(32, FeatureId.RESTAURANTS, "rec serv",
      "Restaurants"),
  SPORTS(33, FeatureId.SPORTS, "rec",
      "Sports"),
  FOOD_AND_BEVERAGES(34, FeatureId.FOOD_AND_BEVERAGES, "rec serv",
      "Food & Beverages"),
  MOTION_PICTURES_AND_FILM(35, FeatureId.MOTION_PICTURES_AND_FILM, "art med rec",
      "Motion Pictures and Film"),
  BROADCAST_MEDIA(36, FeatureId.BROADCAST_MEDIA, "med rec",
      "Broadcast Media"),
  MUSEUMS_AND_INSTITUTIONS(37, FeatureId.MUSEUMS_AND_INSTITUTIONS,
      "art med rec",
      "Museums and Institutions"),
  FINE_ART(38, FeatureId.FINE_ART, "art med rec",
      "Fine Art"),
  PERFORMING_ARTS(39, FeatureId.PERFORMING_ARTS, "art med rec",
      "Performing Arts"),
  RECREATIONAL_FACILITIES_AND_SERVICES(40, FeatureId.RECREATIONAL_FACILITIES_AND_SERVICES,
      "rec serv", "Recreational Facilities and Services"),
  BANKING(41, FeatureId.BANKING, "fin",
      "Banking"),
  INSURANCE(42, FeatureId.INSURANCE, "fin",
      "Insurance"),
  FINANCIAL_SERVICES(43, FeatureId.FINANCIAL_SERVICES, "fin",
      "Financial Services"),
  REAL_ESTATE(44, FeatureId.REAL_ESTATE, "cons fin good",
      "Real Estate"),
  INVESTMENT_BANKING(45, FeatureId.INVESTMENT_BANKING, "fin",
      "Investment Banking"),
  INVESTMENT_MANAGEMENT(46, FeatureId.INVESTMENT_MANAGEMENT, "fin",
      "Investment Management"),
  ACCOUNTING(47, FeatureId.ACCOUNTING, "corp fin",
      "Accounting"),
  CONSTRUCTION(48, FeatureId.CONSTRUCTION, "cons",
      "Construction"),
  BUILDING_MATERIALS(49, FeatureId.BUILDING_MATERIALS, "cons",
      "Building Materials"),
  ARCHITECTURE_AND_PLANNING(50, FeatureId.ARCHITECTURE_AND_PLANNING, "cons",
      "Architecture & Planning"),
  CIVIL_ENGINEERING(51, FeatureId.CIVIL_ENGINEERING, "cons gov",
      "Civil Engineering"),
  AVIATION_AND_AEROSPACE(52, FeatureId.AVIATION_AND_AEROSPACE, "gov man",
      "Aviation & Aerospace"),
  AUTOMOTIVE(53, FeatureId.AUTOMOTIVE, "man",
      "Automotive"),
  CHEMICALS(54, FeatureId.CHEMICALS, "man",
      "Chemicals"),
  MACHINERY(55, FeatureId.MACHINERY, "man",
      "Machinery"),
  MINING_AND_METALS(56, FeatureId.MINING_AND_METALS, "man",
      "Mining & Metals"),
  OIL_AND_ENERGY(57, FeatureId.OIL_AND_ENERGY, "man",
      "Oil & Energy"),
  SHIPBUILDING(58, FeatureId.SHIPBUILDING, "man",
      "Shipbuilding"),
  UTILITIES(59, FeatureId.UTILITIES, "man",
      "Utilities"),
  TEXTILES(60, FeatureId.TEXTILES, "man",
      "Textiles"),
  PAPER_AND_FOREST_PRODUCTS(61, FeatureId.PAPER_AND_FOREST_PRODUCTS, "man",
      "Paper & Forest Products"),
  RAILROAD_MANUFACTURE(62, FeatureId.RAILROAD_MANUFACTURE, "man",
      "Railroad Manufacture"),
  FARMING(63, FeatureId.FARMING, "agr",
      "Farming"),
  RANCHING(64, FeatureId.RANCHING, "agr",
      "Ranching"),
  DAIRY(65, FeatureId.DAIRY, "agr",
      "Dairy"),
  FISHERY(66, FeatureId.FISHERY, "agr",
      "Fishery"),
  PRIMARY_SECONDARY_EDUCATION(67, FeatureId.PRIMARY_SECONDARY_EDUCATION, "edu",
      "Primary/Secondary Education"),
  HIGHER_EDUCATION(68, FeatureId.HIGHER_EDUCATION, "edu",
      "Higher Education"),
  EDUCATION_MANAGEMENT(69, FeatureId.EDUCATION_MANAGEMENT, "edu",
      "Education Management"),
  RESEARCH(70, FeatureId.RESEARCH, "edu gov",
      "Research"),
  MILITARY(71, FeatureId.MILITARY, "gov",
      "Military"),
  LEGISLATIVE_OFFICE(72, FeatureId.LEGISLATIVE_OFFICE, "gov leg",
      "Legislative Office"),
  JUDICIARY(73, FeatureId.JUDICIARY, "gov leg",
      "Judiciary"),
  INTERNATIONAL_AFFAIRS(74, FeatureId.INTERNATIONAL_AFFAIRS, "gov",
      "International Affairs"),
  GOVERNMENT_ADMINISTRATION(75, FeatureId.GOVERNMENT_ADMINISTRATION, "gov",
      "Government Administration"),
  EXECUTIVE_OFFICE(76, FeatureId.EXECUTIVE_OFFICE, "gov",
      "Executive Office"),
  LAW_ENFORCEMENT(77, FeatureId.LAW_ENFORCEMENT, "gov leg",
      "Law Enforcement"),
  PUBLIC_SAFETY(78, FeatureId.PUBLIC_SAFETY, "gov",
      "Public Safety"),
  PUBLIC_POLICY(79, FeatureId.PUBLIC_POLICY, "gov",
      "Public Policy"),
  MARKETING_AND_ADVERTISING(80, FeatureId.MARKETING_AND_ADVERTISING, "corp med",
      "Marketing and Advertising"),
  NEWSPAPERS(81, FeatureId.NEWSPAPERS, "med rec",
      "Newspapers"),
  PUBLISHING(82, FeatureId.PUBLISHING, "med rec",
      "Publishing"),
  PRINTING(83, FeatureId.PRINTING, "med rec",
      "Printing"),
  INFORMATION_SERVICES(84, FeatureId.INFORMATION_SERVICES, "med serv",
      "Information Services"),
  LIBRARIES(85, FeatureId.LIBRARIES, "med rec serv",
      "Libraries"),
  ENVIRONMENTAL_SERVICES(86, FeatureId.ENVIRONMENTAL_SERVICES, "org serv",
      "Environmental Services"),
  PACKAGE_FREIGHT_DELIVERY(87, FeatureId.PACKAGE_FREIGHT_DELIVERY, "serv tran",
      "Package/Freight Delivery"),
  INDIVIDUAL_AND_FAMILY_SERVICES(88, FeatureId.INDIVIDUAL_AND_FAMILY_SERVICES, "org serv",
      "Individual & Family Services"),
  RELIGIOUS_INSTITUTIONS(89, FeatureId.RELIGIOUS_INSTITUTIONS, "org serv",
      "Religious Institutions"),
  CIVIC_AND_SOCIAL_ORGANIZATION(90, FeatureId.CIVIC_AND_SOCIAL_ORGANIZATION, "org serv",
      "Civic & Social Organization"),
  CONSUMER_SERVICES(91, FeatureId.CONSUMER_SERVICES, "org serv",
      "Consumer Services"),
  TRANSPORTATION_TRUCKING_RAILROAD(92, FeatureId.TRANSPORTATION_TRUCKING_RAILROAD, "tran",
      "Transportation/Trucking/Railroad"),
  WAREHOUSING(93, FeatureId.WAREHOUSING, "tran",
      "Warehousing"),
  AIRLINES_AVIATION(94, FeatureId.AIRLINES_AVIATION, "man tech tran",
      "Airlines/Aviation"),
  MARITIME(95, FeatureId.MARITIME, "tran",
      "Maritime"),
  INFORMATION_TECHNOLOGY_AND_SCIENCE(96, FeatureId.INFORMATION_TECHNOLOGY_AND_SCIENCE,
      "tech",
      "Information Technology and Services"),
  MARKET_RESEARCH(97, FeatureId.MARKET_RESEARCH, "corp",
      "Market Research"),
  PUBLIC_RELATIONS_AND_COMMUNICATIONS(98, FeatureId.PUBLIC_RELATIONS_AND_COMMUNICATIONS, "corp",
      "Public Relations and Communications"),
  DESIGN(99, FeatureId.DESIGN, "art med",
      "Design"),
  NON_PROFIT_ORGANIZATION_MANAGEMENT(100, FeatureId.NON_PROFIT_ORGANIZATION_MANAGEMENT, "org",
      "Non-Profit Organization Management"),
  FUND_RAISING(101, FeatureId.FUND_RAISING, "org",
      "Fund-Raising"),
  PROGRAM_DEVELOPMENT(102, FeatureId.PROGRAM_DEVELOPMENT, "corp org",
      "Program Development"),
  WRITING_AND_EDITING(103, FeatureId.WRITING_AND_EDITING, "art med rec",
      "Writing and Editing"),
  STAFFING_AND_RECRUITING(104, FeatureId.STAFFING_AND_RECRUITING, "corp",
      "Staffing and Recruiting"),
  PROFESSIONAL_TRAINING_AND_COACHING(105, FeatureId.PROFESSIONAL_TRAINING_AND_COACHING, "corp",
      "Professional Training & Coaching"),
  VENTURE_CAPITAL_AND_PRIVATE_EQUITY(106, FeatureId.VENTURE_CAPITAL_AND_PRIVATE_EQUITY, "fin tech",
      "Venture Capital & Private Equity"),
  POLITICAL_ORGANIZATION(107, FeatureId.POLITICAL_ORGANIZATION, "gov org",
      "Political Organization"),
  TRANSLATION_AND_LOCALIZATION(108, FeatureId.TRANSLATION_AND_LOCALIZATION, "corp gov serv",
      "Translation and Localization"),
  COMPUTER_GAMES(109, FeatureId.COMPUTER_GAMES, "med rec",
      "Computer Games"),
  EVENTS_SERVICES(110, FeatureId.EVENTS_SERVICES, "corp rec serv",
      "Events Services"),
  ARTS_AND_CRAFTS(111, FeatureId.ARTS_AND_CRAFTS, "art med rec",
      "Arts and Crafts"),
  ELECTRICAL_ELECTRONIC_MANUFACTURING(112, FeatureId.ELECTRICAL_ELECTRONIC_MANUFACTURING,
      "good man", "Electrical/Electronic Manufacturing"),
  ONLINE_MEDIA(113, FeatureId.ONLINE_MEDIA, "med",
      "Online Media"),
  NANOTECHNOLOGY(114, FeatureId.NANOTECHNOLOGY, "gov man tech",
      "Nanotechnology"),
  MUSIC(115, FeatureId.MUSIC, "art rec",
      "Music"),
  LOGISTICS_AND_SUPPLY_CHAIN(116, FeatureId.LOGISTICS_AND_SUPPLY_CHAIN, "corp tran",
      "Logistics and Supply Chain"),
  PLASTICS(117, FeatureId.PLASTICS, "man",
      "Plastics"),
  COMPUTER_AND_NETWORK_SECURITY(118, FeatureId.COMPUTER_AND_NETWORK_SECURITY, "tech",
      "Computer & Network Security"),
  WIRELESS(119, FeatureId.WIRELESS, "tech",
      "Wireless"),
  ALTERNATIVE_DISPUTE_RESOLUTION(120, FeatureId.ALTERNATIVE_DISPUTE_RESOLUTION, "leg org",
      "Alternative Dispute Resolution"),
  SECURITY_AND_INVESTIGATIONS(121, FeatureId.SECURITY_AND_INVESTIGATIONS, "corp org serv",
      "Security and Investigations"),
  FACILITIES_SERVICES(122, FeatureId.FACILITIES_SERVICES, "corp serv",
      "Facilities Services"),
  OUTSOURCING_OFFSHORING(123, FeatureId.OUTSOURCING_OFFSHORING, "corp",
      "Outsourcing/Offshoring"),
  HEALTH_WELLNESS_AND_FITNESS(124, FeatureId.HEALTH_WELLNESS_AND_FITNESS, "hlth rec",
      "Health, Wellness and Fitness"),
  ALTERNATIVE_MEDICINE(125, FeatureId.ALTERNATIVE_MEDICINE, "hlth",
      "Alternative Medicine"),
  MEDIA_PRODUCTION(126, FeatureId.MEDIA_PRODUCTION, "med rec",
      "Media Production"),
  ANIMATION(127, FeatureId.ANIMATION, "art med",
      "Animation"),
  COMMERCIAL_REAL_ESTATE(128, FeatureId.COMMERCIAL_REAL_ESTATE, "cons corp fin",
      "Commercial Real Estate"),
  CAPITAL_MARKETS(129, FeatureId.CAPITAL_MARKETS, "fin",
      "Capital Markets"),
  THINK_TANKS(130, FeatureId.THINK_TANKS, "gov org",
      "Think Tanks"),
  PHILANTHROPY(131, FeatureId.PHILANTHROPY, "org",
      "Philanthropy"),
  E_LEARNING(132, FeatureId.E_LEARNING, "edu org",
      "E-Learning"),
  WHOLESALE(133, FeatureId.WHOLESALE, "good",
      "Wholesale"),
  IMPORT_AND_EXPORT(134, FeatureId.IMPORT_AND_EXPORT, "corp good tran",
      "Import and Export"),
  MECHANICAL_OR_INDUSTRIAL_ENGINEERING(135, FeatureId.MECHANICAL_OR_INDUSTRIAL_ENGINEERING,
      "cons gov man", "Mechanical or Industrial Engineering"),
  PHOTOGRAPHY(136, FeatureId.PHOTOGRAPHY, "art med rec",
      "Photography"),
  HUMAN_RESOURCES(137, FeatureId.HUMAN_RESOURCES, "corp",
      "Human Resources"),
  BUSINESS_SUPPLIES_AND_EQUIPMENT(138, FeatureId.BUSINESS_SUPPLIES_AND_EQUIPMENT, "corp man",
      "Business Supplies and Equipment"),
  MENTAL_HEALTH_CARE(139, FeatureId.MENTAL_HEALTH_CARE, "hlth",
      "Mental Health Care"),
  HOSPITAL_AND_HEALTH_CARE(14, FeatureId.HOSPITAL_AND_HEALTH_CARE, "hlth",
      "Hospital & Health Care"),
  GRAPHIC_DESIGN(140, FeatureId.GRAPHIC_DESIGN, "art med",
      "Graphic Design"),
  INTERNATIONAL_TRADE_AND_DEVELOPMENT(141, FeatureId.INTERNATIONAL_TRADE_AND_DEVELOPMENT,
       "gov org tran", "International Trade and Development"),
  WINE_AND_SPIRITS(142, FeatureId.WINE_AND_SPIRITS, "good man rec",
      "Wine and Spirits"),
  LUXURY_GOODS_AND_JEWELRY(143, FeatureId.LUXURY_GOODS_AND_JEWELRY, "good",
      "Luxury Goods & Jewelry"),
  RENEWABLES_AND_ENVIRONMENT(144, FeatureId.RENEWABLES_AND_ENVIRONMENT, "gov man org",
      "Renewables & Environment"),
  GLASS_CERAMICS_AND_CONCRETE(145, FeatureId.GLASS_CERAMICS_AND_CONCRETE, "cons man",
      "Glass, Ceramics & Concrete"),
  PACKAGING_AND_CONTAINERS(146, FeatureId.PACKAGING_AND_CONTAINERS, "good man",
      "Packaging and Containers"),
  INDUSTRIAL_AUTOMATION(147, FeatureId.INDUSTRIAL_AUTOMATION, "cons man",
      "Industrial Automation"),
  GOVERNMENT_RELATIONS(148, FeatureId.GOVERNMENT_RELATIONS, "gov",
      "Government Relations");

  private static final Map<Integer, IndustryCode> INDUSTRY_CODE_MAP = Maps.uniqueIndex(
      Arrays.asList(IndustryCode.values()),
      new Function<IndustryCode, Integer>() {
        @Override
        public Integer apply(IndustryCode industryCode) {
          return industryCode.id;
        }
      });

  private final int id;
  private final FeatureId featureId;
  private final String group;
  private final String description;

  private IndustryCode(int id, FeatureId featureId, String group, String description) {
    this.id = id;
    this.featureId = featureId;
    this.group = group;
    this.description = description;
  }

  public int getId() {
    return id;
  }

  public FeatureId getFeatureId() {
    return featureId;
  }

  public String getGroup() {
    return group;
  }

  public String getDescription() {
    return description;
  }

  public static List<IndustryCode> getFromUserIndustries(Iterable<UserIndustry> userIndustries) {
    List<IndustryCode> industryCodes = new ArrayList<>();
    for (UserIndustry userIndustry : userIndustries) {
      industryCodes.add(INDUSTRY_CODE_MAP.get(userIndustry.getIndustryCodeId()));
    }
    return industryCodes;
  }

  /**
   * It's awful that we have to do this, but unfortunately LinkedIn's Profile
   * response only includes English strings for people's current industries.
   */
  public static IndustryCode fromDescription(String description) {
    for (IndustryCode industryCode : INDUSTRY_CODE_MAP.values()) {
      if (industryCode.description.equals(description)) {
        return industryCode;
      }
    }
    return null;
  }

  public static IndustryCode fromFeatureId(FeatureId featureId) {
    for (IndustryCode industryCode : INDUSTRY_CODE_MAP.values()) {
      if (industryCode.featureId == featureId) {
        return industryCode;
      }
    }
    return null;
  }

  public static IndustryCode fromId(int id) {
    for (IndustryCode industryCode : INDUSTRY_CODE_MAP.values()) {
      if (industryCode.id == id) {
        return industryCode;
      }
    }
    throw new IllegalArgumentException("Value " + id + " is not a valid industry code ID.");
  }

  public static IndustryCode findFromId(int id) {
    return INDUSTRY_CODE_MAP.get(id);
  }

  public JSONObject toJSON() {
    try {
      JSONObject o = new JSONObject();
      o.put("id", id);
      o.put("group", group);
      o.put("description", description);
      return o;
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }
}
