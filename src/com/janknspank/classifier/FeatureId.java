package com.janknspank.classifier;


/**
 * A list of all features we support or plan to support.
 * @see Feature
 *
 * TODO(jonemerson): May want to delete Title from here and allow Feature
 * class implementations to handle them internally.
 */
public enum FeatureId {
  DEFENSE_AND_SPACE(10001, FeatureType.INDUSTRY, "Defense & Space"),
  COMPUTER_HARDWARE(10003, FeatureType.INDUSTRY, "Computer Hardware"),
  COMPUTER_SOFTWARE(10004, FeatureType.INDUSTRY, "Computer Software"),
  COMPUTER_NETWORKING(10005, FeatureType.INDUSTRY, "Computer Networking"),
  INTERNET(10006, FeatureType.INDUSTRY, "Internet"),
  SEMICONDUCTORS(10007, FeatureType.INDUSTRY, "Semiconductors"),
  TELECOMMUNICATIONS(10008, FeatureType.INDUSTRY, "Telecommunications"),
  LAW_PRACTICE(10009, FeatureType.INDUSTRY, "Law Practice"),
  LEGAL_SERVICES(10010, FeatureType.INDUSTRY, "Legal Services"),
  MANAGEMENT_CONSULTING(10011, FeatureType.INDUSTRY, "Management Consulting"),
  BIOTECHNOLOGY(10012, FeatureType.INDUSTRY, "Biotechnology"),
  MEDICAL_PRACTICE(10013, FeatureType.INDUSTRY, "Medical Practice"),
  HOSPITAL_AND_HEALTH_CARE(10014, FeatureType.INDUSTRY, "Hospital & Health Care"),
  PHARMACEUTICALS(10015, FeatureType.INDUSTRY, "Pharmaceuticals"),
  VETERINARY(10016, FeatureType.INDUSTRY, "Veterinary"),
  MEDICAL_DEVICES(10017, FeatureType.INDUSTRY, "Medical Devices"),
  COSMETICS(10018, FeatureType.INDUSTRY, "Cosmetics"),
  APPAREL_AND_FASHION(10019, FeatureType.INDUSTRY, "Apparel & Fashion"),
  SPORTING_GOODS(10020, FeatureType.INDUSTRY, "Sporting Goods"),
  TOBACCO(10021, FeatureType.INDUSTRY, "Tobacco"),
  SUPERMARKETS(10022, FeatureType.INDUSTRY, "Supermarkets"),
  FOOD_PRODUCTION(10023, FeatureType.INDUSTRY, "Food Production"),
  CONSUMER_ELECTRONICS(10024, FeatureType.INDUSTRY, "Consumer Electronics"),
  CONSUMER_GOODS(10025, FeatureType.INDUSTRY, "Consumer Goods"),
  FURNITURE(10026, FeatureType.INDUSTRY, "Furniture"),
  RETAIL(10027, FeatureType.INDUSTRY, "Retail"),
  ENTERTAINMENT(10028, FeatureType.INDUSTRY, "Entertainment"),
  GAMBLING_AND_CASINOS(10029, FeatureType.INDUSTRY, "Gambling & Casinos"),
  LEISURE_TRAVEL_AND_TOURISM(10030, FeatureType.INDUSTRY, "Leisure, Travel & Tourism"),
  HOSPITALITY(10031, FeatureType.INDUSTRY, "Hospitality"),
  RESTAURANTS(10032, FeatureType.INDUSTRY, "Restaurants"),
  SPORTS(10033, FeatureType.INDUSTRY, "Sports"),
  FOOD_AND_BEVERAGES(10034, FeatureType.INDUSTRY, "Food & Beverages"),
  MOTION_PICTURES_AND_FILM(10035, FeatureType.INDUSTRY, "Motion Pictures and Film"),
  BROADCAST_MEDIA(10036, FeatureType.INDUSTRY, "Broadcast Media"),
  MUSEUMS_AND_INSTITUTIONS(10037, FeatureType.INDUSTRY, "Museums and Institutions"),
  FINE_ART(10038, FeatureType.INDUSTRY, "Fine Art"),
  PERFORMING_ARTS(10039, FeatureType.INDUSTRY, "Performing Arts"),
  RECREATIONAL_FACILITIES_AND_SERVICES(10040, FeatureType.INDUSTRY,
      "Recreational Facilities and Services"),
  BANKING(10041, FeatureType.INDUSTRY, "Banking"),
  INSURANCE(10042, FeatureType.INDUSTRY, "Insurance"),
  FINANCIAL_SERVICES(10043, FeatureType.INDUSTRY, "Financial Services"),
  REAL_ESTATE(10044, FeatureType.INDUSTRY, "Real Estate"),
  INVESTMENT_BANKING(10045, FeatureType.INDUSTRY, "Investment Banking"),
  INVESTMENT_MANAGEMENT(10046, FeatureType.INDUSTRY, "Investment Management"),
  ACCOUNTING(10047, FeatureType.INDUSTRY, "Accounting"),
  CONSTRUCTION(10048, FeatureType.INDUSTRY, "Construction"),
  BUILDING_MATERIALS(10049, FeatureType.INDUSTRY, "Building Materials"),
  ARCHITECTURE_AND_PLANNING(10050, FeatureType.INDUSTRY, "Architecture & Planning"),
  CIVIL_ENGINEERING(10051, FeatureType.INDUSTRY, "Civil Engineering"),
  AVIATION_AND_AEROSPACE(10052, FeatureType.INDUSTRY, "Aviation & Aerospace"),
  AUTOMOTIVE(10053, FeatureType.INDUSTRY, "Automotive"),
  CHEMICALS(10054, FeatureType.INDUSTRY, "Chemicals"),
  MACHINERY(10055, FeatureType.INDUSTRY, "Machinery"),
  MINING_AND_METALS(10056, FeatureType.INDUSTRY, "Mining & Metals"),
  OIL_AND_ENERGY(10057, FeatureType.INDUSTRY, "Oil & Energy"),
  SHIPBUILDING(10058, FeatureType.INDUSTRY, "Shipbuilding"),
  UTILITIES(10059, FeatureType.INDUSTRY, "Utilities"),
  TEXTILES(10060, FeatureType.INDUSTRY, "Textiles"),
  PAPER_AND_FOREST_PRODUCTS(10061, FeatureType.INDUSTRY, "Paper & Forest Products"),
  RAILROAD_MANUFACTURE(10062, FeatureType.INDUSTRY, "Railroad Manufacture"),
  FARMING(10063, FeatureType.INDUSTRY, "Farming"),
  RANCHING(10064, FeatureType.INDUSTRY, "Ranching"),
  DAIRY(10065, FeatureType.INDUSTRY, "Dairy"),
  FISHERY(10066, FeatureType.INDUSTRY, "Fishery"),
  PRIMARY_SECONDARY_EDUCATION(10067, FeatureType.INDUSTRY, "Primary/Secondary Education"),
  HIGHER_EDUCATION(10068, FeatureType.INDUSTRY, "Higher Education"),
  EDUCATION_MANAGEMENT(10069, FeatureType.INDUSTRY, "Education Management"),
  RESEARCH(10070, FeatureType.INDUSTRY, "Research"),
  MILITARY(10071, FeatureType.INDUSTRY, "Military"),
  LEGISLATIVE_OFFICE(10072, FeatureType.INDUSTRY, "Legislative Office"),
  JUDICIARY(10073, FeatureType.INDUSTRY, "Judiciary"),
  INTERNATIONAL_AFFAIRS(10074, FeatureType.INDUSTRY, "International Affairs"),
  GOVERNMENT_ADMINISTRATION(10075, FeatureType.INDUSTRY, "Government Administration"),
  EXECUTIVE_OFFICE(10076, FeatureType.INDUSTRY, "Executive Office"),
  LAW_ENFORCEMENT(10077, FeatureType.INDUSTRY, "Law Enforcement"),
  PUBLIC_SAFETY(10078, FeatureType.INDUSTRY, "Public Safety"),
  PUBLIC_POLICY(10079, FeatureType.INDUSTRY, "Public Policy"),
  MARKETING_AND_ADVERTISING(10080, FeatureType.INDUSTRY, "Marketing and Advertising"),
  NEWSPAPERS(10081, FeatureType.INDUSTRY, "Newspapers"),
  PUBLISHING(10082, FeatureType.INDUSTRY, "Publishing"),
  PRINTING(10083, FeatureType.INDUSTRY, "Printing"),
  INFORMATION_SERVICES(10084, FeatureType.INDUSTRY, "Information Services"),
  LIBRARIES(10085, FeatureType.INDUSTRY, "Libraries"),
  ENVIRONMENTAL_SERVICES(10086, FeatureType.INDUSTRY, "Environmental Services"),
  PACKAGE_FREIGHT_DELIVERY(10087, FeatureType.INDUSTRY, "Package/Freight Delivery"),
  INDIVIDUAL_AND_FAMILY_SERVICES(10088, FeatureType.INDUSTRY, "Individual & Family Services"),
  RELIGIOUS_INSTITUTIONS(10089, FeatureType.INDUSTRY, "Religious Institutions"),
  CIVIC_AND_SOCIAL_ORGANIZATION(10090, FeatureType.INDUSTRY, "Civic & Social Organization"),
  CONSUMER_SERVICES(10091, FeatureType.INDUSTRY, "Consumer Services"),
  TRANSPORTATION_TRUCKING_RAILROAD(10092, FeatureType.INDUSTRY, "Transportation/Trucking/Railroad"),
  WAREHOUSING(10093, FeatureType.INDUSTRY, "Warehousing"),
  AIRLINES_AVIATION(10094, FeatureType.INDUSTRY, "Airlines/Aviation"),
  MARITIME(10095, FeatureType.INDUSTRY, "Maritime"),
  INFORMATION_TECHNOLOGY_AND_SCIENCE(10096, FeatureType.INDUSTRY,
      "Information Technology and Services"),
  MARKET_RESEARCH(10097, FeatureType.INDUSTRY, "Market Research"),
  PUBLIC_RELATIONS_AND_COMMUNICATIONS(10098, FeatureType.INDUSTRY,
      "Public Relations and Communications"),
  DESIGN(10099, FeatureType.INDUSTRY, "Design"),
  NON_PROFIT_ORGANIZATION_MANAGEMENT(10100, FeatureType.INDUSTRY,
      "Non-Profit Organization Management"),
  FUND_RAISING(10101, FeatureType.INDUSTRY, "Fund-Raising"),
  PROGRAM_DEVELOPMENT(10102, FeatureType.INDUSTRY, "Program Development"),
  WRITING_AND_EDITING(10103, FeatureType.INDUSTRY, "Writing and Editing"),
  STAFFING_AND_RECRUITING(10104, FeatureType.INDUSTRY, "Staffing and Recruiting"),
  PROFESSIONAL_TRAINING_AND_COACHING(10105, FeatureType.INDUSTRY, "Professional Training & Coaching"),
  VENTURE_CAPITAL_AND_PRIVATE_EQUITY(10106, FeatureType.INDUSTRY, "Venture Capital & Private Equity"),
  POLITICAL_ORGANIZATION(10107, FeatureType.INDUSTRY, "Political Organization"),
  TRANSLATION_AND_LOCALIZATION(10108, FeatureType.INDUSTRY, "Translation and Localization"),
  COMPUTER_GAMES(10109, FeatureType.INDUSTRY, "Computer Games"),
  EVENTS_SERVICES(10110, FeatureType.INDUSTRY, "Events Services"),
  ARTS_AND_CRAFTS(10111, FeatureType.INDUSTRY, "Arts and Crafts"),
  ELECTRICAL_ELECTRONIC_MANUFACTURING(10112, FeatureType.INDUSTRY,
      "Electrical/Electronic Manufacturing"),
  ONLINE_MEDIA(10113, FeatureType.INDUSTRY, "Online Media"),
  NANOTECHNOLOGY(10114, FeatureType.INDUSTRY, "Nanotechnology"),
  MUSIC(10115, FeatureType.INDUSTRY, "Music"),
  LOGISTICS_AND_SUPPLY_CHAIN(10116, FeatureType.INDUSTRY, "Logistics and Supply Chain"),
  PLASTICS(10117, FeatureType.INDUSTRY, "Plastics"),
  COMPUTER_AND_NETWORK_SECURITY(10118, FeatureType.INDUSTRY, "Computer & Network Security"),
  WIRELESS(10119, FeatureType.INDUSTRY, "Wireless"),
  ALTERNATIVE_DISPUTE_RESOLUTION(10120, FeatureType.INDUSTRY, "Alternative Dispute Resolution"),
  SECURITY_AND_INVESTIGATIONS(10121, FeatureType.INDUSTRY, "Security and Investigations"),
  FACILITIES_SERVICES(10122, FeatureType.INDUSTRY, "Facilities Services"),
  OUTSOURCING_OFFSHORING(10123, FeatureType.INDUSTRY, "Outsourcing/Offshoring"),
  HEALTH_WELLNESS_AND_FITNESS(10124, FeatureType.INDUSTRY, "Health, Wellness and Fitness"),
  ALTERNATIVE_MEDICINE(10125, FeatureType.INDUSTRY, "Alternative Medicine"),
  MEDIA_PRODUCTION(10126, FeatureType.INDUSTRY, "Media Production"),
  ANIMATION(10127, FeatureType.INDUSTRY, "Animation"),
  COMMERCIAL_REAL_ESTATE(10128, FeatureType.INDUSTRY, "Commercial Real Estate"),
  CAPITAL_MARKETS(10129, FeatureType.INDUSTRY, "Capital Markets"),
  THINK_TANKS(10130, FeatureType.INDUSTRY, "Think Tanks"),
  PHILANTHROPY(10131, FeatureType.INDUSTRY, "Philanthropy"),
  E_LEARNING(10132, FeatureType.INDUSTRY, "E-Learning"),
  WHOLESALE(10133, FeatureType.INDUSTRY, "Wholesale"),
  IMPORT_AND_EXPORT(10134, FeatureType.INDUSTRY, "Import and Export"),
  MECHANICAL_OR_INDUSTRIAL_ENGINEERING(10135, FeatureType.INDUSTRY,
      "Mechanical or Industrial Engineering"),
  PHOTOGRAPHY(10136, FeatureType.INDUSTRY, "Photography"),
  HUMAN_RESOURCES(10137, FeatureType.INDUSTRY, "Human Resources"),
  BUSINESS_SUPPLIES_AND_EQUIPMENT(10138, FeatureType.INDUSTRY, "Business Supplies and Equipment"),
  MENTAL_HEALTH_CARE(10139, FeatureType.INDUSTRY, "Mental Health Care"),
  GRAPHIC_DESIGN(10140, FeatureType.INDUSTRY, "Graphic Design"),
  INTERNATIONAL_TRADE_AND_DEVELOPMENT(10141, FeatureType.INDUSTRY,
      "International Trade and Development"),
  WINE_AND_SPIRITS(10142, FeatureType.INDUSTRY, "Wine and Spirits"),
  LUXURY_GOODS_AND_JEWELRY(10143, FeatureType.INDUSTRY, "Luxury Goods & Jewelry"),
  RENEWABLES_AND_ENVIRONMENT(10144, FeatureType.INDUSTRY, "Renewables & Environment"),
  GLASS_CERAMICS_AND_CONCRETE(10145, FeatureType.INDUSTRY, "Glass, Ceramics & Concrete"),
  PACKAGING_AND_CONTAINERS(10146, FeatureType.INDUSTRY, "Packaging and Containers"),
  INDUSTRIAL_AUTOMATION(10147, FeatureType.INDUSTRY, "Industrial Automation"),
  GOVERNMENT_RELATIONS(10148, FeatureType.INDUSTRY, "Government Relations"),
  STARTUP_TECH(20000, FeatureType.SERVES_INTENT, "Related to tech startups"),
  STARTUP_TRADITIONAL(20001, FeatureType.SERVES_INTENT,
      "Related to traditional brick and mortar startups"),
  MANUAL_HEURISTIC_ACQUISITIONS(30000, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks for whether an article is about an acquisition"),
  MANUAL_HEURISTIC_LAUNCHES(30001, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks for whether an article is about a product launch"),
  MANUAL_HEURISTIC_FUNDRAISING(30002, FeatureType.MANUAL_HEURISTIC,
      "Hard-coded search term checks for whether an article is about a funding round");

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
}
