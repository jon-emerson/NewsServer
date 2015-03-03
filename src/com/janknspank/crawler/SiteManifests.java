package com.janknspank.crawler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.net.InternetDomainName;
import com.google.protobuf.TextFormat;
import com.janknspank.common.Asserts;
import com.janknspank.common.Logger;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Validator;
import com.janknspank.proto.CrawlerProto.SiteManifest;
import com.janknspank.proto.CrawlerProto.TestInstructions.ArticleUrlDetectorChecks;

public class SiteManifests {
  private static final Logger LOG = new Logger(SiteManifests.class);
  private static Map<String, SiteManifest> CONTENT_SITE_MAP = null;
  private static List<SiteManifest> CONTENT_SITE_LIST = null;

  /**
   * Validates that the passed-in domain is indeed part of the root domain or an
   * aka root domain in the specified content site.  This is used for validation
   * of blacklist subdomains and test instructions.
   */
  private static void validateDomainInSiteManifest(final String domain, SiteManifest site)
      throws SiteManifestException {
    Set<String> akaRootDomainSet = ImmutableSet.copyOf(site.getAkaRootDomainList());
    boolean found = false;
    String mutableDomain = domain;
    while (!found && mutableDomain.contains(".")) {
      if (mutableDomain.equals(site.getRootDomain()) || akaRootDomainSet.contains(mutableDomain)) {
        found = true;
      }
      mutableDomain = mutableDomain.substring(mutableDomain.indexOf(".") + 1);
    }
    if (!found) {
      throw new SiteManifestException("Domain \"" + domain
          + "\" does not belong to site with root domain \"" + site.getRootDomain() + "\"");
    }
  }

  /**
   * Validates that the passed-in URL does indeed belong to the passed content
   * site.  This is used for validation of test instructions.
   */
  private static void validateUrlInSiteManifest(String url, SiteManifest site)
      throws SiteManifestException {
    try {
      validateDomainInSiteManifest(new URL(url).getHost(), site);
    } catch (MalformedURLException e) {
      throw new SiteManifestException("Bad URL: " + url, e);
    }
  }

  private static void validateSiteManifest(File manifestFile, SiteManifest site)
      throws SiteManifestException {
    // We're kind of overloading the meaning of (required) and other Database
    // annotations here, so unfortunately Validator throws Database exceptions,
    // even though the Database isn't involved.  Therefore just catch the
    // exceptions and rethrow them as new ContentSiteManifestExceptions.
    String errorPrefix = "Error in site manifest " + manifestFile.getAbsolutePath() + ": ";
    try {
      Validator.assertValid(site);
    } catch (DatabaseRequestException | DatabaseSchemaException e) {
      throw new SiteManifestException(errorPrefix + e.getMessage(), e);
    }

    // Make sure this .manifest file defines a SiteManifest for the right domain.
    Asserts.assertTrue(manifestFile.getName().equals(site.getRootDomain() + ".manifest"),
        errorPrefix + " File does not specify root_url "
            + manifestFile.getName().substring(0, manifestFile.getName().lastIndexOf(".")),
        SiteManifestException.class);
    Asserts.assertTrue(InternetDomainName.isValid(site.getRootDomain()),
        errorPrefix + "Root domain is not valid: " + site.getRootDomain(),
        SiteManifestException.class);
    Asserts.assertTrue(!site.getRootDomain().startsWith("www."),
        "Don't include www. prefix on root_domain.", SiteManifestException.class);

    // Make sure the AKA root domains are unique and valid.
    Set<String> akaRootDomainSet = ImmutableSet.copyOf(site.getAkaRootDomainList());
    Asserts.assertTrue(site.getAkaRootDomainList().size() == akaRootDomainSet.size(),
        errorPrefix + "Duplicate domains listed in aka_root_domain list",
        SiteManifestException.class);
    for (String akaRootDomain : site.getAkaRootDomainList()) {
      Asserts.assertTrue(!site.getRootDomain().equals(akaRootDomain),
          errorPrefix + "Aka root domain cannot duplicate root domain: " + akaRootDomain,
          SiteManifestException.class);
      // Forbid subdomains in AKAs because there's no point: AKAs are used for
      // site lookups, and we can already look-up via the primary root domain.
      Asserts.assertTrue(!akaRootDomain.endsWith("." + site.getRootDomain()),
          errorPrefix + "Aka root domain cannot be subdomain of root: " + akaRootDomain,
          SiteManifestException.class);
      Asserts.assertTrue(InternetDomainName.isValid(akaRootDomain),
          errorPrefix + "Aka root domain is not valid: " + akaRootDomain,
          SiteManifestException.class);
      Asserts.assertTrue(!akaRootDomain.startsWith("www."),
          "Don't include www. prefix on aka_root_domains.", SiteManifestException.class);
    }

    // Make sure the blacklist subdomains are all underneath the root domain or
    // the aka root domains.
    for (String blacklistSubdomain : site.getSubdomainBlacklistList()) {
      validateDomainInSiteManifest(blacklistSubdomain, site);
    }

    // Validate TestInstructions.
    ArticleUrlDetectorChecks articleUrlDetectorChecks =
        site.getTestInstructions().getArticleUrlDetectorChecks();
    Asserts.assertTrue(
        articleUrlDetectorChecks.getArticleUrlCount() >= 5,
        errorPrefix + "Must have at least 5 Article URLs defined in test_instructions.",
        SiteManifestException.class);
    Asserts.assertTrue(
        articleUrlDetectorChecks.getArticleUrlCount()
            == ImmutableSet.copyOf(articleUrlDetectorChecks.getArticleUrlList()).size(),
        errorPrefix + "Duplicate Article URLs not allowed in test_instructions.",
        SiteManifestException.class);
    for (String articleUrl : articleUrlDetectorChecks.getArticleUrlList()) {
      validateUrlInSiteManifest(articleUrl, site);
    }
    Asserts.assertTrue(
        articleUrlDetectorChecks.getNonArticleUrlCount() >= 5,
        errorPrefix + "Must have at least 5 non-Article URLs defined in test_instructions.",
        SiteManifestException.class);
    Asserts.assertTrue(
        articleUrlDetectorChecks.getNonArticleUrlCount()
            == ImmutableSet.copyOf(articleUrlDetectorChecks.getNonArticleUrlList()).size(),
        errorPrefix + "Duplicate non-Article URLs not allowed in test_instructions.",
        SiteManifestException.class);
    for (String nonArticleUrl : articleUrlDetectorChecks.getNonArticleUrlList()) {
      validateUrlInSiteManifest(nonArticleUrl, site);
      try {
        URL url = new URL(nonArticleUrl);
        Asserts.assertTrue(
            (url.getPath() != null && url.getPath().length() > 2)
                || (url.getQuery() != null && url.getQuery().length() > 4),
            errorPrefix + "Non-article URLs in test_instructions must have non-trivial "
                + "paths or parameters.  Found: " + nonArticleUrl,
            SiteManifestException.class);
      } catch (MalformedURLException e) {
        throw new SiteManifestException(errorPrefix + "Bad URL: " + nonArticleUrl, e);
      }
    }
  }

