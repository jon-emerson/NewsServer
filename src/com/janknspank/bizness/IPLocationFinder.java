package com.janknspank.bizness;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;

import com.janknspank.proto.CoreProto.Location;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.Subdivision;

public class IPLocationFinder {
  private static DatabaseReader READER;
  static {
    try {
      READER = new DatabaseReader.Builder(new File("support/GeoLite2-City.mmdb")).build();
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  public static Location getLocation(HttpServletRequest req) throws BiznessException {
    CityResponse response;
    try {
      response = READER.city(InetAddress.getByName(req.getRemoteAddr()));
    } catch (IOException e) {
      throw new BiznessException("Error reading GeoIP file: " + e.getMessage(), e);
    } catch (GeoIp2Exception e) {
      throw new BiznessException("Internal error in GeoIP: " + e.getMessage(), e);
    }

    Location.Builder locationBuilder = Location.newBuilder();

    Country country = response.getCountry();
    locationBuilder.setCountryCode(country.getIsoCode());  // 'US'
    locationBuilder.setCountryName(country.getName());  // 'United States'

    Subdivision subdivision = response.getMostSpecificSubdivision();
    locationBuilder.setStateCode(subdivision.getIsoCode()); // 'MN'
    locationBuilder.setStateName(subdivision.getName()); // 'Minnesota'

    City city = response.getCity();
    locationBuilder.setCityName(city.getName()); // 'Minneapolis'

    Postal postal = response.getPostal();
    locationBuilder.setPostalCode(postal.getCode());  // '55455'

    com.maxmind.geoip2.record.Location location = response.getLocation();
    locationBuilder.setLatitude(location.getLatitude()); // 44.9733
    locationBuilder.setLongitude(location.getLongitude()); // -93.2323

    return locationBuilder.build();
  }
}
