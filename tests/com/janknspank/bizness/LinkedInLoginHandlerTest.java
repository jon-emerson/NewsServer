package com.janknspank.bizness;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import com.janknspank.database.Validator;
import com.janknspank.proto.UserProto.LinkedInProfile.Employer;

public class LinkedInLoginHandlerTest {
  @Test
  public void testGetEmployers() throws Exception {
    Document document = Jsoup.parse(
        new File("testdata/linkedinprofile.xml"),
        "UTF-8",
        "https://api.linkedin.com/~me");
    List<Employer> employers = LinkedInLoginHandler.getEmployers(document);
    assertEquals(6, employers.size());
    assertEquals("Spotter", employers.get(0).getName());
    assertEquals("Google", employers.get(1).getName());

    for (Employer employer : employers) {
      Validator.assertValid(employer);
    }
  }
}
