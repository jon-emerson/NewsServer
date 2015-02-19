package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.StringUtils;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;

public class GetLatestDemoVersionServlet extends StandardServlet {
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

  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DatabaseSchemaException, BiznessException,
      DatabaseRequestException, RequestException {
    AWSCredentials credentials = new BasicAWSCredentials(S3_ACCESS_KEY, S3_SECRET_KEY);

    ClientConfiguration clientConfig = new ClientConfiguration();
    clientConfig.setProtocol(Protocol.HTTPS);

    AmazonS3 amazonS3Client = new AmazonS3Client(credentials, clientConfig);
    amazonS3Client.setEndpoint("https://s3-us-west-2.amazonaws.com/");

    ObjectListing objects = amazonS3Client.listObjects("spotter-demo");
    int[] latestVersionComponents = {0, 0, 0};

    do {
      for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
        System.out.println(objectSummary.getKey() + "\t" +
            objectSummary.getSize() + "\t" +
            StringUtils.fromDate(objectSummary.getLastModified()));

        // Pull out the major, minor, bug fix version numbers from the file
        // Example file name: Spotter-v1.0.2.ipa
        String fileName = objectSummary.getKey();
        String[] fileNameParts = fileName.split("-v");
        if (fileNameParts.length > 0) {
          String fileVersion = fileNameParts[fileNameParts.length - 1];
          String[] versionComponentStrings = fileVersion.split("\\.");
          int[] versionComponents = {0, 0, 0};
          int i = 0;
          for (String versionComponentString : versionComponentStrings) {
            if (versionComponentString.equals("ipa")) {
              break;
            }
            versionComponents[i] = Integer.parseInt(versionComponentString);
            i++;
          }

          boolean isLatest = false;
          if (versionComponents[0] > latestVersionComponents[0]) {
            isLatest = true;
          } else if (versionComponents[0] == latestVersionComponents[0] 
              && versionComponents[1] == latestVersionComponents[1]) {
            isLatest = true;
          } else if (versionComponents[0] == latestVersionComponents[0] 
              && versionComponents[1] == latestVersionComponents[1]
              && versionComponents[2] > latestVersionComponents[2]) {
            isLatest = true;
          }

          if (isLatest) {
            latestVersionComponents = versionComponents;
          }
        }
      }
      objects = amazonS3Client.listNextBatchOfObjects(objects);
    } while (objects.isTruncated());

    // Create response
    JSONObject response = createSuccessResponse();

    response.put("version", latestVersionComponents[0] 
        + "." + latestVersionComponents[1] + "." + latestVersionComponents[2]);
    response.put("downloadUrl", "https://www.spotternews.com/demo?update=1");
    return response;
  }
}
