package com.janknspank.data;

import java.sql.Connection;

import com.janknspank.proto.Core.AddressBookData;

public class AddressBookDatas {
  /** Helper method for creating the AddressBookData table. */
  public static void main(String args[]) throws Exception {
    Connection connection = Database.getConnection();
    connection.prepareStatement(Database.getCreateTableStatement(AddressBookData.class)).execute();
    for (String statement : Database.getCreateIndexesStatement(AddressBookData.class)) {
      connection.prepareStatement(statement).execute();
    }
  }
}
