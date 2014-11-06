package com.janknspank;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

/**
 * DynamoDb business object: Tracks discovered URLs and the number of
 * occurrences we've seen of them.
 */
public class DiscoveredUrl {
  public static final String TABLE_NAME_STR = "discovered-url";
  public static final String URL_ID_STR = "urlId";
  public static final String URL_STR = "url";
  public static final String OCCURANCE_COUNT_STR = "url";
  private static final String CREATE_DATE_TIME_STR = "createDateTime";
  private static final String LAST_OCCURANCE_DATE_TIME_STR = "lastOccuranceDateTime";
  private static final List<String> AWS_ATTRIBUTE_NAMES = Arrays.asList(
          URL_ID_STR, URL_STR, OCCURANCE_COUNT_STR, CREATE_DATE_TIME_STR,
          LAST_OCCURANCE_DATE_TIME_STR);

  private String urlId;
  private String url;
  private long occuranceCount;
  private Date createDateTime;
  private Date lastOccuranceDateTime;

  public String getUrlId() {
    return urlId;
  }

  public String getUrl() {
    return url;
  }

  public long getOccuranceCount() {
    return occuranceCount;
  }

  public Date getCreateDateTime() {
    return createDateTime;
  }

  public Date getLastOccuranceDateTime() {
    return lastOccuranceDateTime;
  }

  public static class Builder {
    private String urlId;
    private String url;
    private long occuranceCount;
    private Date createDateTime;
    private Date lastOccuranceDateTime;

    public Builder() {
    }

    public Builder setUrlId(String urlId) {
      this.urlId = urlId;
      return this;
    }

    public Builder setUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder setOccuranceCount(long occuranceCount) {
      this.occuranceCount = occuranceCount;
      return this;
    }

    public Builder setCreateDateTime(Date createDateTime) {
      this.createDateTime = createDateTime;
      return this;
    }

    public Builder setLastOccuranceDateTime(Date lastOccuranceDateTime) {
      this.lastOccuranceDateTime = lastOccuranceDateTime;
      return this;
    }

    public DiscoveredUrl build() throws ValidationException {
      DiscoveredUrl discoveredUrl = new DiscoveredUrl();
      discoveredUrl.urlId = urlId;
      discoveredUrl.url = url;
      discoveredUrl.occuranceCount = occuranceCount;
      discoveredUrl.createDateTime = (createDateTime == null) ? new Date() : createDateTime;
      discoveredUrl.lastOccuranceDateTime =
          (lastOccuranceDateTime == null) ? new Date() : lastOccuranceDateTime;
      discoveredUrl.assertValid();
      return discoveredUrl;
    }
  }

  private void assertValid() throws ValidationException {
    Asserts.assertNonEmpty(urlId, URL_ID_STR);
    Asserts.assertNonEmpty(url, URL_STR);
    Asserts.assertNotNull(createDateTime, CREATE_DATE_TIME_STR);
  }

  public Map<String, AttributeValue> toAwsItem() {
    Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
    item.put(URL_ID_STR, new AttributeValue(urlId));
    item.put(URL_STR, new AttributeValue(url));
    item.put(OCCURANCE_COUNT_STR, new AttributeValue().withN(Long.toString(occuranceCount)));
    item.put(CREATE_DATE_TIME_STR,
        new AttributeValue(Constants.DATE_TIME_FORMATTER.format(createDateTime)));
    item.put(LAST_OCCURANCE_DATE_TIME_STR,
        new AttributeValue(Constants.DATE_TIME_FORMATTER.format(lastOccuranceDateTime)));
    return item;
  }

  public static List<String> getAwsAttributeNames() {
    return AWS_ATTRIBUTE_NAMES;
  }

  public JSONObject toJSONObject() {
    JSONObject o = new JSONObject();
    o.put(URL_ID_STR, urlId);
    o.put(URL_STR, url);
    o.put(OCCURANCE_COUNT_STR, occuranceCount);
    o.put(CREATE_DATE_TIME_STR, Constants.DATE_TIME_FORMATTER.format(createDateTime));
    o.put(LAST_OCCURANCE_DATE_TIME_STR,
        Constants.DATE_TIME_FORMATTER.format(lastOccuranceDateTime));
    return o;
  }

  public static DiscoveredUrl fromAwsItem(Map<String, AttributeValue> item)
      throws ValidationException {
    Builder b = new Builder();
    b.setUrlId(item.get(URL_ID_STR).getS());
    b.setUrl(item.get(URL_STR).getS());
    b.setOccuranceCount(Long.parseLong(item.get(OCCURANCE_COUNT_STR).getN()));

    try {
      b.setCreateDateTime(
          Constants.DATE_TIME_FORMATTER.parse(item.get(CREATE_DATE_TIME_STR).getS()));
      b.setLastOccuranceDateTime(
          Constants.DATE_TIME_FORMATTER.parse(item.get(LAST_OCCURANCE_DATE_TIME_STR).getS()));
    } catch (ParseException e) {
      e.printStackTrace();
      b.setCreateDateTime(new Date());
      b.setLastOccuranceDateTime(new Date());
    }

    return b.build();
  }

  public static CreateTableRequest getAwsCreateTableRequest() {
    ArrayList<KeySchemaElement> ks = new ArrayList<KeySchemaElement>();
    ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();

    // A hash of the URL is the URL's ID, and the index of this table.
    ks.add(new KeySchemaElement()
        .withAttributeName(URL_ID_STR)
        .withKeyType(KeyType.HASH));
    attributeDefinitions.add(new AttributeDefinition()
        .withAttributeName(URL_ID_STR)
        .withAttributeType("S"));

    // Provide initial provisioned throughput values.
    ProvisionedThroughput provisionedthroughput = new ProvisionedThroughput()
        .withReadCapacityUnits(20L)
        .withWriteCapacityUnits(20L);

    // Add secondary index for # of occurrences, so that we can sort on it.
    // Unfortunately we have to do this as a hash=ID, range=occurrences index,
    // but it works.
    // TODO: THIS NEEDS TO BE A LOCAL SECONDARY INDEX
//    GlobalSecondaryIndex gsi = new GlobalSecondaryIndex()
//        .withIndexName(URL_ID_STR + "-index")
//        .withProvisionedThroughput(provisionedthroughput)
//        .withKeySchema(new KeySchemaElement()
//            .withAttributeName(URL_ID_STR)
//            .withKeyType(KeyType.HASH))
//        .withProjection(new Projection()
//            .withProjectionType("ALL"));

    // Return the request.
    return new CreateTableRequest()
        .withTableName(TABLE_NAME_STR)
        .withKeySchema(ks)
        .withProvisionedThroughput(provisionedthroughput)
        .withAttributeDefinitions(attributeDefinitions);
  }

  /** Helper method for creating the discovered-url table. */
  public static void main(String args[]) {
    AmazonDynamoDBClient dbClient = DynamoDbHelper.getClient();
    dbClient.createTable(DiscoveredUrl.getAwsCreateTableRequest());
    DynamoDbHelper.waitForTableToBecomeAvailable(DiscoveredUrl.TABLE_NAME_STR);
  }
}
