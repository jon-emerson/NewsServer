package com.janknspank.data;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.janknspank.proto.Core.IndustryCode;

/**
 * Industry codes on articles added by humans
 */
public class IndustryCodes {
  public static final Map<Integer, IndustryCode> INDUSTRY_CODE_MAP = Maps.uniqueIndex(
      ImmutableList.of(
          IndustryCode.newBuilder()
              .setId(1)
              .setGroup("gov tech")
              .setDescription("Defense & Space")
              .build(),
          IndustryCode.newBuilder()
              .setId(10)
              .setGroup("leg")
              .setDescription("Legal Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(100)
              .setGroup("org")
              .setDescription("Non-Profit Organization Management")
              .build(),
          IndustryCode.newBuilder()
              .setId(101)
              .setGroup("org")
              .setDescription("Fund-Raising")
              .build(),
          IndustryCode.newBuilder()
              .setId(102)
              .setGroup("corp org")
              .setDescription("Program Development")
              .build(),
          IndustryCode.newBuilder()
              .setId(103)
              .setGroup("art med rec")
              .setDescription("Writing and Editing")
              .build(),
          IndustryCode.newBuilder()
              .setId(104)
              .setGroup("corp")
              .setDescription("Staffing and Recruiting")
              .build(),
          IndustryCode.newBuilder()
              .setId(105)
              .setGroup("corp")
              .setDescription("Professional Training & Coaching")
              .build(),
          IndustryCode.newBuilder()
              .setId(106)
              .setGroup("fin tech")
              .setDescription("Venture Capital & Private Equity")
              .build(),
          IndustryCode.newBuilder()
              .setId(107)
              .setGroup("gov org")
              .setDescription("Political Organization")
              .build(),
          IndustryCode.newBuilder()
              .setId(108)
              .setGroup("corp gov serv")
              .setDescription("Translation and Localization")
              .build(),
          IndustryCode.newBuilder()
              .setId(109)
              .setGroup("med rec")
              .setDescription("Computer Games")
              .build(),
          IndustryCode.newBuilder()
              .setId(11)
              .setGroup("corp")
              .setDescription("Management Consulting")
              .build(),
          IndustryCode.newBuilder()
              .setId(110)
              .setGroup("corp rec serv")
              .setDescription("Events Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(111)
              .setGroup("art med rec")
              .setDescription("Arts and Crafts")
              .build(),
          IndustryCode.newBuilder()
              .setId(112)
              .setGroup("good man")
              .setDescription("Electrical/Electronic Manufacturing")
              .build(),
          IndustryCode.newBuilder()
              .setId(113)
              .setGroup("med")
              .setDescription("Online Media")
              .build(),
          IndustryCode.newBuilder()
              .setId(114)
              .setGroup("gov man tech")
              .setDescription("Nanotechnology")
              .build(),
          IndustryCode.newBuilder()
              .setId(115)
              .setGroup("art rec")
              .setDescription("Music")
              .build(),
          IndustryCode.newBuilder()
              .setId(116)
              .setGroup("corp tran")
              .setDescription("Logistics and Supply Chain")
              .build(),
          IndustryCode.newBuilder()
              .setId(117)
              .setGroup("man")
              .setDescription("Plastics")
              .build(),
          IndustryCode.newBuilder()
              .setId(118)
              .setGroup("tech")
              .setDescription("Computer & Network Security")
              .build(),
          IndustryCode.newBuilder()
              .setId(119)
              .setGroup("tech")
              .setDescription("Wireless")
              .build(),
          IndustryCode.newBuilder()
              .setId(12)
              .setGroup("gov hlth tech")
              .setDescription("Biotechnology")
              .build(),
          IndustryCode.newBuilder()
              .setId(120)
              .setGroup("leg org")
              .setDescription("Alternative Dispute Resolution")
              .build(),
          IndustryCode.newBuilder()
              .setId(121)
              .setGroup("corp org serv")
              .setDescription("Security and Investigations")
              .build(),
          IndustryCode.newBuilder()
              .setId(122)
              .setGroup("corp serv")
              .setDescription("Facilities Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(123)
              .setGroup("corp")
              .setDescription("Outsourcing/Offshoring")
              .build(),
          IndustryCode.newBuilder()
              .setId(124)
              .setGroup("hlth rec")
              .setDescription("Health, Wellness and Fitness")
              .build(),
          IndustryCode.newBuilder()
              .setId(125)
              .setGroup("hlth")
              .setDescription("Alternative Medicine")
              .build(),
          IndustryCode.newBuilder()
              .setId(126)
              .setGroup("med rec")
              .setDescription("Media Production")
              .build(),
          IndustryCode.newBuilder()
              .setId(127)
              .setGroup("art med")
              .setDescription("Animation")
              .build(),
          IndustryCode.newBuilder()
              .setId(128)
              .setGroup("cons corp fin")
              .setDescription("Commercial Real Estate")
              .build(),
          IndustryCode.newBuilder()
              .setId(129)
              .setGroup("fin")
              .setDescription("Capital Markets")
              .build(),
          IndustryCode.newBuilder()
              .setId(13)
              .setGroup("hlth")
              .setDescription("Medical Practice")
              .build(),
          IndustryCode.newBuilder()
              .setId(130)
              .setGroup("gov org")
              .setDescription("Think Tanks")
              .build(),
          IndustryCode.newBuilder()
              .setId(131)
              .setGroup("org")
              .setDescription("Philanthropy")
              .build(),
          IndustryCode.newBuilder()
              .setId(132)
              .setGroup("edu org")
              .setDescription("E-Learning")
              .build(),
          IndustryCode.newBuilder()
              .setId(133)
              .setGroup("good")
              .setDescription("Wholesale")
              .build(),
          IndustryCode.newBuilder()
              .setId(134)
              .setGroup("corp good tran")
              .setDescription("Import and Export")
              .build(),
          IndustryCode.newBuilder()
              .setId(135)
              .setGroup("cons gov man")
              .setDescription("Mechanical or Industrial Engineering")
              .build(),
          IndustryCode.newBuilder()
              .setId(136)
              .setGroup("art med rec")
              .setDescription("Photography")
              .build(),
          IndustryCode.newBuilder()
              .setId(137)
              .setGroup("corp")
              .setDescription("Human Resources")
              .build(),
          IndustryCode.newBuilder()
              .setId(138)
              .setGroup("corp man")
              .setDescription("Business Supplies and Equipment")
              .build(),
          IndustryCode.newBuilder()
              .setId(139)
              .setGroup("hlth")
              .setDescription("Mental Health Care")
              .build(),
          IndustryCode.newBuilder()
              .setId(14)
              .setGroup("hlth")
              .setDescription("Hospital & Health Care")
              .build(),
          IndustryCode.newBuilder()
              .setId(140)
              .setGroup("art med")
              .setDescription("Graphic Design")
              .build(),
          IndustryCode.newBuilder()
              .setId(141)
              .setGroup("gov org tran")
              .setDescription("International Trade and Development")
              .build(),
          IndustryCode.newBuilder()
              .setId(142)
              .setGroup("good man rec")
              .setDescription("Wine and Spirits")
              .build(),
          IndustryCode.newBuilder()
              .setId(143)
              .setGroup("good")
              .setDescription("Luxury Goods & Jewelry")
              .build(),
          IndustryCode.newBuilder()
              .setId(144)
              .setGroup("gov man org")
              .setDescription("Renewables & Environment")
              .build(),
          IndustryCode.newBuilder()
              .setId(145)
              .setGroup("cons man")
              .setDescription("Glass, Ceramics & Concrete")
              .build(),
          IndustryCode.newBuilder()
              .setId(146)
              .setGroup("good man")
              .setDescription("Packaging and Containers")
              .build(),
          IndustryCode.newBuilder()
              .setId(147)
              .setGroup("cons man")
              .setDescription("Industrial Automation")
              .build(),
          IndustryCode.newBuilder()
              .setId(148)
              .setGroup("gov")
              .setDescription("Government Relations")
              .build(),
          IndustryCode.newBuilder()
              .setId(15)
              .setGroup("hlth tech")
              .setDescription("Pharmaceuticals")
              .build(),
          IndustryCode.newBuilder()
              .setId(16)
              .setGroup("hlth")
              .setDescription("Veterinary")
              .build(),
          IndustryCode.newBuilder()
              .setId(17)
              .setGroup("hlth")
              .setDescription("Medical Devices")
              .build(),
          IndustryCode.newBuilder()
              .setId(18)
              .setGroup("good")
              .setDescription("Cosmetics")
              .build(),
          IndustryCode.newBuilder()
              .setId(19)
              .setGroup("good")
              .setDescription("Apparel & Fashion")
              .build(),
          IndustryCode.newBuilder()
              .setId(20)
              .setGroup("good rec")
              .setDescription("Sporting Goods")
              .build(),
          IndustryCode.newBuilder()
              .setId(21)
              .setGroup("good")
              .setDescription("Tobacco")
              .build(),
          IndustryCode.newBuilder()
              .setId(22)
              .setGroup("good")
              .setDescription("Supermarkets")
              .build(),
          IndustryCode.newBuilder()
              .setId(23)
              .setGroup("good man serv")
              .setDescription("Food Production")
              .build(),
          IndustryCode.newBuilder()
              .setId(24)
              .setGroup("good man")
              .setDescription("Consumer Electronics")
              .build(),
          IndustryCode.newBuilder()
              .setId(25)
              .setGroup("good man")
              .setDescription("Consumer Goods")
              .build(),
          IndustryCode.newBuilder()
              .setId(26)
              .setGroup("good man")
              .setDescription("Furniture")
              .build(),
          IndustryCode.newBuilder()
              .setId(27)
              .setGroup("good man")
              .setDescription("Retail")
              .build(),
          IndustryCode.newBuilder()
              .setId(28)
              .setGroup("med rec")
              .setDescription("Entertainment")
              .build(),
          IndustryCode.newBuilder()
              .setId(29)
              .setGroup("rec")
              .setDescription("Gambling & Casinos")
              .build(),
          IndustryCode.newBuilder()
              .setId(3)
              .setGroup("tech")
              .setDescription("Computer Hardware")
              .build(),
          IndustryCode.newBuilder()
              .setId(30)
              .setGroup("rec serv tran")
              .setDescription("Leisure, Travel & Tourism")
              .build(),
          IndustryCode.newBuilder()
              .setId(31)
              .setGroup("rec serv tran")
              .setDescription("Hospitality")
              .build(),
          IndustryCode.newBuilder()
              .setId(32)
              .setGroup("rec serv")
              .setDescription("Restaurants")
              .build(),
          IndustryCode.newBuilder()
              .setId(33)
              .setGroup("rec")
              .setDescription("Sports")
              .build(),
          IndustryCode.newBuilder()
              .setId(34)
              .setGroup("rec serv")
              .setDescription("Food & Beverages")
              .build(),
          IndustryCode.newBuilder()
              .setId(35)
              .setGroup("art med rec")
              .setDescription("Motion Pictures and Film")
              .build(),
          IndustryCode.newBuilder()
              .setId(36)
              .setGroup("med rec")
              .setDescription("Broadcast Media")
              .build(),
          IndustryCode.newBuilder()
              .setId(37)
              .setGroup("art med rec")
              .setDescription("Museums and Institutions")
              .build(),
          IndustryCode.newBuilder()
              .setId(38)
              .setGroup("art med rec")
              .setDescription("Fine Art")
              .build(),
          IndustryCode.newBuilder()
              .setId(39)
              .setGroup("art med rec")
              .setDescription("Performing Arts")
              .build(),
          IndustryCode.newBuilder()
              .setId(4)
              .setGroup("tech")
              .setDescription("Computer Software")
              .build(),
          IndustryCode.newBuilder()
              .setId(40)
              .setGroup("rec serv")
              .setDescription("Recreational Facilities and Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(41)
              .setGroup("fin")
              .setDescription("Banking")
              .build(),
          IndustryCode.newBuilder()
              .setId(42)
              .setGroup("fin")
              .setDescription("Insurance")
              .build(),
          IndustryCode.newBuilder()
              .setId(43)
              .setGroup("fin")
              .setDescription("Financial Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(44)
              .setGroup("cons fin good")
              .setDescription("Real Estate")
              .build(),
          IndustryCode.newBuilder()
              .setId(45)
              .setGroup("fin")
              .setDescription("Investment Banking")
              .build(),
          IndustryCode.newBuilder()
              .setId(46)
              .setGroup("fin")
              .setDescription("Investment Management")
              .build(),
          IndustryCode.newBuilder()
              .setId(47)
              .setGroup("corp fin")
              .setDescription("Accounting")
              .build(),
          IndustryCode.newBuilder()
              .setId(48)
              .setGroup("cons")
              .setDescription("Construction")
              .build(),
          IndustryCode.newBuilder()
              .setId(49)
              .setGroup("cons")
              .setDescription("Building Materials")
              .build(),
          IndustryCode.newBuilder()
              .setId(5)
              .setGroup("tech")
              .setDescription("Computer Networking")
              .build(),
          IndustryCode.newBuilder()
              .setId(50)
              .setGroup("cons")
              .setDescription("Architecture & Planning")
              .build(),
          IndustryCode.newBuilder()
              .setId(51)
              .setGroup("cons gov")
              .setDescription("Civil Engineering")
              .build(),
          IndustryCode.newBuilder()
              .setId(52)
              .setGroup("gov man")
              .setDescription("Aviation & Aerospace")
              .build(),
          IndustryCode.newBuilder()
              .setId(53)
              .setGroup("man")
              .setDescription("Automotive")
              .build(),
          IndustryCode.newBuilder()
              .setId(54)
              .setGroup("man")
              .setDescription("Chemicals")
              .build(),
          IndustryCode.newBuilder()
              .setId(55)
              .setGroup("man")
              .setDescription("Machinery")
              .build(),
          IndustryCode.newBuilder()
              .setId(56)
              .setGroup("man")
              .setDescription("Mining & Metals")
              .build(),
          IndustryCode.newBuilder()
              .setId(57)
              .setGroup("man")
              .setDescription("Oil & Energy")
              .build(),
          IndustryCode.newBuilder()
              .setId(58)
              .setGroup("man")
              .setDescription("Shipbuilding")
              .build(),
          IndustryCode.newBuilder()
              .setId(59)
              .setGroup("man")
              .setDescription("Utilities")
              .build(),
          IndustryCode.newBuilder()
              .setId(6)
              .setGroup("tech")
              .setDescription("Internet")
              .build(),
          IndustryCode.newBuilder()
              .setId(60)
              .setGroup("man")
              .setDescription("Textiles")
              .build(),
          IndustryCode.newBuilder()
              .setId(61)
              .setGroup("man")
              .setDescription("Paper & Forest Products")
              .build(),
          IndustryCode.newBuilder()
              .setId(62)
              .setGroup("man")
              .setDescription("Railroad Manufacture")
              .build(),
          IndustryCode.newBuilder()
              .setId(63)
              .setGroup("agr")
              .setDescription("Farming")
              .build(),
          IndustryCode.newBuilder()
              .setId(64)
              .setGroup("agr")
              .setDescription("Ranching")
              .build(),
          IndustryCode.newBuilder()
              .setId(65)
              .setGroup("agr")
              .setDescription("Dairy")
              .build(),
          IndustryCode.newBuilder()
              .setId(66)
              .setGroup("agr")
              .setDescription("Fishery")
              .build(),
          IndustryCode.newBuilder()
              .setId(67)
              .setGroup("edu")
              .setDescription("Primary/Secondary Education")
              .build(),
          IndustryCode.newBuilder()
              .setId(68)
              .setGroup("edu")
              .setDescription("Higher Education")
              .build(),
          IndustryCode.newBuilder()
              .setId(69)
              .setGroup("edu")
              .setDescription("Education Management")
              .build(),
          IndustryCode.newBuilder()
              .setId(7)
              .setGroup("tech")
              .setDescription("Semiconductors")
              .build(),
          IndustryCode.newBuilder()
              .setId(70)
              .setGroup("edu gov")
              .setDescription("Research")
              .build(),
          IndustryCode.newBuilder()
              .setId(71)
              .setGroup("gov")
              .setDescription("Military")
              .build(),
          IndustryCode.newBuilder()
              .setId(72)
              .setGroup("gov leg")
              .setDescription("Legislative Office")
              .build(),
          IndustryCode.newBuilder()
              .setId(73)
              .setGroup("gov leg")
              .setDescription("Judiciary")
              .build(),
          IndustryCode.newBuilder()
              .setId(74)
              .setGroup("gov")
              .setDescription("International Affairs")
              .build(),
          IndustryCode.newBuilder()
              .setId(75)
              .setGroup("gov")
              .setDescription("Government Administration")
              .build(),
          IndustryCode.newBuilder()
              .setId(76)
              .setGroup("gov")
              .setDescription("Executive Office")
              .build(),
          IndustryCode.newBuilder()
              .setId(77)
              .setGroup("gov leg")
              .setDescription("Law Enforcement")
              .build(),
          IndustryCode.newBuilder()
              .setId(78)
              .setGroup("gov")
              .setDescription("Public Safety")
              .build(),
          IndustryCode.newBuilder()
              .setId(79)
              .setGroup("gov")
              .setDescription("Public Policy")
              .build(),
          IndustryCode.newBuilder()
              .setId(8)
              .setGroup("gov tech")
              .setDescription("Telecommunications")
              .build(),
          IndustryCode.newBuilder()
              .setId(80)
              .setGroup("corp med")
              .setDescription("Marketing and Advertising")
              .build(),
          IndustryCode.newBuilder()
              .setId(81)
              .setGroup("med rec")
              .setDescription("Newspapers")
              .build(),
          IndustryCode.newBuilder()
              .setId(82)
              .setGroup("med rec")
              .setDescription("Publishing")
              .build(),
          IndustryCode.newBuilder()
              .setId(83)
              .setGroup("med rec")
              .setDescription("Printing")
              .build(),
          IndustryCode.newBuilder()
              .setId(84)
              .setGroup("med serv")
              .setDescription("Information Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(85)
              .setGroup("med rec serv")
              .setDescription("Libraries")
              .build(),
          IndustryCode.newBuilder()
              .setId(86)
              .setGroup("org serv")
              .setDescription("Environmental Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(87)
              .setGroup("serv tran")
              .setDescription("Package/Freight Delivery")
              .build(),
          IndustryCode.newBuilder()
              .setId(88)
              .setGroup("org serv")
              .setDescription("Individual & Family Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(89)
              .setGroup("org serv")
              .setDescription("Religious Institutions")
              .build(),
          IndustryCode.newBuilder()
              .setId(9)
              .setGroup("leg")
              .setDescription("Law Practice")
              .build(),
          IndustryCode.newBuilder()
              .setId(90)
              .setGroup("org serv")
              .setDescription("Civic & Social Organization")
              .build(),
          IndustryCode.newBuilder()
              .setId(91)
              .setGroup("org serv")
              .setDescription("Consumer Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(92)
              .setGroup("tran")
              .setDescription("Transportation/Trucking/Railroad")
              .build(),
          IndustryCode.newBuilder()
              .setId(93)
              .setGroup("tran")
              .setDescription("Warehousing")
              .build(),
          IndustryCode.newBuilder()
              .setId(94)
              .setGroup("man tech tran")
              .setDescription("Airlines/Aviation")
              .build(),
          IndustryCode.newBuilder()
              .setId(95)
              .setGroup("tran")
              .setDescription("Maritime")
              .build(),
          IndustryCode.newBuilder()
              .setId(96)
              .setGroup("tech")
              .setDescription("Information Technology and Services")
              .build(),
          IndustryCode.newBuilder()
              .setId(97)
              .setGroup("corp")
              .setDescription("Market Research")
              .build(),
          IndustryCode.newBuilder()
              .setId(98)
              .setGroup("corp")
              .setDescription("Public Relations and Communications")
              .build(),
          IndustryCode.newBuilder()
              .setId(99)
              .setGroup("art med")
              .setDescription("Design")
              .build()),
      new Function<IndustryCode, Integer>() {
        @Override
        public Integer apply(IndustryCode industryCode) {
          return industryCode.getId();
        }
      });
  
}
