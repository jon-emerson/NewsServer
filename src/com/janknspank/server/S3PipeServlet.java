package com.janknspank.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class S3PipeServlet extends HttpServlet {
  private static final String BUCKET_NAME = "spotter-demo";
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
  
  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String pathToPipe = req.getPathInfo().substring(1);
    System.out.println(pathToPipe);
    
    AWSCredentials credentials = new BasicAWSCredentials(S3_ACCESS_KEY, S3_SECRET_KEY);

    ClientConfiguration clientConfig = new ClientConfiguration();
    clientConfig.setProtocol(Protocol.HTTPS);

    AmazonS3 amazonS3Client = new AmazonS3Client(credentials, clientConfig);
    amazonS3Client.setEndpoint("https://s3-us-west-2.amazonaws.com/");

    InputStream in = null;
    OutputStream out = null;
    S3Object object = null;
    try {
      object = amazonS3Client.getObject(
          new GetObjectRequest(BUCKET_NAME, pathToPipe));
      in = object.getObjectContent();
      resp.setContentType(object.getObjectMetadata().getContentType());
      out = resp.getOutputStream();
      IOUtils.copy(in, out);
    } finally {
      if (object != null) {
        object.close();
      }
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }
  
}
