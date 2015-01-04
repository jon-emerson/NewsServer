package com.janknspank.data;

import java.sql.Connection;

import com.janknspank.proto.Core.AddressBook;

public class AddressBooks {
  /** Helper method for creating the AddressBook table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(AddressBook.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(AddressBook.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
