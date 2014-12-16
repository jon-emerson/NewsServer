package com.janknspank.dom;

import org.xml.sax.helpers.LocatorImpl;

/**
 * Extends the Locator API in org.xml.sax to additionally return the starting
 * indexes for DOM events.
 */
public class LenientLocator extends LocatorImpl {
  private long startingOffset;

  public void setStartingOffset(long startingOffset) {
    this.startingOffset = startingOffset;
  }

  /**
   * Returns the STARTING offset of a DOM event.  The offset is the byte
   * location in the parsed document where the '<' character exists.
   */
  public long getStartingOffset() {
    return startingOffset;
  }
}
