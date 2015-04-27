package com.janknspank.bizness;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.janknspank.classifier.FeatureId;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.Personas;

public class UserIndustriesTest {
  @Test
  public void test() {
    User jonUser = Personas.convertToUser(Personas.getByEmail("panaceaa@gmail.com"));
    assertTrue(UserIndustries.hasFeatureId(jonUser, FeatureId.SOFTWARE));
    assertFalse(UserIndustries.hasFeatureId(jonUser, FeatureId.GOVERNMENT));
  }
}