  /**
   * Builds a Map of SiteManifest object definitions, keyed by root URL, from
   * the .manifest files located in /sites/.
   */
  private static synchronized Map<String, SiteManifest> getSiteManifestMap() {
    if (CONTENT_SITE_MAP == null) {
      CONTENT_SITE_MAP = Maps.newHashMap();
      for (File manifestFile : new File("sites/").listFiles()) {
        if (!manifestFile.getName().endsWith(".manifest")) {
          continue;
        }
        SiteManifest.Builder contentSiteBuilder = SiteManifest.newBuilder();
        Reader reader = null;
        try {
          reader = new FileReader(manifestFile);
          try {
            TextFormat.merge(reader, contentSiteBuilder);
          } catch (TextFormat.ParseException e) {
            throw new SiteManifestException(
                "Error in site manifest " + manifestFile.getAbsolutePath() + ": " + e.getMessage(),
                e);
          }
          SiteManifest contentSite = contentSiteBuilder.build();
          validateSiteManifest(manifestFile, contentSite);
          CONTENT_SITE_MAP.put(contentSite.getRootDomain(), contentSite);
        } catch (IOException e) {
          LOG.error("Could not read site manifest: " + manifestFile.getAbsolutePath(), e);
          throw new Error(e);
        } catch (SiteManifestException e) {
          LOG.error("Invalid manifest: " + manifestFile.getAbsolutePath(), e);
          throw new Error(e);
        } finally {
          IOUtils.closeQuietly(reader);
        }
      }
      CONTENT_SITE_LIST = ImmutableList.copyOf(CONTENT_SITE_MAP.values());
    }
    return CONTENT_SITE_MAP;
  }

  /**
   * Returns SiteManifest objects for every news site supported by our system.
   */
  public static List<SiteManifest> getList() {
    getSiteManifestMap();
    return CONTENT_SITE_LIST;
  }

  /**
   * Gets the site crawling / whitelisting instructions relevant to a specific
   * URL.  E.g. if a CNN article was passed, the SiteManifest object for root URL
   * "cnn.com" would be returned.
   */
  public static SiteManifest getForUrl(URL url) {
    List<SiteManifest> sites = getList();
    if (sites == null) {
      throw new Error("Site manifests did not load.  See exceptions above for explanation.");
    }

    String domain = url.getHost();
    while (domain.contains(".")) {
      for (SiteManifest site : sites) {
        if (site.getRootDomain().equals(domain)
            || Iterables.contains(site.getAkaRootDomainList(), domain)) {
          return site;
        }
      }
      domain = domain.substring(domain.indexOf(".") + 1);
    }
    return null;
  }

}
