package com.janknspank;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;

public class DynamoDbHelper {
  private static AmazonDynamoDBAsyncClient asyncClientSingleton = null;
  private static AmazonDynamoDBClient clientSingleton = null;

  private static void createClients() {
    /*
     * The ProfileCredentialsProvider will return your [default]
     * credential profile by reading from the credentials file located at
     * (~/.aws/credentials).
     */
    AWSCredentials credentials = null;
    try {
      if (System.getenv("AWS_ACCESS_KEY_ID") != null) {
        credentials = new EnvironmentVariableCredentialsProvider().getCredentials();
      } else {
        credentials = new ProfileCredentialsProvider().getCredentials();
      }
    } catch (Exception e) {
        throw new AmazonClientException(
            "Cannot load the credentials from the credential profiles file. " +
            "Please make sure that your credentials file is at the correct " +
            "location (~/.aws/credentials), and is in valid format.",
            e);
    }

    clientSingleton = new AmazonDynamoDBClient(credentials);
    clientSingleton.setEndpoint("dynamodb.us-west-2.amazonaws.com/");

    asyncClientSingleton = new AmazonDynamoDBAsyncClient(credentials);
    asyncClientSingleton.setEndpoint("dynamodb.us-west-2.amazonaws.com/");
  }

  public static AmazonDynamoDBClient getClient() {
    if (clientSingleton == null) {
      createClients();
    }
    return clientSingleton;
  }

  public static AmazonDynamoDBAsyncClient getAsyncClient() {
    if (asyncClientSingleton == null) {
      createClients();
    }
    return asyncClientSingleton;
  }

  public static void deleteTable(String tableName){
    try {
      DeleteTableRequest request = new DeleteTableRequest().withTableName(tableName);
      AmazonDynamoDBClient client = getClient();
      client.deleteTable(request);
    } catch (AmazonServiceException ase) {
      System.err.println("Failed to delete table " + tableName + " " + ase);
    }
  }

  public static void waitForTableToBecomeAvailable(String tableName) {
    System.out.println("Waiting for " + tableName + " to become ACTIVE...");

    long startTime = System.currentTimeMillis();
    long endTime = startTime + (10 * 60 * 1000);
    while (System.currentTimeMillis() < endTime) {
      DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
      AmazonDynamoDBClient client = getClient();
      TableDescription tableDescription = client.describeTable(request).getTable();
      String tableStatus = tableDescription.getTableStatus();
      System.out.println("  - current state: " + tableStatus);
      if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
        return;
      }
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException("Table " + tableName + " never went active");
  }

  public static void waitForTableToBeDeleted(String tableName) {
    System.out.println("Waiting for " + tableName + " while status DELETING...");

    long startTime = System.currentTimeMillis();
    long endTime = startTime + (10 * 60 * 1000);
    while (System.currentTimeMillis() < endTime) {
      try {
        DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
        AmazonDynamoDBClient client = getClient();
        TableDescription tableDescription = client.describeTable(request).getTable();
        String tableStatus = tableDescription.getTableStatus();
        System.out.println("  - current state: " + tableStatus);
        if (tableStatus.equals(TableStatus.ACTIVE.toString())) return;
      } catch (ResourceNotFoundException e) {
        System.out.println("Table " + tableName + " is not found. It was deleted.");
        return;
      }
      try {Thread.sleep(1000);} catch (Exception e) {}
    }
    throw new RuntimeException("Table " + tableName + " was never deleted");
  }

  public static String generateGuid() {
    UUID uuid = UUID.randomUUID();
    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.putLong(uuid.getLeastSignificantBits());
    bb.putLong(uuid.getMostSignificantBits());
    return Base64.encodeBase64URLSafeString(bb.array()).replaceAll("=", "");
  }
}
