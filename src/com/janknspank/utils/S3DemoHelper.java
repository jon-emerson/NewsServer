package com.janknspank.utils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.janknspank.common.Version;

public class S3DemoHelper {
  static final String S3_ACCESS_KEY;
  static final String S3_SECRET_KEY;
  static {
    S3_ACCESS_KEY = System.getenv("S3_ACCESS_KEY");
    if (S3_ACCESS_KEY == null) {
      throw new Error("$S3_ACCESS_KEY is undefined");
    }
    S3_SECRET_KEY = System.getenv("S3_SECRET_KEY");
    if (S3_SECRET_KEY == null) {
      throw new Error("$S3_SECRET_KEY is undefined");
    }
  }

  public static Version findLatestDemoVersion() {
    AWSCredentials credentials = new BasicAWSCredentials(S3_ACCESS_KEY, S3_SECRET_KEY);

    ClientConfiguration clientConfig = new ClientConfiguration();
    clientConfig.setProtocol(Protocol.HTTPS);

    AmazonS3 amazonS3Client = new AmazonS3Client(credentials, clientConfig);
    amazonS3Client.setEndpoint("https://s3-us-west-2.amazonaws.com/");

    ObjectListing objects = amazonS3Client.listObjects("spotter-demo");
    Version latestVersion = new Version("0");

    do {
      for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
        // Pull out the major, minor, bug fix version numbers from the file
        // Example file name: Spotter-v1.0.2.ipa
        String fileName = objectSummary.getKey();
        if (fileName.endsWith(".ipa")) {
          String[] fileNameParts =
              fileName.substring(0, fileName.length() - ".ipa".length()).split("-v");
          if (fileNameParts.length > 0) {
            Version version = new Version(fileNameParts[fileNameParts.length - 1]);
            if (version.atLeast(latestVersion)) {
              latestVersion = version;
            }
          }
        }
      }
      objects = amazonS3Client.listNextBatchOfObjects(objects);
    } while (objects.isTruncated());

    return latestVersion;
  }
}
