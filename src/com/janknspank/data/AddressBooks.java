package com.janknspank.data;

import com.janknspank.proto.Core.AddressBook;

public class AddressBooks {
  /** Helper method for creating the AddressBook table. */
  public static void main(String args[]) throws Exception {
    Database database = Database.getInstance();
    database.prepareStatement(database.getCreateTableStatement(AddressBook.class)).execute();
    for (String statement : database.getCreateIndexesStatement(AddressBook.class)) {
      database.prepareStatement(statement).execute();
    }
  }
}
