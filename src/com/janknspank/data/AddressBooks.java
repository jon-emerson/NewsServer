package com.janknspank.data;

import com.janknspank.proto.Core.AddressBook;

public class AddressBooks {
  /** Helper method for creating the AddressBook table. */
  public static void main(String args[]) throws Exception {
    Database.getInstance().createTable(AddressBook.class);
  }
}
