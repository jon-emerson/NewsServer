package com.janknspank.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.janknspank.database.Database;
import com.janknspank.proto.CoreProto.Url;
import com.janknspank.proto.SiteProto.SiteManifest;
import com.janknspank.proto.SiteProto.SiteManifest.PathBlacklist;

/**
 * Uses SiteManifest.subdomain_blacklist and SiteManifest.path_blacklist against
 * URLs to determine if they're allowed in our system.
 */
public class UrlWhitelist {
  public static final Predicate<String> PREDICATE = new Predicate<String>() {
    @Override
    public boolean apply(String url) {
      return UrlWhitelist.isOkay(url);
    }
  };
  private static final int MAX_URL_LENGTH = Database.getStringLength(Url.class, "url");

  public static boolean isOkay(String urlString) {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      return false;
    }

    // Length exclusion (this is the longest a URL can be in our database).
    if (urlString.length() > MAX_URL_LENGTH) {
      return false;
    }

    // Global path/domain exclusions.
    String domain = url.getHost();
    String path = url.getPath();
    if (domain.contains("video.") ||
        domain.contains("videos.") ||
        path.startsWith("/cgi-bin/") ||
        path.contains("/video/") ||
        path.contains("/videos/")) {
      return false;
    }

    // Global extension exclusions.
    if (path.endsWith("/atom.xml") ||
        path.endsWith("/rss.xml") ||
        path.endsWith(".gif") ||
        path.endsWith(".jpeg") ||
        path.endsWith(".jpg") ||
        path.endsWith(".mp3") ||
        path.endsWith(".mp4") ||
        path.endsWith(".mpeg") ||
        path.endsWith(".pdf") ||
        path.endsWith(".png")) {
      return false;
    }

    // If we don't support the site this URL is hosted on, return false.
    SiteManifest site = SiteManifests.getForUrl(url);
    if (site == null) {
      return false;
    }

    // If the URL is on a blacklisted subdomain, return false.
    for (String blacklistedDomain : site.getSubdomainBlacklistList()) {
      if (domain.equals(blacklistedDomain)) {
        return false;
      }
    }

    // If the URL's path matches a blacklist needle, return false.
    for (PathBlacklist blacklistedPath : site.getPathBlacklistList()) {
      switch (blacklistedPath.getLocation()) {
        case EQUALS:
          if (path.equals(blacklistedPath.getNeedle())) {
            return false;
          }
          break;

        case STARTS_WITH:
          if (path.startsWith(blacklistedPath.getNeedle())) {
            return false;
          }
          break;

        case ENDS_WITH:
          if (path.endsWith(blacklistedPath.getNeedle())) {
            return false;
          }
          break;

        case CONTAINS:
          if (path.contains(blacklistedPath.getNeedle())) {
            return false;
          }
          break;

        case REGEX_FIND:
          if (Pattern.compile(blacklistedPath.getNeedle()).matcher(path).find()) {
            return false;
          }
          break;

        case REGEX_MATCH:
          if (Pattern.compile(blacklistedPath.getNeedle()).matcher(path).matches()) {
            return false;
          }
          break;
      }
    }

    // Everything passed!
    return true;
  }
}
