package com.janknspank.bizness;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.janknspank.proto.EnumsProto.IntentCode;

/**
 * Intent codes on users
 */
public class IntentCodes {
  public static final Map<String, IntentCode> INTENT_CODE_MAP = Maps.uniqueIndex(
      ImmutableList.of(
          IntentCode.newBuilder()
              .setCode("indst")
              .setDescription("Stay on top of your industry")
              .build(),
          IntentCode.newBuilder()
              .setCode("sklls")
              .setDescription("Improve your skills")
              .build(),
          IntentCode.newBuilder()
              .setCode("swtch")
              .setDescription("Switch companies")
              .build(),
          IntentCode.newBuilder()
              .setCode("start")
              .setDescription("Start a company")
              .build()),
      new Function<IntentCode, String>() {
        @Override
        public String apply(IntentCode intentCode) {
          return intentCode.getCode();
        }
      });
}
