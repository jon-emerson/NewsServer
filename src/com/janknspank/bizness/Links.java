package com.janknspank.bizness;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.CoreProto.Link;
import com.janknspank.proto.CoreProto.Url;

/**
 * Tracks a link from one URL's content to another's.  The primary key is a
 * composite of the origin and destination URL IDs, as defined in the
 * DiscoveredUrl table.
 */
public class Links {
  /**
   * Records that there's a link from {@code sourceUrl} to each of the passed
   * {@code destinationUrls}.
   */
  public static void put(final Url sourceUrl, Iterable<Url> destinationUrls)
     throws BiznessException {
    try {
      Database.insert(Iterables.transform(destinationUrls,
          new Function<Url, Link>() {
            @Override
            public Link apply(Url destinationUrl) {
              return Link.newBuilder()
                  .setOriginUrlId(sourceUrl.getId())
                  .setDestinationUrlId(destinationUrl.getId())
                  .setDiscoveryTime(destinationUrl.getDiscoveryTime())
                  .setLastFoundTime(destinationUrl.getDiscoveryTime())
                  .build();
            }
          }));
    } catch (DatabaseSchemaException | DatabaseRequestException e) {
      throw new BiznessException("Could not create link: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes any links coming to or from the passed discovered URL ID.
   */
  public static int deleteIds(List<String> ids) throws DatabaseSchemaException {
    return Database.with(Link.class).delete(new QueryOption.WhereEquals("url_id", ids))
        + deleteFromOriginUrlId(ids);
  }

  /**
   * Deletes all recorded links from the passed URLs.  Useful for cleaning
   * up old interpreted link data before a new interpretation / crawl.
   */
  public static int deleteFromOriginUrlId(Iterable<String> urlIds) throws DatabaseSchemaException {
    return Database.with(Link.class).delete(new QueryOption.WhereEquals("origin_url_id", urlIds));
  }

  /** Helper method for creating the Link table. */
  public static void main(String args[]) throws Exception {
    Database.with(Link.class).createTable();
  }
}
